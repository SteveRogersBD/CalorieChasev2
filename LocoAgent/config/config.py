import os
from dataclasses import dataclass
from dotenv import load_dotenv

load_dotenv()

@dataclass(frozen=True)
class Config:
    AWS_REGION: str = os.getenv("AWS_REGION", "us-east-1")
    MODEL_ID: str = os.getenv("MODEL_ID", "us.anthropic.claude-3-7-sonnet-20250219-v1:0")

    GOOGLE_MAPS_KEY: str = os.getenv("GOOGLE_MAPS_KEY", "")

    USER_SVC_URL: str = os.getenv("USER_SVC_URL", "")
    TRACK_SVC_URL: str = os.getenv("TRACK_SVC_URL", "")
    ACTIVITY_SVC_URL: str = os.getenv("ACTIVITY_SVC_URL", "")
    SCORE_SVC_URL: str = os.getenv("SCORE_SVC_URL", "")

    MAX_DISTANCE_M: int = int(os.getenv("MAX_DISTANCE_M", "20000"))
    DEFAULT_SYNTHETIC_M: int = int(os.getenv("DEFAULT_SYNTHETIC_M", "900"))
    HTTP_TIMEOUT_S: int = int(os.getenv("HTTP_TIMEOUT_S", "12"))

cfg = Config()
