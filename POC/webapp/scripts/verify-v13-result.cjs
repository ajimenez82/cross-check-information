// Replay the redacted real result offline; never contact the provider.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const directory = path.resolve(__dirname, '../review/integration');
const response = JSON.parse(fs.readFileSync(path.join(directory, 'political-v13-result.json'), 'utf8'));
const app = process.env.APP_URL || 'http://127.0.0.1:5179';
(async () => {
 const browser = await chromium.launch({channel:'msedge',headless:true});
 const reviews = [];
 try {
  for (const width of [1536,390]) {
   const context = await browser.newContext({viewport:{width,height:1000}});
   const page = await context.newPage(); const errors=[]; let requests=0;
   page.on('pageerror', error=>errors.push(error.message));
   await page.route('**/api/**', route=> {
    if (new URL(route.request().url()).pathname.startsWith('/src/')) return route.continue();
    requests++;
    assert(route.request().url().endsWith('/api/analysis/jobs'));
    return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(response)});
   });
   await page.goto(app);
   await page.getByLabel('Tu consulta',{exact:true}).fill('¿Los límites al precio del alquiler reducen la oferta de vivienda en alquiler en España? Analiza el periodo 2023–2025 y contrasta publicaciones con distintas posiciones.');
   await page.getByRole('button',{name:'Analizar',exact:true}).click();
   await page.getByRole('heading',{name:'Valoración final',exact:true}).waitFor();
   const positions=page.locator('aside').filter({has:page.getByRole('heading',{name:'Posiciones en las publicaciones',exact:true})});
   assert((await positions.innerText()).includes('No calculable'));
   assert(!(await positions.innerText()).includes('%'));
   assert((await page.locator('[data-verdict="INSUFFICIENT_EVIDENCE"]').innerText()).includes('Evidencia insuficiente'));
   await positions.getByText('Publicaciones excluidas (1)',{exact:true}).click();
   assert((await positions.innerText()).includes('Antecedente'));
   const positionBox=await positions.boundingBox();
   const sourceBox=await page.getByRole('heading',{name:'Fuentes',exact:true}).boundingBox();
   const verdictBox=await page.getByRole('heading',{name:'Valoración final',exact:true}).boundingBox();
   if(width===390) { assert(positionBox.y>verdictBox.y);assert(sourceBox.y>positionBox.y+positionBox.height-1); }
   else assert(positionBox.x>verdictBox.x);
   assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
   await page.screenshot({path:path.join(directory,`political-v13-${width}.png`),fullPage:true});
   await page.getByRole('button',{name:/Ver referencia/}).first().click();
   await page.getByRole('dialog').waitFor();
   assert((await page.getByRole('dialog').innerText()).includes('Banco de España'));
   await page.screenshot({path:path.join(directory,`political-v13-source-${width}.png`),fullPage:true});
   await page.getByRole('button',{name:'Cerrar',exact:true}).click();
   await page.reload();
   await page.getByRole('heading',{name:'Valoración final',exact:true}).waitFor();
   assert.equal(requests,1);assert.deepEqual(errors,[]);
   reviews.push({width,passed:true,apiRequestsIntercepted:requests,percentagesAbsent:true,sourceDialog:true,persisted:true});
   await context.close();
  }
  fs.writeFileSync(path.join(directory,'political-v13-ui-review.json'),JSON.stringify(reviews,null,2));
  console.log('PASS: real v13 result replayed at 1536 and 390px; no percentages, source dialog, layout and persistence verified; no provider calls.');
 } finally { await browser.close(); }
})().catch(error=>{console.error(error);process.exitCode=1;});
