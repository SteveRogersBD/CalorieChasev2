import math
import random
import requests
from pydantic import BaseModel, Field, field_validator
from strands import tool
from agent.config import cfg

# --- pydantic models for IO clarity ---
class PlanRouteIn(BaseModel):
    start_lat: float
    start_lng: float
    mode: str = Field(pattern="^(walk|jog|run)$")
    distance_m: int | None = None
    prompt: str | None = ""

    @field_validator("start_lat")
    @classmethod
    def lat_ok(cls, v):
        if not (-90 <= v <= 90):
            raise ValueError("invalid latitude")
        return v

    @field_validator("start_lng")
    @classmethod
    def lng_ok(cls, v):
        if not (-180 <= v <= 180):
            raise ValueError("invalid longitude")
        return v

class Dest(BaseModel):
    lat: float
    long: float
    name: str | None = None

class PlanRouteOut(BaseModel):
    polyline: str
    est_duration: float  # minutes
    dest: Dest
    source: str  # "places" | "directions" | "synthetic"

PACE_MIN_PER_KM = {
    "walk": 13.5,
    "jog": 7.5,
    "run": 5.5,
}

def _est_duration_min(mode: str, distance_m: float) -> float:
    pace = PACE_MIN_PER_KM[mode]
    return round(pace * (distance_m / 1000.0), 2)

def _places_nearby(lat: float, lng: float, keywords: list[str]) -> dict | None:
    if not cfg.GOOGLE_MAPS_KEY:
        return None
    # use rankby=distance; at least one keyword
    kw = " ".join(keywords) if keywords else "park"
    url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
    params = dict(
        location=f"{lat},{lng}",
        rankby="distance",
        keyword=kw,
        key=cfg.GOOGLE_MAPS_KEY,
    )
    r = requests.get(url, params=params, timeout=cfg.HTTP_TIMEOUT_S)
    r.raise_for_status()
    data = r.json()
    results = data.get("results", [])
    return results[0] if results else None

def _directions(start_lat: float, start_lng: float, end_lat: float, end_lng: float) -> tuple[str, float]:
    """
    returns (encoded_polyline, distance_m). if google key missing, fall back to straight-line synthetic polyline.
    """
    if cfg.GOOGLE_MAPS_KEY:
        url = "https://maps.googleapis.com/maps/api/directions/json"
        params = dict(
            origin=f"{start_lat},{start_lng}",
            destination=f"{end_lat},{end_lng}",
            mode="walking",  # walking profile; we estimate pace by mode later
            key=cfg.GOOGLE_MAPS_KEY,
        )
        r = requests.get(url, params=params, timeout=cfg.HTTP_TIMEOUT_S)
        r.raise_for_status()
        data = r.json()
        routes = data.get("routes", [])
        if routes:
            leg = routes[0]["legs"][0]
            dist_m = leg["distance"]["value"]
            poly = routes[0]["overview_polyline"]["points"]
            return poly, float(dist_m)
    # fallback: straight segment polyline, approximate distance
    poly = _encode_polyline([(start_lat, start_lng), (end_lat, end_lng)])
    dist_m = _haversine_m(start_lat, start_lng, end_lat, end_lng)
    return poly, dist_m

def _synthetic_point(lat: float, lng: float, meters: float) -> tuple[float, float]:
    # simple bearing-based offset (~1 deg lat ≈ 111_320 m)
    bearing = random.uniform(0, 360) * math.pi / 180
    dlat = (meters * math.cos(bearing)) / 111_320.0
    dlng = (meters * math.sin(bearing)) / (111_320.0 * math.cos(lat * math.pi / 180))
    return lat + dlat, lng + dlng

def _haversine_m(lat1, lon1, lat2, lon2):
    R = 6371000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlmb = math.radians(lon2 - lon1)
    a = math.sin(dphi/2)**2 + math.cos(p1)*math.cos(p2)*math.sin(dlmb/2)**2
    return 2 * R * math.asin(math.sqrt(a))

# lightweight polyline encoder (2-point case ok; general case supported)
def _encode_polyline(latlngs: list[tuple[float, float]]) -> str:
    def encode_value(value):
        value = ~(value << 1) if value < 0 else (value << 1)
        chunks = []
        while value >= 0x20:
            chunks.append((0x20 | (value & 0x1f)) + 63)
            value >>= 5
        chunks.append(value + 63)
        return "".join(map(chr, chunks))

    result = []
    prev_lat = prev_lng = 0
    for lat, lng in latlngs:
        ilat = int(round(lat * 1e5))
        ilng = int(round(lng * 1e5))
        dlat = ilat - prev_lat
        dlng = ilng - prev_lng
        result.append(encode_value(dlat))
        result.append(encode_value(dlng))
        prev_lat, prev_lng = ilat, ilng
    return "".join(result)

def _keywords_from_prompt(prompt: str | None) -> list[str]:
    if not prompt:
        return ["park"]
    # keep it dead simple for v1
    words = [w.lower() for w in prompt.split() if w.isalpha()]
    base = ["park", "trail", "river", "quiet", "safe"]
    return list(dict.fromkeys([*words, *base]))[:5]

@tool
def plan_route(start_lat: float, start_lng: float, mode: str, distance_m: int | None = None, prompt: str = "") -> str:
    """
    Generate a one-way route to a nearby place. Returns compact JSON (string).
    """
    # validate & cap
    args = PlanRouteIn(start_lat=start_lat, start_lng=start_lng, mode=mode, distance_m=distance_m, prompt=prompt)
    target_m = min(args.distance_m or cfg.DEFAULT_SYNTHETIC_M, cfg.MAX_DISTANCE_M)

    # 1) try places → directions
    dest_src = "synthetic"
    dest_lat, dest_lng, dest_name = None, None, None
    if cfg.GOOGLE_MAPS_KEY:
        poi = _places_nearby(args.start_lat, args.start_lng, _keywords_from_prompt(args.prompt))
        if poi:
            geom = poi["geometry"]["location"]
            dest_lat, dest_lng = geom["lat"], geom["lng"]
            dest_name = poi.get("name")
            dest_src = "places"

    # no poi? synthesize a point roughly target_m away
    if dest_lat is None:
        dest_lat, dest_lng = _synthetic_point(args.start_lat, args.start_lng, target_m)
        dest_name = None
        dest_src = "synthetic"

    poly, route_m = _directions(args.start_lat, args.start_lng, dest_lat, dest_lng)
    # if we actually got directions from google, mark as directions (more precise)
    if dest_src != "synthetic" and cfg.GOOGLE_MAPS_KEY:
        dest_src = "directions"

    out = PlanRouteOut(
        polyline=poly,
        est_duration=_est_duration_min(args.mode, route_m),
        dest=Dest(lat=dest_lat, long=dest_lng, name=dest_name),
        source=dest_src,
    )
    return out.model_dump_json()
