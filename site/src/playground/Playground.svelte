<script>
  import { onMount } from 'svelte';
  import { examples, files as bundled, words } from 'virtual:playground';
  import { createEditor } from './editor.js';
  import { build, cancel, loadEngine } from './engine.js';
  import { createView, modelFromStl } from '../lib/viewer.js';

  // ---- State
  // Examples measured (by ci/examples-site.py --playground) to build in more than FAST_MS are
  // listed apart, and open without building until asked, so playing around stays responsive.
  const FAST_MS = 5000;
  const isSlow = (e) => e.timedOut || (e.ms ?? 0) > FAST_MS;
  const secs = (ms) => (ms < 10_000 ? (ms / 1000).toFixed(1) : String(Math.round(ms / 1000)));
  const estimate = (e) => (e.timedOut ? `over ${Math.round(e.ms / 1000)} s` : `about ${secs(e.ms)} s`);
  const byName = new Map(examples.map((e) => [e.name, e]));
  const fast = examples.filter((e) => !isSlow(e));
  const sections = [...new Set(fast.map((e) => e.section))].map((title) => ({
    title, items: fast.filter((e) => e.section === title),
  }));
  const slowOnes = examples.filter(isSlow).sort((a, b) => a.ms - b.ms);

  let current = $state(null);         // the example open
  let active = $state('');            // the file in the editor (a path under examples/)
  let files = { ...bundled };         // every file, with this session's edits
  let edited = $state(new Set());     // paths that differ from the bundled copy
  let engine = $state('loading');     // loading | ready | error
  let engineError = $state('');
  let status = $state('idle');        // idle | building | done | failed | cancelled
  let result = $state(null);
  let auto = $state(true);
  let slow = $state(false);           // the last build was too slow to rebuild on every edit
  let edges = $state(true);
  let viewState = $state('idle');
  let theme = $state(readTheme());
  let tab = $state('problems');
  let params = $state([]);
  let elapsed = $state(0);
  let lastStl = null;
  let gated = $state(false);          // a slow example, opened but not built yet

  let editorEl, viewEl;
  let editor, view;
  let timer = null;
  let ticker = null;
  const AUTO_LIMIT_MS = 4000;

  const problems = $derived(result?.diagnostics ?? []);
  const checks = $derived(result?.checks ?? []);
  const failedChecks = $derived(checks.filter((c) => !c.passed).length);
  const fileList = $derived(current ? [current.main, ...current.parts] : []);

  function readTheme() {
    try { return localStorage.getItem('theme') || 'auto'; } catch { return 'auto'; }
  }
  $effect(() => {
    if (theme === 'auto') delete document.documentElement.dataset.theme;
    else document.documentElement.dataset.theme = theme;
    try { localStorage.setItem('theme', theme); } catch {}
  });
  const cycleTheme = () => (theme = { auto: 'light', light: 'dark', dark: 'auto' }[theme]);

  // ---- Saved edits (this browser only): one entry per edited file.
  const key = (path) => `playground:${path}`;
  function loadSaved(path) {
    try { return localStorage.getItem(key(path)); } catch { return null; }
  }
  function save(path, text) {
    try {
      if (text === bundled[path]) localStorage.removeItem(key(path));
      else localStorage.setItem(key(path), text);
    } catch {}
  }

  // ---- Opening examples
  async function readHash() {
    const params = new URLSearchParams(location.hash.slice(1));
    const name = params.get('ex');
    const shared = params.get('src');
    const example = byName.get(name) ?? byName.get('01_spacer') ?? examples[0];
    let text = null;
    if (shared) {
      try { text = await inflate(shared); } catch (e) { console.error('bad shared source', e); }
    }
    open(example, text);
  }

  function open(example, sharedText = null) {
    current = example;
    const next = new Set();
    for (const path of [example.main, ...example.parts]) {
      const saved = loadSaved(path);
      files[path] = saved ?? bundled[path];
      if (saved != null && saved !== bundled[path]) next.add(path);
    }
    if (sharedText != null) {
      files[example.main] = sharedText;
      if (sharedText !== bundled[example.main]) next.add(example.main);
    }
    edited = next;
    active = example.main;
    editor?.load(files[active]);
    readParams();
    result = null;
    lastStl = null;
    view?.clear();
    slow = isSlow(example);
    gated = slow;
    document.title = `${example.number ? example.number + ' ' : ''}${example.title} · playground`;
    const wanted = `ex=${example.name}`;
    if (!location.hash.includes(wanted) || sharedText == null) history.replaceState(null, '', `#${wanted}`);
    if (engine === 'ready' && !gated) run({ frame: true });
  }

  function buildGated() {
    gated = false;
    run({ frame: true });
  }

  function choose(event) {
    const example = byName.get(event.target.value);
    if (example) open(example);
  }

  function switchFile(path) {
    if (path === active) return;
    active = path;
    editor.load(files[path]);
    readParams();
    editor.setDiagnostics(path === current.main ? problems : []);
  }

  function resetFile() {
    files[active] = bundled[active];
    save(active, bundled[active]);
    edited.delete(active);
    edited = new Set(edited);
    editor.load(files[active]);
    readParams();
    schedule(true);
  }

  // ---- Parameters: numeric `param` lines become sliders that rewrite the line.
  const PARAM = /^(\s*param\s+([A-Za-z_]\w*)\s*=\s*)(-?(?:\d+(?:\.\d*)?|\.\d+))(\s*(mm|cm|m|in|ft|deg|rad)\b)?(.*)$/;
  function readParams() {
    const lines = files[active].split('\n');
    const found = [];
    lines.forEach((text, i) => {
      const m = text.match(PARAM);
      if (!m) return;
      const value = Number(m[3]);
      const unit = m[5] ?? '';
      const comment = m[6].match(/\/\/\s*(.*)$/)?.[1] ?? '';
      const prev = params.find((p) => p.name === m[2]);
      found.push({ name: m[2], value, unit, comment, line: i + 1, range: prev?.range ?? range(value, unit) });
    });
    params = found;
  }
  function range(v, unit) {
    const a = Math.abs(v);
    const integer = Number.isInteger(v) && !unit;
    const step = integer ? 1 : a >= 100 ? 1 : a >= 10 ? 0.5 : a >= 1 ? 0.1 : 0.01;
    if (unit === 'deg') return { min: Math.min(0, v), max: Math.max(360, v), step: a >= 10 ? 1 : 0.5 };
    if (v === 0) return { min: -10, max: 10, step };
    const max = v > 0 ? Math.max(v * 3, v + 10 * step) : 0;
    const min = v > 0 ? 0 : v * 3;
    return { min, max, step };
  }
  function setParam(p, value) {
    const text = files[active].split('\n')[p.line - 1];
    const m = text.match(PARAM);
    if (!m) return;
    const digits = p.range.step >= 1 ? 0 : p.range.step >= 0.1 ? 1 : 2;
    const formatted = String(Number(Number(value).toFixed(digits)));
    editor.replaceLine(p.line, `${m[1]}${formatted}${m[4] ?? ''}${m[6]}`);
  }

  // ---- Building
  function onEdit(text) {
    files[active] = text;
    save(active, text);
    const next = new Set(edited);
    if (text === bundled[active]) next.delete(active); else next.add(active);
    edited = next;
    readParams();
    schedule();
  }

  function schedule(force = false) {
    clearTimeout(timer);
    if (!force && (!auto || slow)) return;
    timer = setTimeout(() => run(), 350);
  }

  async function run({ withChecks = false, frame = false } = {}) {
    clearTimeout(timer);
    if (engine !== 'ready' || !current) return;
    gated = false;
    status = 'building';
    const started = performance.now();
    elapsed = 0;
    clearInterval(ticker);
    ticker = setInterval(() => (elapsed = performance.now() - started), 100);
    const snapshot = {};
    for (const [path, text] of Object.entries(files)) snapshot[path] = text;
    const r = await build({ files: snapshot, main: current.main, checks: withChecks });
    clearInterval(ticker);
    if (r.cancelled) return; // a newer build replaced this one
    result = r;
    const total = r.timings?.total_ms ?? performance.now() - started;
    slow = total > AUTO_LIMIT_MS;
    status = r.ok ? 'done' : 'failed';
    if (r.diagnostics?.some((d) => d.severity === 'error')) tab = 'problems';
    else if (withChecks) tab = 'checks';
    if (active === current.main) editor.setDiagnostics(r.diagnostics ?? []);
    if (r.stl) {
      lastStl = r.stl;
      view.show(modelFromStl(r.stl), { frame });
    }
  }

  function stop() {
    cancel();
    clearInterval(ticker);
    status = 'cancelled';
  }

  // ---- Sharing and downloads
  async function deflate(text) {
    const stream = new Blob([text]).stream().pipeThrough(new CompressionStream('deflate-raw'));
    const bytes = new Uint8Array(await new Response(stream).arrayBuffer());
    let out = '';
    for (let i = 0; i < bytes.length; i += 0x8000) out += String.fromCharCode(...bytes.subarray(i, i + 0x8000));
    return btoa(out).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  }
  async function inflate(text) {
    const b64 = text.replace(/-/g, '+').replace(/_/g, '/');
    const bytes = Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
    const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream('deflate-raw'));
    return new Response(stream).text();
  }
  let copied = $state(false);
  async function copyLink() {
    let hash = `ex=${current.name}`;
    if (files[current.main] !== bundled[current.main]) hash += `&src=${await deflate(files[current.main])}`;
    const url = `${location.href.split('#')[0]}#${hash}`;
    history.replaceState(null, '', `#${hash}`);
    try { await navigator.clipboard.writeText(url); copied = true; setTimeout(() => (copied = false), 1200); } catch {}
  }
  function download(blob, filename) {
    const url = URL.createObjectURL(blob);
    Object.assign(document.createElement('a'), { href: url, download: filename }).click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  const downloadSource = () => download(new Blob([files[active]], { type: 'text/plain' }), active.split('/').pop());
  const downloadStl = () => lastStl && download(new Blob([lastStl], { type: 'model/stl' }), `${current.name}.stl`);

  // ---- Formatting
  const seconds = (ms) => (ms >= 10_000 ? `${(ms / 1000).toFixed(0)} s` : ms >= 1000 ? `${(ms / 1000).toFixed(1)} s` : `${Math.round(ms)} ms`);
  const count = (n) => n.toLocaleString();
  const fileLabel = (path) => path.replace(`examples/${current?.name}_parts/`, '').replace(/^examples\//, '');

  onMount(() => {
    editor = createEditor(editorEl, {
      words,
      doc: '',
      onChange: onEdit,
      onRun: () => run(),
      onCheck: () => run({ withChecks: true }),
    });
    view = createView(viewEl, { onState: (s) => (viewState = s) });
    readHash();
    loadEngine().then(
      () => { engine = 'ready'; if (!gated) run({ frame: true }); },
      (e) => { engine = 'error'; engineError = String(e.message || e); },
    );
    const onHash = () => readHash();
    window.addEventListener('hashchange', onHash);
    return () => {
      window.removeEventListener('hashchange', onHash);
      editor.destroy();
      view.destroy();
    };
  });
</script>

<div class="page">
  <header>
    <a class="brand" href="examples.html{current ? `#${current.name}` : ''}" title="Back to this example in the gallery">
      <svg viewBox="0 0 32 32" width="20" height="20" aria-hidden="true">
        <path d="M16 3 28 10v12L16 29 4 22V10z" fill="var(--accent)" />
        <path d="M16 3 28 10 16 17 4 10z" fill="var(--accent-soft)" opacity="0.9" />
      </svg>
      <span>Playground</span>
    </a>
    <select value={current?.name} onchange={choose} aria-label="Example">
      {#each sections as s (s.title)}
        <optgroup label={s.title}>
          {#each s.items as e (e.name)}
            <option value={e.name}>{e.number ? `${e.number}  ` : ''}{e.title}</option>
          {/each}
        </optgroup>
      {/each}
      {#if slowOnes.length}
        <optgroup label="Slow to build (over {FAST_MS / 1000} s each)">
          {#each slowOnes as e (e.name)}
            <option value={e.name}>{e.number ? `${e.number}  ` : ''}{e.title} · {estimate(e)}</option>
          {/each}
        </optgroup>
      {/if}
    </select>
    <div class="actions">
      {#if status === 'building'}
        <button class="primary" onclick={stop} title="Stop this build">Stop · {seconds(elapsed)}</button>
      {:else}
        <button class="primary" onclick={() => run()} disabled={engine !== 'ready'} title="Build (⌘↵)">Run</button>
      {/if}
      <button onclick={() => run({ withChecks: true })} disabled={engine !== 'ready'} title="Build and verify the file's checks (⇧⌘↵)">Check</button>
      <label class="toggle" title="Rebuild after each edit (off automatically when a build takes over {AUTO_LIMIT_MS / 1000} s)">
        <input type="checkbox" bind:checked={auto} /> Auto
      </label>
      <span class="sep"></span>
      <button onclick={copyLink} title="Copy a link to this example, with your edits">{copied ? 'Copied' : 'Share'}</button>
      <button onclick={downloadSource} title="Download this file">.sc</button>
      <button onclick={downloadStl} disabled={!lastStl} title="Download the model">.stl</button>
      <button class="icon" onclick={cycleTheme} title="Theme: {theme}" aria-label="Theme: {theme}">
        {#if theme === 'light'}☀{:else if theme === 'dark'}☾{:else}◐{/if}
      </button>
    </div>
  </header>

  <div class="panes">
    <section class="source">
      <div class="tabs">
        {#each fileList as path (path)}
          <button class:active={path === active} onclick={() => switchFile(path)}>
            {fileLabel(path)}{#if edited.has(path)}<span class="dot" title="Edited"></span>{/if}
          </button>
        {/each}
        <span class="spacer"></span>
        {#if edited.has(active)}<button class="link" onclick={resetFile} title="Discard your edits to this file">Reset</button>{/if}
      </div>
      <div class="editor" bind:this={editorEl}></div>
    </section>

    <section class="output">
      <div class="viewer-bar">
        <span class="state {status}">
          {#if engine === 'loading'}
            Loading the engine…
          {:else if engine === 'error'}
            Engine unavailable
          {:else if status === 'building'}
            Building… {seconds(elapsed)}
          {:else if status === 'cancelled'}
            Stopped
          {:else if gated}
            Not built yet: {estimate(current)} per build
          {:else if result?.ok}
            Built in {seconds(result.wall_ms ?? result.timings.total_ms)} · {count(result.triangles)} tris
          {:else if result}
            {result.stage === 'compile' ? 'Compile error' : 'Build failed'}
          {/if}
        </span>
        {#if slow && auto && !gated && status !== 'building'}<span class="hint">slow: ⌘↵ to rebuild</span>{/if}
        <span class="spacer"></span>
        <button onclick={() => { edges = !edges; view?.setEdges(edges); }} aria-pressed={edges}>Edges</button>
        <button onclick={() => view?.frame()}>Reset view</button>
      </div>
      <div class="viewport" class:ready={viewState === 'ready'} bind:this={viewEl}>
        {#if engine === 'error'}
          <div class="note error"><strong>Engine unavailable</strong><span>{engineError}</span></div>
        {:else if gated}
          <div class="note gate">
            <strong>This one is slow to build</strong>
            <span>{current.title} takes {estimate(current)} to build in the browser, so it doesn't rebuild as you type. Edit freely and build when you're ready.</span>
            <button class="primary" onclick={buildGated} disabled={engine !== 'ready'}>Build it ({estimate(current)})</button>
            <span class="muted-note">or pick one of the fast examples from the list</span>
          </div>
        {:else if !lastStl && (engine === 'loading' || status === 'building')}
          <div class="note"><span class="spinner"></span><span>{engine === 'loading' ? 'Loading the engine (about 15 MB)…' : 'Building…'}</span></div>
        {:else if !lastStl && result && !result.ok}
          <div class="note"><span>No model: see Problems.</span></div>
        {/if}
        {#if status === 'building' && lastStl}<div class="busy"></div>{/if}
      </div>

      <div class="panel">
        <div class="panel-tabs">
          <button class:active={tab === 'params'} onclick={() => (tab = 'params')}>Parameters <span>{params.length}</span></button>
          <button class:active={tab === 'problems'} onclick={() => (tab = 'problems')}>
            Problems <span class:bad={problems.some((d) => d.severity === 'error')}>{problems.length}</span>
          </button>
          <button class:active={tab === 'checks'} onclick={() => (tab = 'checks')}>
            Checks {#if checks.length}<span class:bad={failedChecks}>{checks.length - failedChecks}/{checks.length}</span>{/if}
          </button>
          <span class="spacer"></span>
          {#if result?.timings}
            <span class="timings" title="compile · engine · checks · export">
              {seconds(result.timings.compile_ms ?? 0)} · {seconds(result.timings.engine_ms ?? 0)}{#if result.timings.checks_ms} · {seconds(result.timings.checks_ms)}{/if}
            </span>
          {/if}
        </div>
        <div class="panel-body">
          {#if tab === 'params'}
            {#if params.length}
              <div class="params">
                {#each params as p (p.name)}
                  <label class="param" title={p.comment}>
                    <button class="name link" onclick={() => editor.goto(p.line)}>{p.name}</button>
                    <input type="range" min={p.range.min} max={p.range.max} step={p.range.step} value={p.value}
                      oninput={(e) => setParam(p, e.target.value)} />
                    <input type="number" step={p.range.step} value={p.value}
                      onchange={(e) => setParam(p, e.target.value)} />
                    <span class="unit">{p.unit}</span>
                  </label>
                {/each}
              </div>
            {:else}
              <p class="empty">No numeric <code>param</code> lines in this file.</p>
            {/if}
          {:else if tab === 'problems'}
            {#if problems.length}
              <ul class="problems">
                {#each problems as d, i (i)}
                  <li class={d.severity}>
                    {#if d.line && active === current?.main}
                      <button class="link" onclick={() => editor.goto(d.line)}>line {d.line}</button>
                    {:else if d.line}<span class="muted">line {d.line}</span>{/if}
                    <span class="stage">{d.stage}</span>
                    <span class="message">{d.message}</span>
                  </li>
                {/each}
              </ul>
            {:else if result}
              <p class="empty">{result.ok ? 'No problems.' : 'The build failed without a message.'}</p>
            {:else}
              <p class="empty">Edits rebuild the model; ⌘↵ builds now, ⇧⌘↵ also runs the file's checks.</p>
            {/if}
          {:else}
            {#if checks.length}
              <ul class="checks">
                {#each checks as c, i (i)}
                  <li class:fail={!c.passed}>
                    <span class="mark">{c.passed ? '✓' : '✗'}</span>
                    {#if c.line}<button class="link" onclick={() => editor.goto(c.line)}>{c.statement}</button>{:else}<span>{c.statement}</span>{/if}
                    <span class="muted">{c.actual ?? '–'} {c.unit ?? ''}</span>
                  </li>
                {/each}
              </ul>
            {:else}
              <p class="empty">Checks run with <strong>Check</strong> (⇧⌘↵): clearances, overhangs, sizes and one-piece bodies, as <code>evaluate</code> verifies them.</p>
            {/if}
          {/if}
        </div>
      </div>
    </section>
  </div>
</div>

<style>
  .page { display: flex; flex-direction: column; height: 100vh; }

  header {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 12px;
    border-bottom: 1px solid var(--border);
    background: var(--panel);
    flex-wrap: wrap;
  }
  .brand { display: flex; align-items: center; gap: 8px; color: inherit; text-decoration: none; font-weight: 650; }
  select {
    min-width: 0;
    max-width: 340px;
    flex: 1 1 220px;
    padding: 5px 8px;
    border: 1px solid var(--border);
    border-radius: 7px;
    background: var(--bg);
    color: var(--text);
    font: inherit;
    font-size: 13.5px;
  }
  .actions { display: flex; align-items: center; gap: 6px; margin-left: auto; flex-wrap: wrap; }
  .sep { width: 1px; height: 20px; background: var(--border); margin: 0 4px; }

  button {
    padding: 4px 10px;
    border: 1px solid var(--border);
    border-radius: 6px;
    background: var(--bg);
    color: var(--text);
    font: inherit;
    font-size: 13px;
    cursor: pointer;
  }
  button:hover:not(:disabled) { border-color: var(--accent); }
  button:disabled { opacity: 0.5; cursor: default; }
  button.primary { background: var(--accent); border-color: var(--accent); color: #fff; font-weight: 600; min-width: 64px; }
  button[aria-pressed='true'] { color: var(--accent); }
  button.icon { width: 30px; padding: 4px 0; color: var(--muted); }
  button.link { border: none; background: none; padding: 0; color: var(--accent); font-size: inherit; text-align: left; }
  button.link:hover { text-decoration: underline; }
  .toggle { display: flex; align-items: center; gap: 4px; font-size: 13px; color: var(--muted); }

  .panes {
    flex: 1;
    min-height: 0;
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }
  .source, .output { display: flex; flex-direction: column; min-height: 0; min-width: 0; }
  .source { border-right: 1px solid var(--border); background: var(--code-bg); }

  .tabs, .viewer-bar, .panel-tabs {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 5px 8px;
    border-bottom: 1px solid var(--border);
    background: var(--panel);
    font-size: 12.5px;
    min-height: 37px;
  }
  .tabs { overflow-x: auto; }
  .tabs button:not(.link) { font-family: var(--mono); font-size: 12px; color: var(--muted); white-space: nowrap; }
  .tabs button.active { color: var(--text); border-color: var(--accent); }
  .dot { display: inline-block; width: 6px; height: 6px; margin-left: 6px; border-radius: 50%; background: var(--accent); vertical-align: 1px; }
  .spacer { flex: 1; }
  .editor { flex: 1; min-height: 0; overflow: hidden; }
  .editor :global(.cm-editor) { height: 100%; }

  .state { font-weight: 600; }
  .state.failed { color: var(--danger); }
  .hint { color: var(--muted); }

  .viewport {
    position: relative;
    flex: 1;
    min-height: 0;
    background: var(--viewport);
    cursor: grab;
    touch-action: none;
  }
  .viewport:active { cursor: grabbing; }
  .viewport :global(canvas.gl) { position: absolute; inset: 0; width: 100%; height: 100%; display: block; opacity: 0; transition: opacity 0.15s; }
  .viewport.ready :global(canvas.gl) { opacity: 1; }
  .note {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: var(--muted);
    font-size: 13.5px;
    text-align: center;
    padding: 24px;
  }
  .note.error strong { color: var(--danger); }
  .note.gate {
    inset: auto;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: min(420px, calc(100% - 32px));
    border: 1px solid var(--border);
    border-radius: 12px;
    background: var(--panel);
    box-shadow: 0 8px 30px rgb(0 0 0 / 0.08);
  }
  .note.gate strong { color: var(--text); font-size: 15px; }
  .note.gate span { max-width: 46ch; line-height: 1.45; }
  .note.gate .primary { margin-top: 6px; }
  .muted-note { font-size: 12px; }
  .note.error span { font-family: var(--mono); font-size: 12px; max-width: 60ch; }
  .busy {
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
    height: 2px;
    background: linear-gradient(90deg, transparent, var(--accent), transparent);
    background-size: 40% 100%;
    background-repeat: no-repeat;
    animation: slide 1.1s linear infinite;
  }
  @keyframes slide { from { background-position: -40% 0; } to { background-position: 140% 0; } }
  .spinner {
    width: 22px;
    height: 22px;
    border: 2px solid var(--border);
    border-top-color: var(--accent);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }
  @keyframes spin { to { transform: rotate(360deg); } }

  .panel { height: min(38vh, 340px); display: flex; flex-direction: column; border-top: 1px solid var(--border); background: var(--panel); }
  .panel-tabs { border-top: none; }
  .panel-tabs button:not(.link) { border: none; background: none; color: var(--muted); padding: 4px 8px; border-radius: 6px; }
  .panel-tabs button.active { color: var(--text); background: var(--accent-soft); }
  .panel-tabs span { font: 11.5px var(--mono); margin-left: 3px; color: var(--muted); }
  .panel-tabs span.bad { color: var(--danger); }
  .timings { font: 11.5px var(--mono); color: var(--muted); }
  .panel-body { flex: 1; overflow: auto; padding: 8px 12px 12px; font-size: 13px; }
  .empty { color: var(--muted); margin: 6px 0; }

  .params { display: grid; grid-template-columns: max-content minmax(80px, 1fr) 76px max-content; gap: 6px 10px; align-items: center; }
  .param { display: contents; }
  .param .name { font: 12.5px var(--mono); }
  .param input[type='range'] { width: 100%; accent-color: var(--accent); }
  .param input[type='number'] {
    width: 76px;
    padding: 2px 6px;
    border: 1px solid var(--border);
    border-radius: 5px;
    background: var(--bg);
    color: var(--text);
    font: 12px var(--mono);
  }
  .unit { color: var(--muted); font: 12px var(--mono); min-width: 2em; }

  ul { list-style: none; margin: 0; padding: 0; }
  .problems li, .checks li { display: flex; gap: 10px; align-items: baseline; padding: 4px 0; border-bottom: 1px solid var(--border); }
  .problems li.error .stage { color: var(--danger); }
  .problems li.warning .stage { color: #b7791f; }
  .stage { font: 600 11px var(--mono); text-transform: uppercase; min-width: 6em; }
  .message { font: 12px var(--mono); white-space: pre-wrap; word-break: break-word; }
  .problems .link, .checks .link { font: 12px var(--mono); white-space: nowrap; }
  .checks .mark { color: #2da44e; font-weight: 700; }
  .checks li.fail .mark { color: var(--danger); }
  .checks .link { white-space: normal; text-align: left; }
  .muted { color: var(--muted); font: 12px var(--mono); white-space: nowrap; margin-left: auto; }
  code { font-family: var(--mono); font-size: 0.92em; }

  @media (max-width: 900px) {
    .page { height: auto; min-height: 100vh; }
    .panes { grid-template-columns: minmax(0, 1fr); }
    .source { border-right: none; height: 55vh; }
    .viewport { height: min(80vw, 420px); flex: none; }
    .panel { height: auto; max-height: 50vh; }
  }
</style>
