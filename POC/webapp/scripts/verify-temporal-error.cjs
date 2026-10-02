const { completedJob } = require('./job-fixture.cjs');
// Run through OpenAiApiTest with -Dbrowser.temporal=true and a local Vite server.
// Requests reach the real test Java API; only its upstream provider is simulated.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const path = require('node:path');
const fs = require('node:fs');

(async () => {
  const api = new URL(process.env.TEST_API_URL);
  assert.equal(api.hostname, '127.0.0.1');
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const output = path.resolve(__dirname, '../review/integration');
  fs.mkdirSync(output, { recursive: true });
  const events = [];
  let calls = 0;
  try {
    const page = await browser.newPage({ viewport: { width: 1536, height: 1000 } });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.route('**/api/analysis/jobs', async route => {
      calls++;
      assert.equal(route.request().postDataJSON().conversationToken, null);
      const response = await route.fetch({ url: new URL('/api/analysis/start', api).href, maxRetries: 0 });
      const body = await response.json();
      events.push({ status: response.status(), code: body.code, requestId: response.headers()['x-request-id'] });
      if (calls === 1) {
        assert.equal(response.status(), 502);
        assert.equal(body.code, 'INVALID_ANALYSIS_OUTPUT');
        assert.equal(body.requestId, response.headers()['x-request-id']);
        assert(!body.analysis && !body.conversationToken);
      } else assert.equal(response.status(), 200);
      const view = completedJob(response.ok() ? body : null);
      if (!response.ok()) Object.assign(view, { status: 'FAILED', error: { ...body, executionState: body.executionState ?? 'UNKNOWN' } });
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(view) });
    });
    await page.goto(process.env.APP_URL || 'http://127.0.0.1:5179');
    await page.getByRole('textbox', { name: 'Tu consulta', exact: true }).fill('temporal-invalid');
    await page.getByRole('button', { name: 'Analizar', exact: true }).click();
    const alert = page.getByRole('alert');
    await alert.waitFor();
    const message = await alert.innerText();
    assert(message.includes('El servicio ha devuelto un resultado que no se puede mostrar.'));
    assert(message.includes(events[0].requestId));
    assert.equal(await page.getByRole('heading', { name: 'Valoración final', exact: true }).count(), 0);
    assert.equal(await page.getByRole('heading', { name: 'Posiciones en las publicaciones', exact: true }).count(), 0);
    assert(!/\d\s*%/.test(await page.locator('body').innerText()));
    await page.waitForTimeout(1500);
    assert.equal(calls, 1, 'No automatic retry');
    await page.screenshot({ path: path.join(output, 'temporal-error.png'), fullPage: true });
    await page.getByRole('link', { name: 'Nueva conversación', exact: true }).first().click();
    await page.getByRole('textbox', { name: 'Tu consulta', exact: true }).fill('valid-new-query');
    await page.getByRole('button', { name: 'Analizar', exact: true }).click();
    await page.getByRole('heading', { name: 'Valoración final', exact: true }).waitFor();
    await page.getByText('Ver desglose de publicaciones', { exact: true }).click();
    await page.getByText('Tercero citado · Asociación citada', { exact: false }).waitFor();
    await page.getByText('Publicaciones excluidas (1)', { exact: true }).click();
    await page.getByText('Fuera del alcance · Antecedente', { exact: true }).waitFor();
    assert.equal(await page.getByRole('alert').count(), 0);
    assert.equal(calls, 2);
    assert.deepEqual(errors, []);
    await page.screenshot({ path: path.join(output, 'temporal-recovery.png'), fullPage: true });
    fs.writeFileSync(path.join(output, 'temporal-diagnostics.json'), JSON.stringify({ calls, events, recovered: true }, null, 2));
    console.log('PASS: temporal rejection through Java, no result or percentages, no retry, new v2 query succeeds with visible trace.');
  } finally {
    await browser.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
