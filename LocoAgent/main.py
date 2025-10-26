import os
from dotenv import load_dotenv
from strands import Agent
from bedrock_agentcore.runtime import BedrockAgentCoreApp
from bedrock_agentcore.memory.integrations.strands.session_manager import AgentCoreMemorySessionManager
from bedrock_agentcore.memory.integrations.strands.config import AgentCoreMemoryConfig, RetrievalConfig

# load env early
load_dotenv()

from agent.config import cfg  # after env is loaded
from tools.plan_route import plan_route
from tools.estimate_calories import estimate_calories

app = BedrockAgentCoreApp()

SYSTEM_PROMPT = (
    "you are the caloriechase agent. you ONLY have two tools: plan_route and "
    "estimate_calories. when a new session is created, call plan_route once; "
    "if no poi found, use fallback. when a session ends and stats are provided, "
    "call estimate_calories and return a compact json (no prose). "
    "validate lat/lng and cap distance to MAX_DISTANCE_M."
)

def _build_agent(session_id: str, actor_id: str):
    memory_config = AgentCoreMemoryConfig(
        memory_id=os.getenv("BEDROCK_AGENTCORE_MEMORY_ID", "disabled"),  # ok if missing for local
        session_id=session_id,
        actor_id=actor_id,
        retrieval_config={
            f"/users/{actor_id}/preferences": RetrievalConfig(top_k=3, relevance_score=0.5),
        },
    )
    return Agent(
        model=cfg.MODEL_ID,
        system_prompt=SYSTEM_PROMPT,
        tools=[plan_route, estimate_calories],
        session_manager=AgentCoreMemorySessionManager(memory_config, cfg.AWS_REGION),
    )

@app.entrypoint
def invoke(payload: dict, context=None):
    """
    AgentCore entrypoint. Expects payload like:
      {"prompt": "...", "start_lat": ..., "start_lng": ..., "mode": "...", ...}
    Returns compact JSON strings for the app to parse.
    """
    actor_id = "user"
    session_id = "default"
    if context is not None:
        # AgentCore provides these; safe defaults for local run
        actor_id = getattr(context, "actor_id", actor_id)
        session_id = getattr(context, "session_id", session_id)

    agent = _build_agent(session_id, actor_id)
    prompt = payload.get("prompt", "")
    result = agent(prompt)
    # strands returns a rich object; normalize to raw text
    text = result.message.get("content", [{}])[0].get("text", str(result))
    return {"response": text}

if __name__ == "__main__":
    # local dev server (useful before agentcore launch)
    # you can `python main.py` and curl:
    #   curl -X POST localhost:8000/invoke -d '{"prompt":"plan a 1km jog from ..."}'
    app.run()
