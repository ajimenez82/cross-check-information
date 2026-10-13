"""Capture one real agent response per named scenario; reruns resume saved sessions."""
from pathlib import Path
import argparse, datetime, hashlib, json, re, time, urllib.request

parser = argparse.ArgumentParser()
parser.add_argument("--scenario", required=True)
parser.add_argument("--query", required=True)
args = parser.parse_args()
if not re.fullmatch(r"[A-Z][A-Z0-9_]{0,63}", args.scenario):
    raise SystemExit("Invalid scenario name")
service = Path(__file__).resolve().parents[1]
target = service / "bootstrap/target/replay-capture" / args.scenario
output = service / "infrastructure/src/main/resources/openai/replay" / args.scenario
target.mkdir(parents=True, exist_ok=True)
output.mkdir(parents=True, exist_ok=True)
config = (service / "config/application-local.yml").read_text(encoding="utf-8-sig")
def setting(name):
    match = re.search(r"^" + name + r":\s*(.*?)\s*$", config, re.M)
    if not match:
        raise SystemExit("Missing local setting: " + name)
    return match.group(1).strip().strip("\"'")
key = setting("OPENAI_API_KEY")
agent = setting("OPENAI_POLITICAL_ANALYSIS_AGENT_ID")
revision = setting("POLITICAL_AGENT_REVISION")
prepared = json.loads((service / "bootstrap/target/political-agent-unified.request.json").read_text(encoding="utf-8-sig"))
def request(path, body=None):
    req = urllib.request.Request("https://api.openai.com/v1/" + path,
        headers={"Authorization": "Bearer " + key, "OpenAI-Beta": "agents=v1", "Content-Type": "application/json"},
        data=None if body is None else json.dumps(body).encode())
    with urllib.request.urlopen(req, timeout=60) as response:
        return json.load(response)
def save(path, value):
    path.write_bytes(json.dumps(value, ensure_ascii=False, indent=2).encode("utf-8"))
def pages(path):
    rows = []
    while True:
        data = request(path)
        rows.extend(data["data"])
        if not data.get("has_more"):
            return rows
        path = path.split("&after=")[0] + "&after=" + data["last_id"]
state_file = target / "state.json"
if (output / "response.json").exists():
    if not (output / "metadata.json").exists():
        raise SystemExit("Incomplete capture metadata; recover the saved session manually")
    existing = json.loads((output / "metadata.json").read_text(encoding="utf-8"))
    if existing["query"] != args.query or existing["revision"] != revision:
        raise SystemExit("Existing capture has another query or revision; choose a new scenario")
    print(args.scenario + ": already captured; no inference submitted", flush=True)
    raise SystemExit(0)
if state_file.exists():
    state = json.loads(state_file.read_text())
    if state["query"] != args.query or state["revision"] != revision:
        raise SystemExit("Capture identity differs; choose a new scenario")
    if not state.get("session"):
        raise SystemExit("Submission uncertain; reconcile manually, do not resubmit")
else:
    state = {"query": args.query, "revision": revision, "startedAt": datetime.datetime.now(datetime.timezone.utc).isoformat()}
    save(state_file, state)
    session = request("agents/sessions", {"agent_id": agent, "agent": prepared,
        "environment": {"type": "none"}, "metadata": {"replay_scenario": args.scenario},
        "input": json.dumps({"currentInput": args.query, "previousContext": None}, ensure_ascii=False), "stream": False})
    state["session"] = session["id"]
    save(state_file, state)
print(args.scenario + ": observing saved session", flush=True)
deadline = time.monotonic() + 600
while time.monotonic() < deadline:
    turns = pages("agents/sessions/" + state["session"] + "/turns?order=asc&limit=100")
    if turns and turns[-1]["status"] in ("completed", "failed", "cancelled", "waiting"):
        break
    time.sleep(5)
else:
    raise SystemExit("Still running; rerun to resume without submitting again")
turn = turns[-1]
if turn["status"] != "completed":
    save(target / "failure.json", {"status": turn["status"]})
    raise SystemExit("Remote state: " + turn["status"])
items = pages("agents/sessions/" + state["session"] + "/items?order=asc&limit=100")
messages = [i for i in items if i.get("turn_id") == turn["id"] and i.get("role") == "assistant" and i.get("phase") == "final_answer"]
if len(messages) != 1:
    raise SystemExit("Unexpected final response count")
raw = "".join(part["text"] for part in messages[0]["content"] if part["type"] == "output_text")
(output / "response.json").write_bytes(raw.encode("utf-8"))
save(output / "metadata.json", {"scenario": args.scenario, "query": args.query, "revision": revision,
    "model": prepared["model"], "startedAt": state["startedAt"],
    "capturedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "instructionsSha256": hashlib.sha256(prepared["instructions"].encode()).hexdigest(),
    "responseSha256": hashlib.sha256(raw.encode()).hexdigest(),
    "usage": turn.get("usage"), "remoteStatus": turn["status"], "previousContext": None})
print(args.scenario + ": original output saved", flush=True)
