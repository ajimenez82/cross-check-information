// Browser integration checks; no extra project dependency is required.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  try {
    const page = await browser.newPage({ viewport: { width: 1536, height: 1024 } });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    const appUrl = process.env.APP_URL || 'http://127.0.0.1:5173';
    const examples = path.resolve(__dirname, '../../cross-check-service/docs/examples');
    const fixture = name => JSON.parse(fs.readFileSync(path.join(examples, name), 'utf8'));
    const requests = [];
    let scenario = 'classified';
    let tokenCounter = 0;
    let release;
    await page.route('**/api/analysis/start', async route => {
      const body = route.request().postDataJSON();
      requests.push(body);
      assert.deepEqual(Object.keys(body).sort(), ['conversationToken', 'text']);
      if (scenario === 'offline') return route.abort('connectionfailed');
      if (scenario === 'delayed') await new Promise(resolve => { release = resolve; });
      if (scenario === 'timeout' || scenario === 'expired') {
        return route.fulfill({ status: scenario === 'timeout' ? 504 : 410, contentType: 'application/json',
          body: JSON.stringify(fixture(scenario === 'timeout' ? 'timeout-error.json' : 'expired-reference-error.json')) });
      }
      const result = fixture(scenario === 'zero' ? 'zero-units-response.json' :
        scenario === 'unavailable' ? 'insufficient-evidence-response.json' : 'classified-response.json');
      result.conversationToken = 'opaque-test-token-' + (++tokenCounter);
      if (scenario === 'malformed') result.analysis.verdict.status = 'UNKNOWN';
      if (scenario === 'unsafe') result.analysis.sources[0].url = 'javascript:alert(1)';
      if (scenario === 'duplicate') result.analysis.publicationPositions.units.push(result.analysis.publicationPositions.units[0]);
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(result) });
    });
    await page.goto(appUrl);
    await page.evaluate(() => localStorage.clear());
    await page.reload();
    const query = page.getByRole('textbox', { name: 'Tu consulta', exact: true });
    const analyze = page.getByRole('button', { name: 'Analizar', exact: true });
    assert(await analyze.isDisabled());
    await query.fill('   ');
    assert(await analyze.isDisabled());
    await page.getByRole('button', { name: '¿Qué evidencias respaldan esta declaración?' }).click();
    assert.equal(await query.inputValue(), '¿Qué evidencias respaldan esta declaración?');
    assert.equal(requests.length, 0);
    await query.fill('Consulta escrita por el usuario');
    scenario = 'delayed';
    await analyze.click();
    await page.getByRole('status').filter({ hasText: 'Esperando respuesta' }).waitFor();
    assert(await page.getByLabel('Pregunta de seguimiento').isDisabled());
    assert.equal(requests.length, 1);
    assert.deepEqual(requests[0], { text: 'Consulta escrita por el usuario', conversationToken: null });
    scenario = 'classified'; release();
    await page.getByRole('heading', { name: 'Ejemplo de publicaciones clasificadas' }).waitFor();
    assert((await page.getByLabel('Tu consulta', { exact: true }).textContent()).includes('Consulta escrita por el usuario'));
    assert((await page.getByText('Cuestiona: 1 de 2 (50 %)').textContent()).includes('50'));
    assert.equal(await page.getByText('80 %', { exact: true }).count(), 0);
    await page.getByText('Ver desglose de publicaciones', { exact: true }).click();
    await page.getByRole('button', { name: 'Ver referencia Publicación ilustrativa A', exact: true }).first().click();
    assert.equal(await page.getByRole('link', { name: 'Abrir fuente en una pestaña nueva' }).getAttribute('href'), 'https://example.org/publicacion-a');
    await page.getByRole('button', { name: 'Cerrar', exact: true }).click();
    await page.getByLabel('Pregunta de seguimiento').fill('Pregunta siguiente');
    await page.getByRole('button', { name: 'Enviar seguimiento' }).click();
    await page.getByRole('heading', { name: 'Ejemplo de publicaciones clasificadas' }).nth(1).waitFor();
    assert.equal(requests[1].conversationToken, 'opaque-test-token-1');
    const savedUrl = page.url();
    await page.reload();
    await page.getByRole('heading', { name: 'Ejemplo de publicaciones clasificadas' }).nth(1).waitFor();
    assert.equal(requests.length, 2, 'Reload must not resubmit');
    await page.getByLabel('Pregunta de seguimiento').fill('Seguimiento tras recarga');
    await page.getByRole('button', { name: 'Enviar seguimiento' }).click();
    await page.getByRole('heading', { name: 'Ejemplo de publicaciones clasificadas' }).nth(2).waitFor();
    assert.equal(requests[2].conversationToken, 'opaque-test-token-2');

    async function newQuery(nextScenario, text = 'Consulta ' + nextScenario) {
      scenario = nextScenario;
      await page.getByRole('link', { name: 'Nueva conversación', exact: true }).first().click();
      await query.fill(text);
      await analyze.click();
    }
    await newQuery('zero');
    await page.getByText('No calculable', { exact: true }).waitFor();
    assert.equal(requests.at(-1).conversationToken, null);
    await newQuery('unavailable');
    await page.getByText('Medición no disponible', { exact: true }).waitFor();
    await newQuery('timeout');
    await page.getByRole('alert').waitFor();
    assert((await page.getByRole('alert').textContent()).includes('podría haber continuado'));
    assert((await page.getByRole('alert').textContent()).includes('ID_ILUSTRATIVO'));
    const failedCount = requests.length;
    await page.waitForTimeout(200);
    assert.equal(requests.length, failedCount);
    assert.equal(await page.getByRole('heading', { name: 'Valoración final' }).count(), 0);
    scenario = 'unavailable';
    await page.getByLabel('Consulta para reintentar').fill('Consulta corregida');
    await page.getByRole('button', { name: 'Reintentar', exact: true }).click();
    await page.getByText('Medición no disponible', { exact: true }).waitFor();
    assert.equal(requests.at(-1).text, 'Consulta corregida');
    assert.equal(requests.length, failedCount + 1);
    await newQuery('expired');
    await page.getByRole('link', { name: 'Iniciar nueva conversación', exact: true }).waitFor();
    assert.equal(await page.getByRole('button', { name: 'Reintentar', exact: true }).count(), 0);
    for (const mode of ['malformed', 'unsafe', 'duplicate', 'offline']) {
      await newQuery(mode);
      await page.getByRole('alert').waitFor();
      assert.equal(await page.getByRole('heading', { name: 'Valoración final' }).count(), 0);
    }
    await page.goto(savedUrl);
    await page.getByRole('heading', { name: 'Ejemplo de publicaciones clasificadas' }).nth(2).waitFor();
    fs.mkdirSync('review/integration', { recursive: true });
    await page.screenshot({ path: 'review/integration/desktop-api.png', fullPage: true });
    for (const width of [320, 390, 760, 1536]) {
      await page.setViewportSize({ width, height: 900 });
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), 'Unexpected horizontal overflow at ' + width);
    }
    await page.setViewportSize({ width: 390, height: 844 });
    await page.screenshot({ path: 'review/integration/mobile-api.png', fullPage: true });
    await page.getByRole('button', { name: 'Abrir navegación' }).click();
    await page.getByRole('dialog', { name: 'Navegación' }).getByRole('link', { name: 'Nueva conversación', exact: true }).click();
    await query.waitFor();
    assert.equal(await page.getByRole('dialog', { name: 'Navegación' }).count(), 0);
    await page.setViewportSize({ width: 1536, height: 1024 });
    await page.evaluate(() => localStorage.setItem('crosscheck.conversations.v1', 'corrupt'));
    await page.reload();
    await page.getByRole('status').filter({ hasText: 'No se ha podido recuperar' }).waitFor();
    await newQuery('unavailable', 'Historial recuperado');
    await page.getByText('Medición no disponible', { exact: true }).waitFor();
    page.once('dialog', dialog => dialog.accept());
    await page.getByRole('button', { name: 'Borrar historial local', exact: true }).first().click();
    await page.getByRole('heading', { name: 'No hay una conversación disponible' }).waitFor();
    assert.equal(await page.evaluate(() => JSON.parse(localStorage.getItem('crosscheck.conversations.v1')).length), 0);
    assert.deepEqual(errors, []);
    console.log('PASS: request contract, pending state, results, follow-ups, refresh, history, zero/unavailable, errors, explicit retry, unsafe responses, desktop/mobile and overflow.');

    if (process.env.LIVE_API === '1') {
      await page.unroute('**/api/analysis/start');
      await page.goto(appUrl);
      await query.fill('Comprobación con Spring Boot');
      await analyze.click();
      await page.getByRole('heading', { name: 'Análisis simulado · datos ilustrativos', exact: true }).waitFor();
      await page.getByLabel('Pregunta de seguimiento').fill('Seguimiento real por HTTP');
      await page.getByRole('button', { name: 'Enviar seguimiento' }).click();
      await page.getByRole('heading', { name: 'Seguimiento simulado · datos ilustrativos', exact: true }).waitFor();
      await page.reload();
      await page.getByRole('heading', { name: 'Seguimiento simulado · datos ilustrativos', exact: true }).waitFor();
      await page.screenshot({ path: 'review/integration/live-api.png', fullPage: true });
      console.log('PASS: browser → Vite proxy → Spring Boot, encrypted token follow-up and reload.');
    }
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exit(1); });

