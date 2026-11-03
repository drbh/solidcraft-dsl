// Renders gallery thumbnails with the page's own viewer, in headless Chrome.
//
//   node scripts/thumbs.mjs <page.html> <out-dir> <example name>...
//
// Writes <out-dir>/<name>.webp for each name (transparent background). Chrome is found at
// $CHROME, or at the usual macOS and Linux paths. Called by ci/examples-site.py.
import { existsSync, mkdirSync, writeFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { chromium } from 'playwright-core';

const [page, outDir, ...names] = process.argv.slice(2);
const chrome = [
  process.env.CHROME,
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  '/usr/bin/google-chrome',
  '/usr/bin/chromium',
  '/usr/bin/chromium-browser',
].find((p) => p && existsSync(p));
if (!chrome) {
  console.error('thumbs: no Chrome found (set CHROME); skipping thumbnails');
  process.exit(2);
}

mkdirSync(outDir, { recursive: true });
// macOS renders on the GPU through Metal; a Linux CI runner has no GPU, so WebGL runs on
// SwiftShader there.
const gpu = process.platform === 'darwin'
  ? ['--use-angle=metal', '--enable-gpu', '--ignore-gpu-blocklist']
  : ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'];
const browser = await chromium.launch({ executablePath: chrome, args: gpu });
const tab = await browser.newPage({ viewport: { width: 800, height: 600 } });
tab.on('pageerror', (e) => console.error('thumbs: page error:', e.message));
await tab.goto(pathToFileURL(resolve(page)).href, { waitUntil: 'load', timeout: 180000 });
await tab.waitForFunction(() => typeof window.__solidcraftThumb === 'function');

let failed = 0;
for (const name of names) {
  try {
    const url = await tab.evaluate((n) => window.__solidcraftThumb(n), name);
    writeFileSync(join(outDir, `${name}.webp`), Buffer.from(url.split(',')[1], 'base64'));
    console.error(`  thumb  ${name}`);
  } catch (error) {
    failed++;
    console.error(`  thumb FAILED ${name}: ${error.message.split('\n')[0]}`);
  }
}
await browser.close();
process.exit(failed ? 1 : 0);
