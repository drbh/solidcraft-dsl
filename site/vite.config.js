// Builds the examples page into one self-contained index.html.
//
// ci/examples-site.py writes .data/manifest.json (sources, thumbnails and compressed GLBs). The
// sources are highlighted here at build time with the repository's TextMate grammar, ordered into
// the sections of categories.js and handed to the app as `virtual:examples`. The GLBs never go
// through the bundler: they are injected into the HTML as inert <script type="text/plain"> blocks
// that the viewer decodes when an example is opened.
import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import { viteSingleFile } from 'vite-plugin-singlefile';
import { createHighlighter } from 'shiki';
import categories from './categories.js';

const here = (p) => fileURLToPath(new URL(p, import.meta.url));
const manifestPath = process.env.EXAMPLES_MANIFEST || here('./.data/manifest.json');
const grammarPath = here('../editors/vscode/syntaxes/solidcraft.tmLanguage.json');

function examples() {
  const id = 'virtual:examples';
  let manifest;
  const load = () => (manifest ??= JSON.parse(readFileSync(manifestPath, 'utf8')));
  return {
    name: 'solidcraft-examples',
    resolveId: (source) => (source === id ? '\0' + id : null),
    async load(resolved) {
      if (resolved !== '\0' + id) return null;
      this.addWatchFile(manifestPath);
      const grammar = JSON.parse(readFileSync(grammarPath, 'utf8'));
      const highlighter = await createHighlighter({
        themes: ['github-light', 'github-dark'],
        langs: [{ ...grammar, name: 'solidcraft' }],
      });
      const { commit, examples } = load();
      const palette = new Map(); // "light dark" -> class index
      const byName = new Map(examples.map((e) => [e.name, e]));
      const sections = categories
        .map((c) => ({ ...c, examples: c.examples.filter((n) => byName.has(n)) }))
        .filter((c) => c.examples.length);
      const listed = new Set(sections.flatMap((c) => c.examples));
      const rest = examples.filter((e) => !listed.has(e.name)).map((e) => e.name);
      if (rest.length) sections.push({ title: 'Other', blurb: 'Not yet in a section.', examples: rest });
      const ordered = sections.flatMap((c) => c.examples.map((n) => [c.title, byName.get(n)]));
      // Inline (one-file page): base64 `glb` and `thumb`. Hosted (--site): `glbUrl` and `thumbUrl`.
      const list = ordered.map(([section, { glb, thumb, glbUrl, thumbUrl, ...rest }]) => ({
        ...rest,
        section,
        hasModel: Boolean(glb || glbUrl),
        glbUrl: glbUrl ?? null,
        thumb: thumbUrl ?? (thumb ? `data:image/webp;base64,${thumb}` : null),
        html: highlight(highlighter, rest.source, palette),
      }));
      const sectionList = sections.map(({ title, blurb, examples }) => ({ title, blurb, examples }));
      const css = [...palette].map(([pair, i]) => {
        const [light, dark] = pair.split(' ');
        return `.t${i}{--l:${light};--d:${dark}}`;
      }).join('');
      const { playground: hasPlayground = false } = load();
      return `export const commit = ${JSON.stringify(commit)};\n` +
        `export const playground = ${JSON.stringify(hasPlayground)};\n` +
        `export const tokenCss = ${JSON.stringify(css)};\n` +
        `export const sections = ${JSON.stringify(sectionList)};\n` +
        `export const examples = ${JSON.stringify(list)};\n`;
    },
    transformIndexHtml: {
      order: 'post',
      handler() {
        return load()
          .examples.filter((e) => e.glb)
          .map((e) => ({
            tag: 'script',
            attrs: { type: 'text/plain', id: `glb-${e.name}` },
            children: e.glb,
            injectTo: 'body',
          }));
      },
    },
  };
}

const escape = (t) => t.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

// Shiki's own HTML puts both theme colours inline on every token, which is ~25x the source size
// over all the examples. Emit one short class per colour pair instead, merging neighbouring tokens
// of the same colour; the classes are defined once in `tokenCss`.
function highlight(highlighter, source, palette) {
  const { tokens } = highlighter.codeToTokens(source, {
    lang: 'solidcraft',
    themes: { light: 'github-light', dark: 'github-dark' },
  });
  const plain = `${highlighter.getTheme('github-light').fg} ${highlighter.getTheme('github-dark').fg}`
    .toLowerCase();
  const lines = tokens.map((line) => {
    let html = '';
    let open = null; // colour pair of the open span; plain text goes unwrapped
    for (const token of line) {
      const style = token.htmlStyle ?? {};
      const pair = `${style.color} ${style['--shiki-dark']}`.toLowerCase();
      // Whitespace has no visible colour, so it joins whatever span is open.
      if (pair !== open && !/^\s*$/.test(token.content)) {
        if (open) html += '</span>';
        open = pair === plain ? null : pair;
        if (open) {
          if (!palette.has(pair)) palette.set(pair, palette.size);
          html += `<span class="t${palette.get(pair)}">`;
        }
      }
      html += escape(token.content);
    }
    if (open) html += '</span>';
    return `<span class="line">${html}</span>`;
  });
  return `<pre class="shiki"><code>${lines.join('\n')}</code></pre>`;
}

