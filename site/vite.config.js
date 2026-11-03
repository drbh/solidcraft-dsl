// Builds the examples page into one self-contained index.html.
//
// ci/examples-site.py writes .data/manifest.json (sources, thumbnails and compressed GLBs). The
// sources are highlighted here at build time with the repository's TextMate grammar, ordered into
// the sections of categories.js and handed to the app as `virtual:examples`. The GLBs never go
// through the bundler: they are injected into the HTML as inert <script type="text/plain"> blocks
// that the viewer decodes when an example is opened.
import { readFileSync } from 'node:fs';
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
      const list = ordered.map(([section, { glb, thumb, ...rest }]) => ({
        ...rest,
        section,
        hasModel: Boolean(glb),
        thumb: thumb ? `data:image/webp;base64,${thumb}` : null,
        html: highlight(highlighter, rest.source, palette),
      }));
      const sectionList = sections.map(({ title, blurb, examples }) => ({ title, blurb, examples }));
      const css = [...palette].map(([pair, i]) => {
        const [light, dark] = pair.split(' ');
        return `.t${i}{--l:${light};--d:${dark}}`;
      }).join('');
      return `export const commit = ${JSON.stringify(commit)};\n` +
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

export default defineConfig({
  plugins: [svelte(), examples(), viteSingleFile()],
  build: { chunkSizeWarningLimit: 4096 },
});
