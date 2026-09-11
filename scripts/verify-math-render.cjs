// Run with NODE_PATH pointing to a Playwright installation. No provider/network access required.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');
const { PNG } = require('pngjs');

(async () => {
  const root = path.resolve(__dirname, '..');
  const source = fs.readFileSync(path.join(root, 'app/src/main/java/com/hkm/pozix/ui/components/richcontent/KaTeXMathView.kt'), 'utf8');
  const html = source.match(/private val MATH_HTML = """([\s\S]*?)"""\.trimIndent\(\)/)[1];
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  try {
    const page = await browser.newPage({ viewport: { width: 320, height: 1600 } });
    await page.route('http://pozix.test/**', async route => {
      const relative = new URL(route.request().url()).pathname.slice(1);
      if (!relative) return route.fulfill({ contentType: 'text/html', body: html });
      const asset = path.resolve(root, 'app/src/main/assets/katex', relative);
      assert.ok(asset.startsWith(path.join(root, 'app/src/main/assets/katex') + path.sep));
      return route.fulfill({ body: fs.readFileSync(asset), contentType: relative.endsWith('.css') ? 'text/css' : relative.endsWith('.js') ? 'application/javascript' : 'font/woff2' });
    });
    await page.addInitScript(() => {
      window.MathBridge = {
        measured: (token, height, overflow) => { window.measurement = { token, height, overflow }; },
        failed: token => { throw new Error('KaTeX render failed: ' + token); }
      };
    });
    await page.goto('http://pozix.test/');
    const cases = [
      ['photo-water', String.raw`K_w=[H^+][OH^-]=1.0\times10^{-14}`],
      ['photo-energy', String.raw`E^2=(mc^2)^2+(pc)^2`],
      ['photo-cauchy', String.raw`\left(\sum_{i=1}^n a_i b_i\right)^2\leq\left(\sum_{i=1}^n a_i^2\right)\left(\sum_{i=1}^n b_i^2\right)`],
      ['photo-fourier', String.raw`f(x)=\frac{a_0}{2}+\sum_{n=1}^\infty\left(a_n\cos\frac{n\pi x}{L}+b_n\sin\frac{n\pi x}{L}\right)`],
      ['photo-derivative', String.raw`f'(x_0)=\lim_{h\to0}\frac{f(x_0+h)-f(x_0)}{h}`],
      ['fraction', String.raw`\frac{1}{\sqrt{x^2+1}}`],
      ['integral', String.raw`\int_0^\infty \frac{x^2}{e^x-1}\,dx`],
      ['tall-matrix', String.raw`\begin{matrix}` + Array(32).fill('x & y').join(String.raw`\\`) + String.raw`\end{matrix}`],
      ['wide', Array(30).fill('x^2').join('+')],
      ['short-again', 'x+1']
    ];
    let token = 0;
    for (const [name, latex] of cases) {
      token++;
      await page.evaluate(({latex, token}) => window.renderMath(latex, '#eeeeee', 20, true, token), {latex, token});
      await page.waitForFunction(token => window.measurement?.token === token, token);
      const bounds = await page.evaluate(() => ({...window.measurement,
        actual: document.getElementById('math').getBoundingClientRect().height,
        left: document.getElementById('math').getBoundingClientRect().left,
        errors: document.querySelectorAll('.katex-error').length }));
      assert.equal(bounds.errors, 0, name);
      assert.equal(bounds.height, Math.ceil(bounds.actual) + 8, name + ': measured height');
      assert.ok(bounds.left >= 0, name + ': left edge clipped');
      if (name === 'tall-matrix') assert.ok(bounds.height > 500, 'tall formulas must not be capped');
      if (name === 'wide') assert.ok(bounds.overflow, 'wide formulas must scroll');
      if (name === 'short-again') assert.ok(bounds.height < 50, 'old height must not persist');
      // Compare against actual painted pixels in an unconstrained viewport, not our own height formula.
      const pixels = PNG.sync.read(await page.screenshot({ omitBackground: true }));
      let bottomInk = -1;
      for (let y = 0; y < pixels.height; y++) {
        for (let x = 0; x < pixels.width; x++) {
          if (pixels.data[(y * pixels.width + x) * 4 + 3] > 32) { bottomInk = y; break; }
        }
      }
      assert.ok(bottomInk >= 0, name + ': no visible formula');
      assert.ok(bottomInk < bounds.height, name + ': painted formula would be clipped');
      assert.ok(bounds.height - bottomInk <= 20, name + ': excess blank space below visible formula');
      console.log(name, JSON.stringify(bounds));
    }
    await page.evaluate(() => window.renderMath(String.raw`\frac{a+b}{\sqrt{x^2+1}}`, '#222222', 30, true, 99));
    await page.waitForFunction(() => window.measurement?.token === 99);
    console.log('PASS: 11 browser render cases, including painted-pixel bounds (not Android device verification)');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
