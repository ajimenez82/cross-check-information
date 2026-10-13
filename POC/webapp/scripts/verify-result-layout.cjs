// Offline regression for the multiple-claim branch of the result layout.
const {chromium}=require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const {spawn}=require('node:child_process');
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const web=path.resolve(__dirname,'..');
(async()=>{
 const net=require('node:net');
 await new Promise((resolve,reject)=>{const s=net.createServer();s.once('error',reject);s.listen(5181,'127.0.0.1',()=>s.close(resolve));});
 const server=spawn(process.execPath,['node_modules/vite/bin/vite.js','--host','127.0.0.1','--port','5181','--strictPort'],{cwd:web,windowsHide:true,env:{...process.env,API_PROXY_TARGET:'http://127.0.0.1:1'},stdio:'ignore'});
 let browser;
 try{
  for(let i=0;i<60;i++){try{if((await fetch('http://127.0.0.1:5181')).ok)break;}catch{}await new Promise(r=>setTimeout(r,250));}
  browser=await chromium.launch({channel:'msedge',headless:true});
  const page=await browser.newPage({viewport:{width:390,height:1000}});
  const raw=JSON.parse(fs.readFileSync(path.resolve(web,'../cross-check-service/bootstrap/src/test/resources/openai/claim-report-example.json'),'utf8'));
  const data={...raw,analyzedAt:new Date().toISOString(),asOf:null,verdict:{status:'NO_SINGLE_VERDICT',explanation:'Caso sintético con conclusiones diferentes.',documentarySupport:'Datos ficticios para comprobar la interfaz.',sourceIds:[]},
   publicationPositions:{availability:'UNAVAILABLE',reason:'Caso de varias afirmaciones.',proposition:null,selectionCriteria:null,period:null,consultedAt:null,units:[],excluded:[]}};
  await page.goto('http://127.0.0.1:5181');
  const id='00000000-0000-4000-8000-000000000001';
  await page.evaluate(({id,data})=>localStorage.setItem('crosscheck.conversations.v1',JSON.stringify([{id,title:'Prueba sintética de varias afirmaciones',token:null,turns:[{id:'turn1',text:'Ejemplo ficticio de frecuencia y precio',createdAt:new Date().toISOString(),status:'done',analysis:data}]}])),{id,data});
  await page.goto('http://127.0.0.1:5181/resultados/'+id);
  await page.getByRole('heading',{name:'Valoración por afirmaciones',exact:true}).waitFor();
  for(const claim of raw.claimAnalysis.claims) assert(await page.getByRole('heading',{name:claim.proposition,exact:true}).isVisible());
  assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  await page.getByText('Ver explicación documental completa',{exact:true}).focus();
  await page.keyboard.press('Enter');
  assert(await page.getByText(data.verdict.documentarySupport,{exact:true}).isVisible());
  console.log('PASS multiple-claim breakdown, mobile width and keyboard disclosure');
 }finally{if(browser)await browser.close();server.kill();}
})().catch(error=>{console.error(error);process.exitCode=1});
