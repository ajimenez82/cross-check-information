const { completedJob } = require('./job-fixture.cjs');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');

(async () => {
  assert(process.env.TEST_API_URL, 'Run from OpenAiApiTest with its local upstream');
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  let savedResponse;
  try {
    for (const width of [1536, 390]) {
      const page = await browser.newPage({ viewport: { width, height: 1000 } });
      let calls = 0;
      await page.route('**/api/analysis/jobs', async route => {
        calls++;
        const response = await fetch(`${process.env.TEST_API_URL}/api/analysis/start`, {
          method: 'POST', headers: { 'Content-Type': 'application/json' }, body: route.request().postData(),
        });
        const body = await response.text();
        if (response.ok) savedResponse = JSON.parse(body);
        await route.fulfill({ status: response.ok ? 200 : response.status, contentType: 'application/json', body: response.ok ? JSON.stringify(completedJob(JSON.parse(body))) : body });
      });
      await page.goto(process.env.APP_URL || 'http://127.0.0.1:5179');
      await page.getByRole('textbox', { name: 'Tu consulta', exact: true }).fill('La medida aumentó la frecuencia y redujo el precio.');
      await page.getByRole('button', { name: 'Analizar', exact: true }).click();
      const claims = page.getByRole('region', { name: 'Valoración por afirmaciones' });
      await claims.getByRole('heading', { name: 'aumentó la frecuencia', exact: true }).waitFor();
      await claims.getByText('Respaldada', { exact: true }).waitFor();
      await claims.getByText('Refutada', { exact: true }).waitFor();
      await page.getByText('Sin veredicto único', { exact: true }).waitFor();
      await page.getByText('Medición no disponible', { exact: true }).waitFor();
      await claims.getByRole('button').first().click();
      await page.getByRole('dialog').waitFor();
      await page.getByRole('button', { name: 'Cerrar', exact: true }).click();
      await page.getByRole('heading', { name: 'Valoración por afirmaciones', exact: true }).click();
      await page.evaluate(() => { document.activeElement?.blur(); window.scrollTo(0, 0); });
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'No horizontal overflow');
      await page.screenshot({ path: `../../webapp/review/integration/claims-${width}.png`, fullPage: true });
      await page.reload();
      await claims.getByText('Refutada', { exact: true }).waitFor();
      assert.equal(calls, 1, 'Reload must use saved history');
      await page.getByLabel('Pregunta de seguimiento').fill('¿Y después?');
      await page.getByRole('button', { name: 'Enviar seguimiento', exact: true }).click();
      await page.getByRole('region', { name: 'Aclaración necesaria' }).waitFor();
      assert.equal(await page.getByRole('heading', { name: 'Valoración final', exact: true }).count(), 1);
      await page.reload();
      await page.getByText('¿Qué periodo posterior quieres analizar?', { exact: true }).waitFor();
      assert.equal(calls, 2, 'Reload of clarification must not resubmit');
      await page.evaluate(() => { document.activeElement?.blur(); window.scrollTo(0, 0); });
      await page.screenshot({ path: `../../webapp/review/integration/clarification-${width}.png`, fullPage: true });
      await page.getByLabel('Pregunta de seguimiento').fill('Entre 2026 y 2027');
      await page.getByRole('button', { name: 'Enviar seguimiento', exact: true }).click();
      await page.getByRole('heading', { name: 'Valoración final', exact: true }).nth(1).waitFor();
      assert.equal(calls, 3, 'One request per submitted message');
      await page.close();
    }
    for (const mode of ['unknown-reference', 'individual-global-code', 'wrong-aggregate', 'legacy-missing', 'legacy-null', 'lost-context', 'both-results']) {
      const page = await browser.newPage();
      const body = structuredClone(savedResponse);
      const claims = body.analysis.claimAnalysis;
      if (mode === 'unknown-reference') claims.claims[0].verdict.sourceIds = ['UNKNOWN'];
      if (mode === 'individual-global-code') claims.claims[0].verdict.status = 'NO_SINGLE_VERDICT';
      if (mode === 'wrong-aggregate') body.analysis.verdict.status = 'SUPPORTED';
      if (mode === 'legacy-missing') delete body.analysis.claimAnalysis;
      if (mode === 'legacy-null') body.analysis.claimAnalysis = null;
      if (mode === 'lost-context') {
        body.analysis = null; body.conversationToken = null;
        body.clarification = { question: 'Escribe la consulta completa.', reason: 'CONTEXT_UNAVAILABLE' };
      }
      if (mode === 'both-results') body.clarification = { question: '¿Qué periodo?', reason: 'MISSING_PERIOD' };
      await page.route('**/api/analysis/jobs', route => route.fulfill({ contentType: 'application/json', body: JSON.stringify(completedJob(body)) }));
      await page.goto(process.env.APP_URL || 'http://127.0.0.1:5179');
      await page.getByRole('textbox', { name: 'Tu consulta', exact: true }).fill('Local contract check');
      await page.getByRole('button', { name: 'Analizar', exact: true }).click();
      if (mode === 'lost-context') {
        await page.getByRole('region', { name: 'Aclaración necesaria' }).waitFor();
        assert(await page.getByLabel('Pregunta de seguimiento').isEnabled());
        assert.equal(await page.getByRole('alert').count(), 0);
      } else if (mode.startsWith('legacy')) {
        await page.getByRole('heading', { name: 'Valoración final', exact: true }).waitFor();
        assert.equal(await page.getByRole('region', { name: 'Valoración por afirmaciones' }).count(), 0);
      } else {
        await page.getByRole('alert').waitFor();
        assert.equal(await page.getByRole('heading', { name: 'Valoración final', exact: true }).count(), 0);
      }
      await page.close();
    }
    console.log('PASS: claims through Java API, desktop/mobile, references and saved history');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
