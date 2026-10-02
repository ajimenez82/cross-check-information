const { completedJob } = require('./job-fixture.cjs');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  try {
    const page = await browser.newPage();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    const fixture = JSON.parse(fs.readFileSync(path.resolve(__dirname,
      '../../cross-check-service/docs/examples/classified-response.json'), 'utf8'));
    const statuses = {
      SUPPORTED: ['Respaldada', 'circle-check'], REFUTED: ['Refutada', 'circle-x'],
      MISLEADING: ['Engañosa o fuera de contexto', 'triangle-alert'],
      INSUFFICIENT_EVIDENCE: ['Evidencia insuficiente', 'circle-help'],
      OPINION: ['Interpretación u opinión', 'message-circle'],
      NO_SINGLE_VERDICT: ['Sin veredicto único', 'git-fork'],
      SUPPORTED_BY_PUBLICATIONS: ['Respaldada por las publicaciones', 'newspaper'],
      QUESTIONED_BY_PUBLICATIONS: ['Cuestionada por las publicaciones', 'newspaper'],
    };
    let status;
    await page.route('**/api/analysis/jobs', route => {
      const response = structuredClone(fixture);
      response.analysis.verdict.status = status;
      response.analysis.sources.forEach(source => { source.consultedAt = null; });
      response.analysis.publicationPositions.consultedAt = null;
      response.analysis.verdict.explanation = 'La evidencia disponible no permite respaldar ni refutar la afirmación.';
      if (status.endsWith('_BY_PUBLICATIONS')) {
        const position = status === 'SUPPORTED_BY_PUBLICATIONS' ? 'SUPPORTS' : 'QUESTIONS';
        response.analysis.publicationPositions.units.forEach(unit => { unit.position = position; });
        response.analysis.verdict.explanation += position === 'SUPPORTS'
          ? ' Predomina el respaldo en esta muestra ficticia.'
          : ' Predomina el cuestionamiento en esta muestra ficticia.';
      }
      return route.fulfill({ contentType: 'application/json', body: JSON.stringify(completedJob(response)) });
    });
    for (const width of [1536, 390]) {
      await page.setViewportSize({ width, height: 1000 });
      for (const [code, [label, icon]] of Object.entries(statuses)) {
        status = code;
        await page.goto(process.env.APP_URL || 'http://127.0.0.1:5178');
        await page.getByRole('textbox', { name: 'Tu consulta', exact: true }).fill('Consulta de prueba');
        await page.getByRole('button', { name: 'Analizar', exact: true }).click();
        const verdict = page.locator(`[data-verdict="${code}"]`);
        await verdict.getByText(label, { exact: true }).waitFor();
        assert.equal(await verdict.locator(`svg.lucide-${icon}`).count(), 1);
        const positions = page.locator('aside').filter({ has: page.getByRole('heading', { name: 'Posiciones en las publicaciones' }) });
        const article = page.locator('article').filter({ has: verdict });
        const sources = page.locator('section').filter({ has: page.getByRole('heading', { name: 'Fuentes', exact: true }) }).last();
        const [a, p, s] = await Promise.all([article.boundingBox(), positions.boundingBox(), sources.boundingBox()]);
        if (width === 390) {
          assert(p.y >= a.y + a.height - 1, 'Mobile positions must follow analysis');
          assert(s.y >= p.y + p.height - 1, 'Mobile sources must follow positions');
        } else assert(p.x >= a.x + a.width - 1, 'Desktop positions must remain on the right');
        assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'No horizontal overflow');
      }
      await page.screenshot({ path: `review/integration/verdict-${width}.png`, fullPage: true });
      await page.reload();
      await page.getByText('Cuestionada por las publicaciones', { exact: true }).waitFor();
      await page.getByRole('button', { name: 'Ver referencia Publicación ilustrativa A', exact: true }).first().click();
      await page.getByText('Consulta: No disponible', { exact: true }).waitFor();
      await page.getByRole('button', { name: 'Cerrar', exact: true }).click();
    }
    assert.deepEqual(errors, []);
    console.log('PASS: eight verdicts, icons, history reload, desktop/mobile order and overflow.');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exit(1); });
