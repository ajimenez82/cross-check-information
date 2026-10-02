"""Executable design only, not application code. Requires jsonschema 4.26.0."""
import json
from pathlib import Path

from jsonschema import Draft202012Validator, ValidationError

workspace = Path(__file__).resolve().parents[3]
proposal = workspace / "Docs/proposals"
schema = json.loads((proposal / "claim-assessment.schema.json").read_text(encoding="utf-8-sig"))
Draft202012Validator.check_schema(schema)
validator = Draft202012Validator(schema)


def derive_documentary_verdict(payload, input_text, source_ids):
    validator.validate(payload)
    claims = payload["claims"]
    decomposition = payload["decomposition"]
    if decomposition["kind"] == "SINGLE":
        if len(claims) != 1 or decomposition["basis"] != "SINGLE_PROPOSITION":
            raise ValueError("Invalid single decomposition")
    elif len(claims) < 2 or decomposition["basis"] == "SINGLE_PROPOSITION":
        raise ValueError("Invalid multiple decomposition")
    ids, propositions, spans = set(), set(), []
    for claim in claims:
        normalized = " ".join(claim["proposition"].casefold().split())
        if claim["id"] in ids or normalized in propositions:
            raise ValueError("Duplicate claim")
        ids.add(claim["id"])
        propositions.add(normalized)
        excerpt = claim["inputExcerpt"]
        start = input_text.find(excerpt)
        if start < 0 or input_text.find(excerpt, start + 1) >= 0:
            raise ValueError("Missing or ambiguous input anchor")
        end = start + len(excerpt)
        if any(start < previous_end and previous_start < end for previous_start, previous_end in spans):
            raise ValueError("Overlapping input anchors")
        spans.append((start, end))
        if not set(claim["verdict"]["sourceIds"]).issubset(source_ids):
            raise ValueError("Unknown evidence reference")
    statuses = {claim["verdict"]["status"] for claim in claims}
    return next(iter(statuses)) if len(statuses) == 1 else "NO_SINGLE_VERDICT"


def main():
    cases = json.loads((proposal / "claim-assessment.examples.json").read_text(encoding="utf-8-sig"))["cases"]
    for case in cases:
        try:
            actual = derive_documentary_verdict(case["payload"], case["inputText"], set(case["sourceIds"]))
        except (ValueError, ValidationError):
            actual = "REJECT"
        if actual != case["expected"]:
            raise AssertionError(f"{case['name']}: expected {case['expected']}, got {actual}")
        print(f"PASS {case['name']}: {actual}")
    print(f"PASS: {len(cases)} design cases; no production change or inference")


if __name__ == "__main__":
    main()
