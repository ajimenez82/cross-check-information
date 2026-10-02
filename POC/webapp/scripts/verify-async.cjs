// Offline browser checks. LIVE_API=1 additionally exercises a running dev backend.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const { randomUUID, randomBytes } = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const fixture = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../../cross-check-service/docs/examples/classified-response.json'), 'utf8'));
const app = process.env.APP_URL || 'http://127.0.0.1:5179';
const storageKey = 'crosscheck.conversations.v1';
const screenshotDir = process.env.ASYNC_REVIEW_DIR;
const cases = [];
const envelope = (id, status = 'RUNNING', result = null, error = null) => ({
  analysisId: id, status, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(),
  completedAt: ['COMPLETED', 'FAILED'].includes(status) ? new Date().toISOString() : null,
  expiresAt: new Date(Date.now() + 86400000).toISOString(),
  pollAfterSeconds: ['COMPLETED', 'FAILED'].includes(status) ? null : 1, result, error,
});
const fulfill = (route, body, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
const history = page => page.evaluate(key => JSON.parse(localStorage.getItem(key)), storageKey);
const final = page => page.getByRole('heading', { name: 'Valoración final', exact: true });
async function submit(page, text = 'Consulta de prueba asíncrona') {
  await page.goto(app);
  await page.getByLabel('Tu consulta', { exact: true }).fill(text);
  await page.getByRole('button', { name: 'Analizar', exact: true }).click();
}
async function until(check, message) {
  for (let i = 0; i < 100; i++) {
    if (await check()) return;
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  throw new Error(message);
}
(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const errors = [];
  async function run(name, test, width = 1536) {
    if (process.env.ASYNC_CASE_FILTER && !name.includes(process.env.ASYNC_CASE_FILTER)) return;
    const context = await browser.newContext({ viewport: { width, height: 1000 } });
    context.on('page', page => page.on('pageerror', error => errors.push(`${name}: ${error.message}`)));
    try { await test(await context.newPage(), context); cases.push(name); console.log(`PASS: ${name}`); }
    finally { await context.close(); }
  }
  try {
    for (const width of [1536, 390]) await run(`reload, progress, follow-up and clarification (${width})`, async page => {
      let ready = false, posts = 0, gets = 0;
      const id = randomUUID();
      await page.route('**/api/analysis/jobs**', async route => {
        const request = route.request();
        if (request.method() === 'POST') {
          posts++;
          const saved = await history(page);
          const turn = saved[0].turns.at(-1);
          assert.equal(turn.job.key, request.headers()['idempotency-key'], 'Persist before submit');
          assert.equal(`Bearer ${turn.job.accessToken}`, request.headers().authorization);
          assert.deepEqual(turn.job.body, request.postDataJSON());
          if (posts === 2) {
            assert.equal(request.postDataJSON().conversationToken, fixture.conversationToken);
            return fulfill(route, envelope(randomUUID(), 'COMPLETED', { analysis: null, conversationToken: 'clarification-token', clarification: { question: '¿Qué periodo quieres analizar?', reason: 'MISSING_PERIOD' } }));
          }
          if (posts === 3) {
            assert.equal(request.postDataJSON().conversationToken, 'clarification-token');
            return fulfill(route, envelope(randomUUID(), 'COMPLETED', fixture));
          }
          return fulfill(route, envelope(id, 'QUEUED'), 202);
        }
        gets++;
        assert(request.url().endsWith(id));
        return fulfill(route, ready ? envelope(id, 'COMPLETED', fixture) : envelope(id));
      });
      await submit(page);
      await page.getByText('Consulta en cola…', { exact: true }).waitFor();
      await page.reload();
      await page.getByText('Analizando la consulta…', { exact: true }).waitFor();
      assert.equal(posts, 1);
      assert(gets > 0);
      assert(await page.getByLabel('Pregunta de seguimiento').isDisabled());
      if (screenshotDir) await page.screenshot({ path: path.join(screenshotDir, `async-pending-${width}.png`), fullPage: true });
      ready = true;
      await final(page).waitFor();
      assert.equal((await history(page))[0].turns[0].job, undefined, 'Remove completed job credentials');
      await page.reload();
      await final(page).waitFor();
      assert.equal(posts, 1);
      await page.getByLabel('Pregunta de seguimiento').fill('¿Y después?');
      await page.getByRole('button', { name: 'Enviar seguimiento', exact: true }).click();
      await page.getByRole('region', { name: 'Aclaración necesaria' }).waitFor();
      await page.reload();
      await page.getByRole('region', { name: 'Aclaración necesaria' }).waitFor();
      assert.equal(posts, 2);
      await page.getByLabel('Pregunta de seguimiento').fill('2023–2025');
      await page.getByRole('button', { name: 'Enviar seguimiento', exact: true }).click();
      await final(page).nth(1).waitFor();
      assert.equal(posts, 3);
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
      if (screenshotDir) await page.screenshot({ path: path.join(screenshotDir, `async-result-${width}.png`), fullPage: true });
    }, width);

    await run('lost 202 replays the exact saved request after reload', async page => {
      const id = randomUUID();
      const requests = [];
      await page.route('**/api/analysis/jobs**', async route => {
        const request = route.request();
        assert.equal(request.method(), 'POST');
        requests.push({ body: request.postData(), key: request.headers()['idempotency-key'], auth: request.headers().authorization });
        if (requests.length === 1) return route.abort('connectionreset');
        assert.deepEqual(requests[1], requests[0]);
        return fulfill(route, envelope(id, 'COMPLETED', fixture));
      });
      await submit(page);
      await page.getByText(/Sin conexión con el servicio/).waitFor();
      await page.reload();
      await final(page).waitFor();
      assert.equal(requests.length, 2);
    });

    await run('offline and temporary GET errors recover without another POST', async (page, context) => {
      const id = randomUUID(); let posts = 0, gets = 0;
      await page.route('**/api/analysis/jobs**', async route => {
        if (route.request().method() === 'POST') { posts++; return fulfill(route, envelope(id), 202); }
        gets++;
        if (gets === 1) return fulfill(route, { code: 'ANALYSIS_STORAGE_UNAVAILABLE' }, 503);
        return fulfill(route, envelope(id, 'COMPLETED', fixture));
      });
      await submit(page);
      await page.getByText(/Sin conexión con el servicio/).waitFor();
      await context.setOffline(true);
      await new Promise(resolve => setTimeout(resolve, 1200));
      const previousGets = gets;
      await new Promise(resolve => setTimeout(resolve, 1500));
      assert.equal(gets, previousGets);
      await context.setOffline(false);
      await final(page).waitFor(); assert.equal(posts, 1);
    });

    await run('storage failure prevents any request', async page => {
      let calls = 0;
      await page.addInitScript(() => { Storage.prototype.setItem = () => { throw new DOMException('Full', 'QuotaExceededError'); }; });
      await page.route('**/api/analysis/jobs**', route => { calls++; return route.abort(); });
      await submit(page);
      await page.getByText(/No se ha podido guardar el historial/).waitFor();
      await new Promise(resolve => setTimeout(resolve, 1200));
      assert.equal(calls, 0);
    });

    for (const code of ['ANALYSIS_JOB_EXPIRED', 'ANALYSIS_JOB_NOT_FOUND', 'INVALID_JOB_CREDENTIAL', 'IDEMPOTENCY_CONFLICT']) await run(`access error ${code} stops polling`, async page => {
      let calls = 0;
      await page.route('**/api/analysis/jobs**', route => { calls++; return fulfill(route, { code }, code.includes('EXPIRED') ? 410 : code.includes('FOUND') ? 404 : code.includes('CREDENTIAL') ? 401 : 409); });
      await submit(page); await page.getByRole('alert').waitFor();
      await page.reload(); await page.getByRole('alert').waitFor();
      await new Promise(resolve => setTimeout(resolve, 1200)); assert.equal(calls, 1);
    });

    await run('terminal failure never automatically submits again', async page => {
      let calls = 0;
      await page.route('**/api/analysis/jobs**', route => { calls++; return fulfill(route, envelope(randomUUID(), 'FAILED', null, { code: 'EXECUTION_DEADLINE', message: 'El análisis no ha finalizado dentro del plazo.', requestId: randomUUID(), executionState: 'UNKNOWN' })); });
      await submit(page); await page.getByRole('alert').waitFor();
      await page.reload(); await page.getByRole('alert').waitFor();
      await new Promise(resolve => setTimeout(resolve, 1200)); assert.equal(calls, 1);
      assert.equal(await page.getByRole('button', { name: 'Reintentar', exact: true }).count(), 0);
    });

    await run('invalid result can recover the same request', async page => {
      const id = randomUUID(); let key; let calls = 0;
      await page.route('**/api/analysis/jobs**', route => {
        calls++;
        const requestKey = route.request().headers()['idempotency-key'];
        if (!key) key = requestKey; else assert.equal(requestKey, key);
        return fulfill(route, envelope(id, 'COMPLETED', calls === 1 ? { analysis: {}, conversationToken: 'invalid' } : fixture));
      });
      await submit(page); await page.getByRole('alert').waitFor();
      await page.getByRole('button', { name: 'Recuperar este análisis', exact: true }).click();
      await final(page).waitFor(); assert.equal(calls, 2);
    });

    await run('old unacknowledged job expires without replay', async page => {
      const id = randomUUID(); let calls = 0;
      const text = 'Consulta antigua';
      await page.addInitScript(({ storageKey, id, text, key, accessToken }) => localStorage.setItem(storageKey, JSON.stringify([{ id, title: text, token: null, turns: [{ id, text, createdAt: '2026-01-01T00:00:00Z', status: 'pending', job: { key, accessToken, body: { text, conversationToken: null }, expiresAt: '2026-01-02T00:00:00Z' } }] }])), { storageKey, id, text, key: randomUUID(), accessToken: randomBytes(32).toString('base64url') });
      await page.route('**/api/analysis/jobs**', route => { calls++; return route.abort(); });
      await page.goto(`${app}/resultados/${id}`); await page.getByRole('alert').waitFor(); assert.equal(calls, 0);
    });

    await run('two tabs preserve one pending request and clearing removes recovery', async (page, context) => {
      const id = randomUUID(); let posts = 0;
      await context.route('**/api/analysis/jobs**', route => {
        if (route.request().method() === 'POST') posts++;
        return fulfill(route, envelope(id), 202);
      });
      await submit(page); await page.getByText('Analizando la consulta…', { exact: true }).waitFor();
      const other = await context.newPage(); await other.goto(page.url());
      await other.getByText('Analizando la consulta…', { exact: true }).waitFor();
      assert.equal(posts, 1);
      other.once('dialog', dialog => { assert(dialog.message().includes('no cancela')); void dialog.accept(); });
      await other.getByRole('button', { name: 'Borrar historial local', exact: true }).click();
      await page.getByRole('heading', { name: 'No hay una conversación disponible' }).waitFor();
      assert.equal(await history(page), null);
    });

    await run('long-running job remains recoverable and hidden tabs stop polling', async page => {
      const id = randomUUID(); let gets = 0, ready = false;
      await page.route('**/api/analysis/jobs**', route => {
        if (route.request().method() === 'GET') gets++;
        return fulfill(route, ready ? envelope(id, 'COMPLETED', fixture) : envelope(id));
      });
      await submit(page); await page.getByText('Analizando la consulta…', { exact: true }).waitFor();
      await page.evaluate(key => {
        const saved = JSON.parse(localStorage.getItem(key));
        saved[0].turns[0].createdAt = new Date(Date.now() - 95000).toISOString();
        localStorage.setItem(key, JSON.stringify(saved));
      }, storageKey);
      await page.reload(); await page.getByText('El análisis está tardando más de lo habitual.', { exact: true }).waitFor();
      await page.evaluate(() => { Object.defineProperty(document, 'hidden', { configurable: true, get: () => true }); document.dispatchEvent(new Event('visibilitychange')); });
      await new Promise(resolve => setTimeout(resolve, 1200));
      const previous = gets;
      await new Promise(resolve => setTimeout(resolve, 1600)); assert.equal(gets, previous);
      ready = true;
      await page.evaluate(() => { Object.defineProperty(document, 'hidden', { configurable: true, get: () => false }); document.dispatchEvent(new Event('visibilitychange')); });
      await final(page).waitFor();
    });

    await run('simultaneous follow-ups in two tabs create only one new job', async (page, context) => {
      let posts = 0;
      const pendingId = randomUUID();
      await context.route('**/api/analysis/jobs**', route => {
        if (route.request().method() === 'POST') posts++;
        return fulfill(route, posts === 1 ? envelope(randomUUID(), 'COMPLETED', fixture) : envelope(pendingId));
      });
      await submit(page); await final(page).waitFor();
      const other = await context.newPage(); await other.goto(page.url()); await final(other).waitFor();
      await page.getByLabel('Pregunta de seguimiento').fill('Primera pestaña');
      await other.getByLabel('Pregunta de seguimiento').fill('Segunda pestaña');
      await Promise.all([page, other].map(tab => tab.evaluate(() => document.getElementById('follow-up').closest('form').requestSubmit())));
      await until(async () => posts === 2, 'One follow-up must be submitted');
      await new Promise(resolve => setTimeout(resolve, 1200));
      assert.equal(posts, 2);
      assert.equal((await history(page))[0].turns.length, 2);
    });

    await run('15-second request timeout reuses the same idempotency key', async page => {
      const id = randomUUID(); let calls = 0, key;
      await page.route('**/api/analysis/jobs**', async route => {
        calls++;
        const requestKey = route.request().headers()['idempotency-key'];
        if (!key) key = requestKey; else assert.equal(requestKey, key);
        if (calls === 1) { await new Promise(resolve => setTimeout(resolve, 16000)); return route.abort().catch(() => {}); }
        return fulfill(route, envelope(id, 'COMPLETED', fixture));
      });
      await submit(page);
      await page.getByText(/Sin conexión con el servicio/).waitFor({ timeout: 20000 });
      await final(page).waitFor(); assert.equal(calls, 2);
    });

    if (process.env.LIVE_API === '1') await run('browser → Vite → Spring Boot dev worker and encrypted follow-up', async page => {
      let posts = 0, gets = 0;
      page.on('request', request => { if (request.url().includes('/api/analysis/jobs')) { if (request.method() === 'POST') posts++; else gets++; } });
      await submit(page, 'Prueba offline del servicio simulado'); await final(page).waitFor({ timeout: 30000 });
      const saved = await history(page); assert.equal(saved[0].token.split('.').length, 5);
      await page.getByLabel('Pregunta de seguimiento').fill('¿Puedes ampliar el análisis simulado?');
      await page.getByRole('button', { name: 'Enviar seguimiento', exact: true }).click();
      await final(page).nth(1).waitFor({ timeout: 30000 });
      await page.reload(); await final(page).nth(1).waitFor();
      assert.equal(posts, 2); assert(gets >= 2);
    });
    assert.deepEqual(errors, []);
    console.log(`PASS: ${cases.length} asynchronous browser scenarios; no paid inference.`);
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
