from strands import agent, tool, Agent
from bedrock_agentcore import BedrockAgentCoreApp

# ----------------------------------
# 1️⃣ define a tool
# ----------------------------------
@tool()
def greet(name: str) -> str:
    """Return a friendly greeting."""
    return f"Hello {name}! I'm your Bedrock Agent 🚀"

# ----------------------------------
# 2️⃣ Create the Agent
# ----------------------------------
def create_agent():
    """
    Creates an Agent using Bedrock's Agent() constructor.
    """
    model = "amazon.nova-micro-v1:0"  # change to the model you have access to

    # instantiate the Agent with its model and tools
    my_agent = Agent(
        model=model,
        tools=[greet],
        description="A simple demo Bedrock Agent that greets users."
    )
    return my_agent


# ----------------------------------
# 3️⃣ define entrypoint for AgentCore
# ----------------------------------
app = BedrockAgentCoreApp()

@app.entrypoint
def invoke(request: dict):
    """
    AgentCore entrypoint: receives JSON -> calls agent -> returns response
    """
    user_name = request.get("name", "friend")
    my_agent = create_agent()

    # ask the agent to use its tool to greet
    result = my_agent.run(f"Please greet {user_name}")
    return {"ok": True, "output": result}


# ----------------------------------
# 4️⃣ local dev server
# ----------------------------------
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
