// Historical synchronous-transport checks. For the current frontend run verify-async.cjs.
// Local HTTP provider only. Run Vite with API_PROXY_TARGET=http://127.0.0.1:8098
// and VITE_ANALYSIS_TIMEOUT_MS=1500, then run this script with APP_URL set.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const fixture = JSON.parse(fs.readFileSync(path.resolve(__dirname,
  '../../cross-check-service/docs/examples/insufficient-evidence-response.json'), 'utf8'));
const events = [];
let scenario = 'slow';
let calls = 0;
const server = http.createServer((req, res) => {
  req.resume();
  const number = ++calls;
  const current = scenario;
  const started = Date.now();
  const event = { layer: 'mock', number, scenario: current };
  events.push({ ...event, phase: 'received' });
  res.on('close', () => events.push({ ...event, phase: 'closed', finished: res.writableFinished, elapsedMs: Date.now() - started }));
  res.setHeader('Content-Type', 'application/json');
  res.setHeader('X-Request-Id', 'timeout-test-' + number);
  if (current === 'body-delay') { res.writeHead(200); res.write('{'); }
  setTimeout(() => {
    if (res.destroyed) return;
    if (current === 'server-timeout') {
      res.writeHead(504);
      res.end(JSON.stringify({ code: 'ANALYSIS_TIMEOUT', requestId: 'timeout-test-' + number, executionState: 'UNKNOWN' }));
    } else {
      const body = JSON.stringify({ ...fixture, conversationToken: 'test-token-' + number });
      res.end(current === 'body-delay' ? body.slice(1) : body);
    }
  }, ['body-delay', 'client-timeout'].includes(current) ? 2400 : 700);
});
(async () => {
  await new Promise(resolve => server.listen(8098, '127.0.0.1', resolve));
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  let page;
  try {
    page = await browser.newPage();
    page.on('requestfailed', req => events.push({ layer: 'browser', phase: 'requestfailed', failure: req.failure()?.errorText }));
    page.on('response', res => { if (res.url().endsWith('/api/analysis/start')) events.push({ layer: 'browser', phase: 'headers', status: res.status(), requestId: res.headers()['x-request-id'] }); });
    page.on('pageerror', error => events.push({ layer: 'browser', phase: 'pageerror', type: error.name }));
    await page.goto(process.env.APP_URL || 'http://127.0.0.1:5179');
    await page.getByRole('textbox', { name: 'Tu consulta', exact: true }).fill('Local test');
    await page.getByRole('button', { name: 'Analizar', exact: true }).click();
    await page.getByText('Medición no disponible', { exact: true }).waitFor();
    await page.getByLabel('Pregunta de seguimiento').fill('Local follow-up');
    await page.getByRole('button', { name: 'Enviar seguimiento' }).click();
    await page.getByText('Medición no disponible', { exact: true }).nth(1).waitFor();
    assert.equal(calls, 2);
    for (const next of ['server-timeout', 'client-timeout', 'body-delay']) {
      scenario = next;
      await page.getByRole('link', { name: 'Nueva conversación', exact: true }).first().click();
      await page.getByRole('textbox', { name: 'Tu consulta', exact: true }).fill('Local timeout test');
      const before = calls;
      await page.getByRole('button', { name: 'Analizar', exact: true }).click();
      const alert = page.getByRole('alert');
      await alert.waitFor();
      const message = await alert.textContent();
      assert(message.includes(next === 'server-timeout' ? 'El servicio ha agotado' : 'tiempo de espera del navegador'), message);
      if (next !== 'client-timeout') assert(message.includes('timeout-test-'), 'Preserve request identifier after headers');
      await page.waitForTimeout(1100);
      assert.equal(calls, before + 1, 'No automatic retry');
      assert.equal(await page.getByRole('heading', { name: 'Valoración final' }).count(), 0);
    }
    assert(!events.some(event => event.phase === 'pageerror'));
    console.log('PASS: delayed initial/follow-up, server timeout, browser timeout before/after headers, request ID and no retries.');
  } catch (error) {
    events.push({ layer: 'test', phase: 'failed', type: error.name });
    if (page) await page.screenshot({ path: 'review/integration/timeout-failure.png', fullPage: true }).catch(() => {});
    throw error;
  } finally {
    fs.writeFileSync('review/integration/timeout-diagnostics.json', JSON.stringify(events, null, 2));
    await browser.close();
    server.closeAllConnections();
    server.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
