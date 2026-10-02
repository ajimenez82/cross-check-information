"""Offline contract checks. Requires jsonschema 4.26.0; never calls OpenAI."""
import copy
import json
from pathlib import Path

from jsonschema import Draft202012Validator

service = Path(__file__).resolve().parents[1]
workspace = service.parents[1]


def read(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


template = read(service / "docs/openai/political-agent.template.json")
draft = read(workspace / "Docs/proposals/political-report-v2.schema.json")
assessment_schema = read(workspace / "Docs/proposals/publication-assessment.schema.json")
example = read(workspace / "Docs/proposals/political-report-v2.example.json")
invalid_live = read(service / "infrastructure/src/test/resources/openai/traced-report-v7-invalid.json")
checks = 0
for schema in (template["text"]["format"]["schema"], draft):
    Draft202012Validator.check_schema(schema)
    validator = Draft202012Validator(schema)
    validator.validate(example)
    assert not validator.is_valid(invalid_live), "The original contradictory report must be rejected"
    checks += 2
    for relation in ("REQUESTED_PERIOD", "RETROSPECTIVE"):
        sample = copy.deepcopy(example)
        for source in sample["sources"]:
            source["publishedAt"] = None
        unit = next(a for a in sample["publicationPositions"]["assessments"] if a["decision"] == "COUNT")
        unit["trace"]["temporalRelation"] = relation
        validator.validate(sample)
        checks += 1
    for field, values in (("match", ("PARTIAL", "OUT_OF_SCOPE", "UNCERTAIN")),
                          ("temporalRelation", ("BACKGROUND", "UNKNOWN"))):
        for value in values:
            sample = copy.deepcopy(example)
            unit = next(a for a in sample["publicationPositions"]["assessments"] if a["decision"] == "COUNT")
            target = unit["trace"]["scope"] if field == "match" else unit["trace"]
            target[field] = value
            assert not validator.is_valid(sample), f"COUNT with {value} must fail"
            # A synthetic explicit exclusion must remain representable, without changing its trace.
            unit["decision"] = "EXCLUDE"
            unit["position"] = None
            validator.validate(sample)
            checks += 2

Draft202012Validator.check_schema(assessment_schema)
for item in read(workspace / "Docs/proposals/publication-assessment.examples.json")["cases"]:
    Draft202012Validator(assessment_schema).validate(item)
    checks += 1
version_three = read(workspace / "Docs/proposals/political-report-v3.schema.json")
Draft202012Validator.check_schema(version_three)
v3 = Draft202012Validator(version_three)
v3_example = read(workspace / "Docs/proposals/political-report-v3.example.json")
v3.validate(v3_example)
checks += 1
invalid = copy.deepcopy(v3_example)
invalid['verdict'] = invalid['claimAnalysis']['claims'][0]['verdict']
assert not v3.is_valid(invalid), 'Provider v3 must not send a global verdict'
checks += 1
invalid = copy.deepcopy(v3_example)
invalid['claimAnalysis']['claims'][0]['verdict']['status'] = 'NO_SINGLE_VERDICT'
assert not v3.is_valid(invalid), 'Individual claims cannot select a global verdict'
checks += 1
print(f"PASS: {checks} offline schema checks; no inference executed")
response_schema = read(workspace / 'Docs/proposals/political-response-v3.schema.json')
Draft202012Validator.check_schema(response_schema)
response_validator = Draft202012Validator(response_schema)
for name in ('political-response-v3.example.json', 'political-clarification-v3.example.json'):
    response_validator.validate(read(workspace / 'Docs/proposals' / name))
invalid = read(workspace / 'Docs/proposals/political-clarification-v3.example.json')
invalid['clarification']['reason'] = 'CONTEXT_UNAVAILABLE'
assert not response_validator.is_valid(invalid), 'The provider cannot claim lost server context'
print('PASS: 3 additional v3 response schema checks; exclusive result choice is enforced by Java')

# Verify the complete unpublished definition against the implemented response contract.
v3_template = read(service / "docs/openai/political-agent-v3.template.json")
assert v3_template["text"]["format"]["schema"] == response_schema
for field in ("name", "model", "service_tier", "reasoning", "tools"):
    assert v3_template[field] == template[field], f"Unexpected v3 change: {field}"
assert response_schema["properties"]["analysis"]["anyOf"][0] == version_three
print("PASS: v3 template matches the response schema, inner report and existing agent settings")


for stage in ("research", "synthesis"):
    schema = read(service / f"infrastructure/src/main/resources/openai/evidence/{stage}.schema.json")
    Draft202012Validator.check_schema(schema)
    Draft202012Validator(schema).validate(read(service / f"infrastructure/src/test/resources/openai/evidence/{stage}.json"))
print("PASS: research and synthesis schemas and offline examples; no inference executed")

synthesis_validator = Draft202012Validator(read(service / "infrastructure/src/main/resources/openai/evidence/synthesis.schema.json"))
for reason in (None, "", "   ", "\n\t"):
    invalid = read(service / "infrastructure/src/test/resources/openai/evidence/synthesis.json")
    invalid["publicationPositions"]["reason"] = reason
    assert not synthesis_validator.is_valid(invalid), "Synthesis reason must contain non-whitespace text"
invalid = read(service / "infrastructure/src/test/resources/openai/evidence/synthesis.json")
del invalid["publicationPositions"]["reason"]
assert not synthesis_validator.is_valid(invalid), "Synthesis reason is mandatory"
print("PASS: synthesis rejects missing, null, empty and whitespace-only reasons")
