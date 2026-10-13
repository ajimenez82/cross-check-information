const {chromium}=require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {spawn}=require('node:child_process');
const web=path.resolve(__dirname,'..'), service=path.resolve(web,'../cross-check-service');
const logs=path.join(service,'bootstrap/target');
const java=process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME,'bin/java') : 'java';
const scenarios=process.env.REPLAY_SCENARIOS ? process.env.REPLAY_SCENARIOS.split(',') : ['PLUS_ULTRA_V19','VOTO_OBLIGATORIO_V19','ALQUILERES_V19','ACLARACION_V19','PLUS_ULTRA_V20'];
const delay=ms=>new Promise(r=>setTimeout(r,ms));
async function ready(url,child){for(let i=0;i<60;i++){if(child.exitCode!==null)throw Error('Server exited');try{if((await fetch(url)).ok)return;}catch{}await delay(500);}throw Error('Server not ready');}
function start(command,args,cwd,name,env={}){const log=fs.openSync(path.join(logs,name+'.log'),'w');return spawn(command,args,{cwd,windowsHide:true,env:{...process.env,...env},stdio:['ignore',log,log]});}
async function freePort(port) {
 const net=require('node:net');
 await new Promise((resolve,reject)=>{const probe=net.createServer();
 probe.once('error',reject);probe.listen(port,'127.0.0.1',()=>probe.close(resolve));});
}
(async()=>{
 await freePort(8097); await freePort(5179);
 const vite=start(process.execPath,['node_modules/vite/bin/vite.js','--host','127.0.0.1','--port','5179','--strictPort'],web,'replay-vite',{API_PROXY_TARGET:'http://127.0.0.1:8097'});
 let browser,server;
 try{
 await ready('http://127.0.0.1:5179',vite);
 browser=await chromium.launch({channel:'msedge',headless:true});
 for(const scenario of scenarios){
  const metadata=JSON.parse(fs.readFileSync(path.join(service,'infrastructure/src/main/resources/openai/replay',scenario,'metadata.json'),'utf8'));
  server=start(java,['-jar','bootstrap/target/cross-check-bootstrap-0.0.1-SNAPSHOT.jar','--spring.profiles.active=replay,local','--server.port=8097','--crosscheck.replay.scenario='+scenario,'--crosscheck.analysis.jobs.database-path=bootstrap/target/replay-browser/'+scenario],service,'replay-'+scenario,{OPENAI_API_KEY:''});
  await ready('http://127.0.0.1:8097/actuator/health',server);
  const page=await browser.newPage({viewport:{width:1536,height:1100}});
  let terminal,posts=0;
  page.on('response',async response=>{if(!response.url().includes('/api/analysis/jobs'))return;try{
   if(response.request().method()==='POST')posts++;
   const body=await response.json();if(['COMPLETED','FAILED'].includes(body.status))terminal=body;
  }catch{}});
  await page.goto('http://127.0.0.1:5179');
  await page.getByLabel('Tu consulta',{exact:true}).fill(metadata.query);
  await page.getByRole('button',{name:'Analizar',exact:true}).click();
  for(let i=0;i<60&&!terminal;i++)await delay(500);
  assert(terminal,'No terminal result');assert.equal(posts,1);
  if(terminal.result?.conversationToken)terminal.result.conversationToken='[redacted]';
  const destination=path.join(web,'review/integration/verdict-first');fs.mkdirSync(destination,{recursive:true});
  fs.writeFileSync(path.join(destination,scenario+'.json'),JSON.stringify({scenario,query:metadata.query,posts,terminal},null,2));
  assert.equal(terminal.status,'COMPLETED',scenario+': '+terminal.error?.code);
  const heading=terminal.result.analysis?'Valoración final':'Necesito una aclaración';
  await page.getByRole('heading',{name:heading,exact:true}).waitFor();
  await page.reload();await page.getByRole('heading',{name:heading,exact:true}).waitFor();
  if(terminal.result.analysis){
   const analysis=terminal.result.analysis;
   assert.equal(await page.getByRole('heading',{name:'Valoración por afirmaciones',exact:true}).count(),analysis.claimAnalysis?.claims.length>1?1:0);
   const verdict=await page.locator('section[aria-label="Valoración final"]').boundingBox();
   const body=await page.locator('section[aria-label="Análisis"]').boundingBox();
   assert(verdict.y<body.y,'Verdict must precede analysis');
   assert.equal(await page.locator('section[aria-label="Fuentes"] button:visible').count(),Math.min(4,analysis.sources.length));
   if(analysis.sources.length>4){
    await page.getByText('Ver las otras '+(analysis.sources.length-4)+' fuentes',{exact:true}).click();
    assert.equal(await page.locator('section[aria-label="Fuentes"] button:visible').count(),analysis.sources.length);
    await page.locator('section[aria-label="Fuentes"] button').last().click();
    await page.getByRole('dialog').waitFor();
    assert((await page.getByRole('dialog').innerText()).includes(analysis.sources.at(-1).contribution));
    await page.getByRole('button',{name:'Cerrar',exact:true}).click();
    await page.getByText('Ver las otras '+(analysis.sources.length-4)+' fuentes',{exact:true}).click();
   }
   await page.getByText('Qué estamos analizando',{exact:true}).click();
   assert(await page.getByText(analysis.context,{exact:true}).isVisible());
   await page.getByText('Qué estamos analizando',{exact:true}).click();
  }
  await page.evaluate(()=>{document.activeElement?.blur();window.scrollTo(0,0);});
  await page.waitForTimeout(100);
  await page.screenshot({path:path.join(destination,scenario+'-desktop.png'),fullPage:true});
  await page.setViewportSize({width:390,height:1000});assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  if(terminal.result.analysis){
   const content=await page.locator('article').boundingBox();
   const positions=await page.locator('aside').last().boundingBox();
   const sources=await page.locator('section[aria-label="Fuentes"]').boundingBox();
   assert(content.y+content.height<=positions.y+1 && positions.y+positions.height<=sources.y+1,'Mobile order must be analysis, positions, sources');
  }
  await page.evaluate(()=>{document.activeElement?.blur();window.scrollTo(0,0);});
  await page.waitForTimeout(100);
  await page.screenshot({path:path.join(destination,scenario+'-mobile.png'),fullPage:true});
  console.log(JSON.stringify({scenario,status:terminal.status,verdict:terminal.result.analysis?.verdict.status,clarification:!!terminal.result.clarification}));
  await page.close();server.kill();await delay(1000);server=null;
 }
 }finally{if(server)server.kill();if(browser)await browser.close();vite.kill();}
})().catch(error=>{console.error(error.message);process.exitCode=1;});
