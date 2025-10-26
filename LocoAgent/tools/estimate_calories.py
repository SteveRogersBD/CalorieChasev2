import requests
from pydantic import BaseModel, Field, field_validator
from strands import tool
from agent.config import cfg

class EstIn(BaseModel):
    user_id: str
    mode: str = Field(pattern="^(walk|jog|run)$")
    duration_min: float | None = None
    distance_m: float | None = None
    elev_gain_m: float | None = 0.0

    @field_validator("duration_min", "distance_m", mode="before")
    @classmethod
    def nonneg(cls, v):
        if v is None:
            return v
        if float(v) < 0:
            raise ValueError("must be non-negative")
        return float(v)

PACE_DEFAULT = {"walk": 13.5, "jog": 7.5, "run": 5.5}  # min/km
MET_BASE = {"walk": 3.5, "jog": 7.0, "run": 10.0}

def _http_get(url: str):
    r = requests.get(url, timeout=cfg.HTTP_TIMEOUT_S)
    r.raise_for_status()
    return r.json()

def _http_post(url: str, payload: dict):
    r = requests.post(url, json=payload, timeout=cfg.HTTP_TIMEOUT_S)
    r.raise_for_status()
    return r.json()

def _pace_min_per_km(duration_min: float | None, distance_m: float | None, mode: str) -> float:
    if duration_min and distance_m and distance_m > 0:
        return round(duration_min / (distance_m / 1000.0), 2)
    # derive from default pace and whichever one is present
    return PACE_DEFAULT[mode]

def _met_with_elev(base_met: float, elev_gain_m: float, distance_m: float | None) -> float:
    if not distance_m or distance_m <= 0:
        return base_met
    km = max(distance_m / 1000.0, 0.001)
    per_km_gain = (elev_gain_m or 0.0) / km
    adj = base_met * (1.0 + 0.01 * (per_km_gain / 10.0))  # +1% per 10 m gain/km
    return round(adj, 2)

@tool
def estimate_calories(user_id: str, mode: str, duration_min: float | None = None,
                      distance_m: float | None = None, elev_gain_m: float | None = 0.0) -> str:
    """
    Compute calories using METs. Side-effects: creates Activity + Score via your services.
    Returns compact JSON (string).
    """
    args = EstIn(user_id=user_id, mode=mode, duration_min=duration_min, distance_m=distance_m, elev_gain_m=elev_gain_m)

    # 1) get user profile
    user = _http_get(f"{cfg.USER_SVC_URL}/user/{args.user_id}")
    weight_kg = float(user.get("weight_kg") or user.get("weight") or 70)

    # 2) derive pace and MET
    pace = _pace_min_per_km(args.duration_min, args.distance_m, args.mode)  # min/km
    met = _met_with_elev(MET_BASE[args.mode], args.elev_gain_m or 0.0, args.distance_m)

    # 3) energy formula (ACSM variant)
    dur = float(args.duration_min) if args.duration_min else (PACE_DEFAULT[args.mode] * ((args.distance_m or 0) / 1000.0))
    kcal = (met * 3.5 * weight_kg / 200.0) * dur
    result = {
        "mode_used": args.mode,
        "pace_min_per_km": round(pace, 2),
        "met_eff": round(met, 2),
        "kcal": round(kcal, 1),
    }

    # 4) persist activity + score (best-effort; ignore errors in v1)
    try:
        activity_payload = {
            "userId": args.user_id,
            "mode": args.mode,
            "distance_m": args.distance_m,
            "duration_min": dur,
            "elev_gain_m": args.elev_gain_m or 0.0,
            "calories_kcal": result["kcal"],
            "met": result["met_eff"],
        }
        _http_post(f"{cfg.ACTIVITY_SVC_URL}/activities", activity_payload)
    except Exception:
        pass

    try:
        score_payload = {
            "userId": args.user_id,
            "sessionScore": max(int(result["kcal"] // 10), 1)  # trivial v1 formula
        }
        _http_post(f"{cfg.SCORE_SVC_URL}/scores", score_payload)
    except Exception:
        pass

    # return compact json for the app
    import json
    return json.dumps(result)

