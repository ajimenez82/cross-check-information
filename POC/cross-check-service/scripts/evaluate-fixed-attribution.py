"""Isolated attribution evaluation: fixed paraphrases, production attribution rules, no web.
Run with --run once; subsequent runs resume the saved session. No automatic POST retry.
This is not an end-to-end analysis or a reproduction of the full original articles.
"""
import argparse, hashlib, json, re, time, urllib.request, urllib.error
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CASES = json.loads((Path(__file__).with_name("fixed-attribution-cases.json")).read_text(encoding="utf-8-sig"))

def save(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding='utf-8')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', action='store_true')
    args = parser.parse_args()
    prompt = (ROOT/'infrastructure/src/main/resources/openai/unified/instructions.md').read_text(encoding='utf-8')
    rules = prompt.split("## Publication argument content", 1)[1].split('Preserve modal and evidential strength', 1)[0]
    instructions = "## Publication argument content" + rules + '\nIsolated evaluation: classify only the supplied fixed paraphrases, not the truth of the economic claim. No browsing. Return a JSON object with assessments: an array of objects with id, position, stanceOwner, explanation and attributionContext. Positions: SUPPORTS, QUESTIONS, MIXED, NO_EXPLICIT_POSITION. Preserve ambiguity rather than invent adoption.'
    payload = {'proposition':'La subida del SMI de 2019 redujo el empleo agregado frente al escenario sin subida.', 'materialType':'Human-reviewed paraphrases, not verbatim articles. Borderline cases require manual review.', 'cases':[{k:v for k,v in c.items() if k != 'expected'} for c in CASES]}
    fingerprint = hashlib.sha256((instructions+json.dumps(payload,sort_keys=True)).encode()).hexdigest()
    target = ROOT/'bootstrap/target/attribution-eval'/fingerprint[:12]
    target.mkdir(parents=True, exist_ok=True)
    save(target/'input.json', payload)
    save(target/'expectations.json', {c['id']:c['expected'] for c in CASES})
    print('Prepared fixed evaluation: '+str(target), flush=True)
    if not args.run: return
    config = (ROOT/'config/application-local.yml').read_text(encoding='utf-8-sig')
    def setting(name):
        match = re.search(r'^'+name+r':\s*(.*?)\s*$', config, re.M)
        if not match: raise SystemExit('Missing setting: '+name)
        return match.group(1).strip().strip('\"\'')
    key = setting('OPENAI_API_KEY')
    def request(path, body=None):
        req = urllib.request.Request('https://api.openai.com/v1/'+path, headers={'Authorization':'Bearer '+key,'OpenAI-Beta':'agents=v1','Content-Type':'application/json'}, data=None if body is None else json.dumps(body).encode())
        try:
            with urllib.request.urlopen(req, timeout=30) as response: return json.load(response)
        except urllib.error.HTTPError as exc: raise SystemExit('HTTP '+str(exc.code)+'; no automatic resubmission')
        except urllib.error.URLError: raise SystemExit('Network unavailable; no automatic resubmission')
    statefile = target/'state.json'
    if (target/'response.json').exists():
        result=json.loads((target/'response.json').read_text(encoding='utf-8'))
    else:
        if statefile.exists():
            state=json.loads(statefile.read_text(encoding='utf-8'))
            if not state.get('session'): raise SystemExit('Submission uncertain. Inspect state before any new submission.')
        else:
            state={'fingerprint':fingerprint,'model':'gpt-5.4','reasoning':'medium','revision':setting('AGENT_REVISION')}
            save(statefile,state)
            session=request('agents/sessions',{'agent_id':setting('OPENAI_POLITICAL_ANALYSIS_AGENT_ID'),'agent':{'model':'gpt-5.4','service_tier':'default','reasoning':{'effort':'medium'},'instructions':instructions,'tools':[],'multi_agent':{'enabled':False},'text':{'format':{'type':'json_schema','schema':{'type':'object','additionalProperties':False,'required':['assessments'],'properties':{'assessments':{'type':'array','items':{'type':'object','additionalProperties':False,'required':['id','position','stanceOwner','explanation','attributionContext'],'properties':{k:{'type':'string'} for k in ['id','position','stanceOwner','explanation','attributionContext']}}}}}}}},'environment':{'type':'none'},'input':json.dumps(payload,ensure_ascii=False),'stream':False})
            state['session']=session['id'];save(statefile,state)
        deadline=time.monotonic()+300
        while time.monotonic()<deadline:
            turns=request('agents/sessions/'+state['session']+'/turns?order=desc&limit=1')['data']
            if turns and turns[0]['status'] in ('completed','failed','cancelled'): break
            time.sleep(3)
        else: raise SystemExit('Still running. Rerun --run to resume.')
        turn=turns[0]
        save(target/'turn.json',turn)
        if turn['status']!='completed': raise SystemExit('Remote state: '+turn['status'])
        items=request('agents/sessions/'+state['session']+'/items?order=desc&limit=100')['data']
        messages=[i for i in items if i.get('turn_id')==turn['id'] and i.get('role')=='assistant' and i.get('phase')=='final_answer']
        if len(messages)!=1: raise SystemExit('Unexpected final message count')
        result=json.loads(''.join(p['text'] for p in messages[0]['content'] if p['type']=='output_text'))
        save(target/'response.json',result)
    rows=result['assessments']
    if sorted(r['id'] for r in rows)!=sorted(c['id'] for c in CASES): raise SystemExit('Invalid case coverage')
    failed=False
    for row in rows:
        expected=next(c['expected'] for c in CASES if c['id']==row['id'])
        outcome='REVIEW' if expected is None else ('PASS' if expected==row['position'] else 'FAIL')
        failed |= outcome=='FAIL'
        print(outcome+' '+row['id']+': '+row['position']+' | '+row['explanation'])
    if failed: raise SystemExit(1)

if __name__=='__main__': main()
