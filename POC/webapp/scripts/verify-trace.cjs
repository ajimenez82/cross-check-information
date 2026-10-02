const { completedJob } = require('./job-fixture.cjs');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const read = file => JSON.parse(fs.readFileSync(path.resolve(__dirname, file), 'utf8'));

(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  try {
    const page = await browser.newPage();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    const draft = read('../../../Docs/proposals/political-report-v2.example.json');
    delete draft.schemaVersion;
    draft.analyzedAt = '2026-09-27T12:00:00Z';
    const assessments = draft.publicationPositions.assessments;
    delete draft.publicationPositions.assessments;
    Object.assign(draft.publicationPositions, {
      period: null, consultedAt: null,
      units: assessments.filter(item => item.decision === 'COUNT').map(({ id, sourceIds, position, explanation, trace }) =>
        ({ id, sourceIds, position, explanation, trace })),
      excluded: assessments.filter(item => item.decision === 'EXCLUDE').flatMap(item =>
        item.sourceIds.map(sourceId => ({ sourceId, reason: item.explanation, trace: item.trace }))),
    });
    let mode = 'trace';
    let calls = 0;
    await page.route('**/api/analysis/jobs', route => {
      calls++;
      const analysis = structuredClone(draft);
      if (mode === 'legacy') {
        analysis.publicationPositions.units.forEach(unit => { delete unit.trace; });
        analysis.publicationPositions.excluded.forEach(item => { item.trace = null; });
      }
      const trace = analysis.publicationPositions.units[0].trace;
      if (mode === 'owner') trace.stanceOwner = 'UNKNOWN';
      if (mode === 'reference') trace.arguments[0].sourceId = 'S5';
      if (mode === 'missing') delete trace.scope;
      if (mode === 'empty') trace.arguments = [];
      return route.fulfill({ contentType: 'application/json', body: JSON.stringify(completedJob({ conversationToken: 'fixture-token', analysis })) });
    });
    const submit = async () => {
      await page.goto(process.env.APP_URL || 'http://127.0.0.1:5178');
      await page.getByRole('textbox', { name: 'Tu consulta', exact: true }).fill('Prueba local de trazabilidad');
      await page.getByRole('button', { name: 'Analizar', exact: true }).click();
    };
    for (const width of [1536, 390]) {
      await page.setViewportSize({ width, height: 1000 });
      mode = 'trace';
      await submit();
      await page.getByRole('heading', { name: 'Valoración final', exact: true }).waitFor();
      await page.getByText('Ver desglose de publicaciones', { exact: true }).click();
      await page.getByText('Publicaciones excluidas (1)', { exact: true }).click();
      await page.getByText('Tercero citado · Asociación citada', { exact: false }).waitFor();
      await page.getByText('Fuera del alcance · Antecedente', { exact: true }).waitFor();
      assert.equal(await page.getByText('Trazabilidad no disponible', { exact: true }).count(), 0);
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'No overflow');
      const aside = page.locator('aside').filter({ has: page.getByRole('heading', { name: 'Posiciones en las publicaciones' }) });
      const article = page.locator('article');
      const [a, p] = await Promise.all([article.boundingBox(), aside.boundingBox()]);
      assert(width === 390 ? p.y >= a.y + a.height - 1 : p.x >= a.x + a.width - 1);
      await aside.getByRole('button', { name: 'Ver referencia Publicación ficticia 1', exact: true }).last().click();
      await page.getByRole('dialog').waitFor();
      await page.getByRole('button', { name: 'Cerrar', exact: true }).click();
      await page.screenshot({ path: `review/integration/trace-${width}.png`, fullPage: true });
      const before = calls;
      await page.reload();
      await page.getByText('Ver desglose de publicaciones', { exact: true }).click();
      await page.getByText('Tercero citado · Asociación citada', { exact: false }).waitFor();
      assert.equal(calls, before, 'History reload must not submit');
    }
    mode = 'legacy';
    await submit();
    await page.getByRole('heading', { name: 'Valoración final', exact: true }).waitFor();
    await page.reload();
    await page.getByText('Ver desglose de publicaciones', { exact: true }).click();
    await page.getByText('Publicaciones excluidas (1)', { exact: true }).click();
    assert.equal(await page.getByText('Trazabilidad no disponible', { exact: true }).count(), 5);
    for (mode of ['owner', 'reference', 'missing', 'empty']) {
      await submit();
      await page.getByRole('alert').waitFor();
      assert.equal(await page.getByRole('heading', { name: 'Valoración final', exact: true }).count(), 0);
    }
    assert.deepEqual(errors, []);
    console.log('PASS: traced units/exclusions, source dialog, desktop/mobile layout, history, legacy and four invalid traces.');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