// ---- The playground page (playground.html): every example's files, the grammar's word lists for
// the editor, and the engine module (built by ci/playground-engine.py) embedded like the GLBs.
const examplesDir = here('../examples');
const enginePath = process.env.PLAYGROUND_ENGINE || here('../build/playground/engine.wasm.gz');
// Hosted (--site): the engine is a file beside the page, and the gallery is the site's index.
const engineUrl = process.env.PLAYGROUND_ENGINE_URL || null;
const galleryHref = process.env.PLAYGROUND_GALLERY || 'examples.html';
const timingsPath = process.env.PLAYGROUND_TIMINGS || here('../build/playground/timings.json');
const readTimings = () => {
  try { return JSON.parse(readFileSync(timingsPath, 'utf8')); } catch { return {}; }
};

function walk(dir, prefix = '') {
  const out = [];
  for (const name of readdirSync(dir).sort()) {
    const full = `${dir}/${name}`;
    if (statSync(full).isDirectory()) out.push(...walk(full, `${prefix}${name}/`));
    else if (/\.(sc|svg|dxf)$/.test(name)) out.push([`${prefix}${name}`, readFileSync(full, 'utf8')]);
  }
  return out;
}

// Alternations in the grammar's rules, e.g. (extrude|revolve|…), as word lists.
function grammarWords() {
  const grammar = JSON.parse(readFileSync(grammarPath, 'utf8'));
  const words = (...rules) => {
    const found = new Set();
    for (const rule of rules) {
      const node = grammar.repository[rule];
      for (const p of node?.patterns ?? [node]) {
        const text = p?.match ?? p?.begin ?? '';
        for (const group of text.matchAll(/\(\??:?([A-Za-z_]+(?:\|[A-Za-z_]+)+)\)/g)) {
          for (const w of group[1].split('|')) found.add(w);
        }
      }
    }
    return [...found];
  };
  return {
    keyword: [...words('assembly'), 'design', 'param', 'let', 'sketch', 'dim', 'units', 'body', 'on', 'from', 'construction'],
    feature: [...words('feature', 'sketch-entity', 'sketch-constraint')],
    key: words('key'),
    modifier: words('modifier'),
    constant: [...words('plane', 'axis'), 'origin'],
  };
}

// The gallery's titles (ci/examples-site.py `describe`): words from the file name.
const ACRONYMS = new Set(['rc', 'pcb', 'esp32', 'led', 'usb', 'cd', 'tft']);
const SPELLINGS = { ipod: 'iPod', macbook: 'MacBook' };
function titleOf(name) {
  const words = name.replace(/^\d+_/, '').split('_').map((w) =>
    SPELLINGS[w] ?? (ACRONYMS.has(w) || /^[a-z]{2,}\d+$/.test(w) ? w.toUpperCase() : w));
  const title = words.join(' ');
  return title.charAt(0).toUpperCase() + title.slice(1);
}

function playground() {
  const id = 'virtual:playground';
  return {
    name: 'solidcraft-playground',
    resolveId: (source) => (source === id ? '\0' + id : null),
    load(resolved) {
      if (resolved !== '\0' + id) return null;
      const files = Object.fromEntries(walk(examplesDir).map(([p, text]) => [`examples/${p}`, text]));
      const byName = new Map();
      for (const path of Object.keys(files)) {
        const m = path.match(/^examples\/([^/]+)\.sc$/);
        if (m) byName.set(m[1], path);
      }
      const order = categories.flatMap((c) => c.examples.map((n) => [c.title, n])).filter(([, n]) => byName.has(n));
      const listed = new Set(order.map(([, n]) => n));
      for (const n of byName.keys()) if (!listed.has(n)) order.push(['Other', n]);
      const timings = readTimings();
      const list = order.map(([section, name]) => {
        const text = files[byName.get(name)];
        const number = name.match(/^(\d+)_/)?.[1] ?? null;
        const title = titleOf(name);
        const parts = Object.keys(files).filter((p) => p.startsWith(`examples/${name}_parts/`));
        const t = timings[name];
        // ms: the measured build time (a lower bound when timedOut); null when not measured.
        return { name, number, section, title, main: byName.get(name), parts, ms: t?.ms ?? null, timedOut: Boolean(t?.timedOut) };
      });
      return `export const files = ${JSON.stringify(files)};\n` +
        `export const examples = ${JSON.stringify(list)};\n` +
        `export const words = ${JSON.stringify(grammarWords())};\n` +
        `export const engineUrl = ${JSON.stringify(engineUrl)};\n` +
        `export const galleryHref = ${JSON.stringify(galleryHref)};\n`;
    },
    transformIndexHtml: {
      order: 'post',
      handler() {
        if (engineUrl) return [];
        if (!existsSync(enginePath)) {
          this?.warn?.(`no engine at ${enginePath}`);
          return [];
        }
        return [{
          tag: 'script',
          attrs: { type: 'text/plain', id: 'engine-wasm' },
          children: readFileSync(enginePath).toString('base64'),
          injectTo: 'body',
        }];
      },
    },
  };
}

// PAGE=playground builds playground.html; the default is the gallery (index.html). Each is one
// self-contained file, so they are separate builds.
const page = process.env.PAGE === 'playground' ? 'playground' : 'gallery';

export default defineConfig({
  plugins: [svelte(), page === 'playground' ? playground() : examples(), viteSingleFile()],
  build: {
    chunkSizeWarningLimit: 4096,
    rollupOptions: { input: here(page === 'playground' ? './playground.html' : './index.html') },
  },
});
