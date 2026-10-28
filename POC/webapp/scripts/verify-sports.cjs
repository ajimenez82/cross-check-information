// Offline category routing, persistence and legacy-history regression.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { completedJob } = require('./job-fixture.cjs');
const app = process.env.APP_URL || 'http://127.0.0.1:5179';
const storageKey = 'crosscheck.conversations.v1';
const fixture = JSON.parse(fs.readFileSync(path.resolve(__dirname,
  '../../cross-check-service/docs/examples/classified-response.json'), 'utf8'));

(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  try {
    for (const width of [1536, 390]) {
      const page = await browser.newPage({ viewport: { width, height: 1000 } });
      const requests = [], errors = [];
      page.on('pageerror', error => errors.push(error.message));
      await page.route('**/api/analysis/jobs', async route => {
        requests.push(route.request().postDataJSON());
        const result = structuredClone(fixture);
        result.conversationToken = 'sports-token-' + requests.length;
        if (requests.length === 1) {
          result.analysis = null;
          result.clarification = { question: '¿Qué partido y jugada quieres analizar?', reason: 'AMBIGUOUS_REFERENCE' };
        }
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(completedJob(result)) });
      });
      await page.goto(app);
      await page.getByRole('combobox').click();
      await page.getByRole('option', { name: 'Deportes', exact: true }).click();
      const query = page.getByRole('textbox', { name: 'Tu consulta', exact: true });
      assert(await query.isEnabled());
      await page.getByRole('button', { name: '¿Debió ser expulsado este jugador por esta entrada?', exact: true }).click();
      assert((await query.inputValue()).includes('expulsado'));
      assert.equal(requests.length, 0);
      await page.getByRole('button', { name: 'Analizar', exact: true }).click();
      await page.getByText('¿Qué partido y jugada quieres analizar?', { exact: true }).waitFor();
      assert.equal(requests[0].category, 'SPORTS_ANALYSIS');
      assert.equal(requests[0].conversationToken, null);
      await page.getByRole('heading', { name: 'Conversación de Deportes', exact: true }).waitFor();
      await page.reload();
      await page.getByRole('heading', { name: 'Conversación de Deportes', exact: true }).waitFor();
      await page.getByLabel('Pregunta de seguimiento').fill('El partido y la jugada del ejemplo');
      await page.getByRole('button', { name: 'Enviar seguimiento' }).click();
      await page.getByRole('heading', { name: fixture.analysis.title, exact: true }).waitFor();
      assert.equal(requests[1].category, 'SPORTS_ANALYSIS');
      assert.equal(requests[1].conversationToken, 'sports-token-1');
      await page.reload();
      await page.getByLabel('Pregunta de seguimiento').fill('¿Y la segunda jugada?');
      await page.getByRole('button', { name: 'Enviar seguimiento' }).click();
      await page.getByRole('heading', { name: fixture.analysis.title, exact: true }).nth(1).waitFor();
      assert.equal(requests[2].category, 'SPORTS_ANALYSIS');
      assert.equal(requests[2].conversationToken, 'sports-token-2');

      // Old saved conversations have no category and must remain readable as politics.
      await page.evaluate(key => {
        const items = JSON.parse(localStorage.getItem(key));
        delete items[0].category;
        localStorage.setItem(key, JSON.stringify(items));
      }, storageKey);
      await page.reload();
      await page.getByRole('heading', { name: 'Conversación de Análisis político', exact: true }).waitFor();
      await page.goto(app);
      await page.getByRole('combobox').click();
      await page.getByRole('option', { name: 'Salud y bienestar', exact: true }).click();
      assert(await query.isDisabled());
      await page.getByRole('combobox').click();
      await page.getByRole('option', { name: 'Análisis político', exact: true }).click();
      await query.fill('Una consulta política');
      await page.getByRole('button', { name: 'Analizar', exact: true }).click();
      await page.getByRole('heading', { name: fixture.analysis.title, exact: true }).waitFor();
      assert.equal(requests.at(-1).category, 'POLITICAL_ANALYSIS');
      assert.equal(requests.at(-1).conversationToken, null);
      assert.deepEqual(errors, []);
      await page.close();
      console.log(`PASS: sports, clarification, follow-ups, reload, legacy history and politics (${width}px)`);
    }
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
