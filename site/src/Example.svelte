<script>
  import { createView, glbBlob, loadModel } from './lib/viewer.js';
  import { playground } from 'virtual:examples';

  let { example, prev = null, next = null } = $props();

  let viewport = $state();
  let view;
  let status = $state('idle');
  let message = $state('');
  let edges = $state(true);
  let copied = $state(false);

  const lineCount = $derived(example.source.split('\n').length);
  const size = $derived(example.size?.map((v) => (v >= 100 ? v.toFixed(0) : v.toFixed(1))).join(' × '));

  $effect(() => {
    if (!example.hasModel) return;
    view = createView(viewport, {
      onState: (s, m = '') => { status = s; message = m; },
    });
    view.show(loadModel(example.name));
    return () => view.destroy();
  });

  const playgroundTitle = $derived.by(() => {
    const ms = example.playground_ms;
    if (!ms) return 'Edit and rebuild it in the browser';
    const time = example.playground_timed_out ? `over ${Math.round(ms / 1000)} s`
      : `about ${ms < 10_000 ? (ms / 1000).toFixed(1) : Math.round(ms / 1000)} s`;
    return `Edit and rebuild it in the browser: ${time} per build${example.playground_slow ? ", so it won't rebuild as you type" : ''}`;
  });

  const short = (e) => `${e.number ? e.number + ' ' : ''}${e.title}`;

  async function copy() {
    await navigator.clipboard.writeText(example.source);
    copied = true;
    setTimeout(() => (copied = false), 1200);
  }

  function save(blob, filename) {
    const url = URL.createObjectURL(blob);
    const a = Object.assign(document.createElement('a'), { href: url, download: filename });
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  const download = async () => save(await glbBlob(example.name), `${example.name}.glb`);
  const downloadSource = () =>
    save(new Blob([example.source], { type: 'text/plain' }), `${example.name}.sc`);

  function toggleEdges() {
    edges = !edges;
    view?.setEdges(edges);
  }

  function formatCount(n) {
    return n >= 1000 ? `${(n / 1000).toFixed(n >= 10_000 ? 0 : 1)}k` : String(n);
  }
</script>

<section class="example" id={example.name}>
  <div class="crumbs">
    <a href="#/">All examples</a>
    <span>/</span>
    <span>{example.section}</span>
    <span class="spacer"></span>
    {#if prev}<a class="step" href="#{prev.name}" title="Previous (←)">← {short(prev)}</a>{/if}
    {#if next}<a class="step" href="#{next.name}" title="Next (→)">{short(next)} →</a>{/if}
  </div>
  <header>
    <h2>
      {#if example.number}<span class="num">{example.number}</span>{/if}
      {example.title}
    </h2>
    {#if example.summary}<p class="summary">{example.summary}</p>{/if}
  </header>

  <div class="panes" style:--fit="{lineCount * 20 + 66}px">
    <div class="code">
      <div class="bar">
        <span class="file">{example.file}</span>
        <span class="muted">{lineCount} lines</span>
        <span class="spacer"></span>
        <button onclick={copy}>{copied ? 'Copied' : 'Copy'}</button>
        <button onclick={downloadSource}>.sc</button>
        {#if playground}
          <a class="open" class:slow={example.playground_slow} href="playground.html#ex={example.name}"
            title={playgroundTitle}>
            Playground{#if example.playground_slow}<span class="slow-tag">slow</span>{/if}
          </a>
        {/if}
      </div>
      <div class="scroll">{@html example.html}</div>
    </div>

    <div class="model">
      <div class="bar">
        {#if size}<span class="muted">{size} mm</span>{/if}
        {#if example.triangles}<span class="muted">{formatCount(example.triangles)} tris</span>{/if}
        <span class="spacer"></span>
        {#if example.hasModel}
          <button onclick={toggleEdges} aria-pressed={edges}>Edges</button>
          <button onclick={() => view?.frame()}>Reset</button>
          <button onclick={download}>.glb</button>
        {/if}
      </div>
      <div class="viewport" class:ready={status === 'ready'} bind:this={viewport}>
        {#if !example.hasModel}
          <div class="note error">
            <strong>No model</strong>
            <span>{example.error}</span>
          </div>
        {:else if status === 'error'}
          <div class="note error"><strong>Could not load</strong><span>{message}</span></div>
        {:else if status !== 'ready'}
          <div class="note"><span class="spinner"></span></div>
        {/if}
      </div>
    </div>
  </div>
</section>

<style>
  .example { margin: 0; }

  .crumbs {
    display: flex;
    align-items: baseline;
    gap: 8px;
    margin-bottom: 14px;
    font-size: 13px;
    color: var(--muted);
    flex-wrap: wrap;
  }
  .crumbs a { color: var(--accent); text-decoration: none; }
  .crumbs a:hover { text-decoration: underline; }
  .crumbs .spacer { flex: 1; }
  .crumbs .step {
    padding: 3px 10px;
    border: 1px solid var(--border);
    border-radius: 6px;
    background: var(--panel);
    color: var(--text);
    max-width: 260px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .crumbs .step:hover { border-color: var(--accent); text-decoration: none; }

  header { margin-bottom: 12px; }
  h2 { margin: 0; font-size: 22px; letter-spacing: -0.01em; }
  h2 .num {
    display: inline-block;
    min-width: 2em;
    margin-right: 6px;
    padding: 1px 6px;
    border-radius: 6px;
    background: var(--accent-soft);
    color: var(--accent);
    font: 600 13px/1.6 var(--mono);
    text-align: center;
    vertical-align: 2px;
  }
  .summary { margin: 4px 0 0; color: var(--muted); max-width: 90ch; }

  .panes {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    gap: 16px;
    /* Short sources get a shorter row; long ones scroll inside the code pane. */
    height: clamp(420px, min(calc(100vh - 170px), var(--fit)), 1100px);
  }

  .code, .model {
    display: flex;
    flex-direction: column;
    min-height: 0;
    border: 1px solid var(--border);
    border-radius: 10px;
    overflow: hidden;
  }
  .code { background: var(--code-bg); }
  .model { background: transparent; }

  .bar {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 6px 8px 6px 12px;
    border-bottom: 1px solid var(--border);
    background: var(--panel);
    font-size: 12.5px;
    min-height: 37px;
  }
  .file {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-family: var(--mono);
  }
  .muted { color: var(--muted); white-space: nowrap; }
  .spacer { flex: 1; }
  .bar button {
    flex: none;
    padding: 3px 9px;
    border: 1px solid var(--border);
    border-radius: 6px;
    background: var(--bg);
    color: var(--muted);
    font: inherit;
    cursor: pointer;
  }
  .bar button:hover { color: var(--text); }
  .bar .open {
    flex: none;
    padding: 3px 9px;
    border: 1px solid var(--accent);
    border-radius: 6px;
    background: var(--accent);
    color: #fff;
    text-decoration: none;
    font-weight: 600;
  }
  .bar .open.slow {
    background: transparent;
    color: var(--accent);
  }
  .slow-tag {
    margin-left: 6px;
    font: 500 11px var(--mono);
    color: var(--muted);
  }
  .bar button[aria-pressed='true'] {
    color: var(--accent);
    border-color: color-mix(in srgb, var(--accent) 45%, var(--border));
  }

  .scroll { flex: 1; overflow: auto; min-height: 0; }

  /* The page's one WebGL canvas is moved in here (see lib/viewer.js). */
  .viewport {
    position: relative;
    flex: 1;
    min-height: 0;
    background: var(--viewport);
    cursor: grab;
    touch-action: none;
  }
  .viewport:active { cursor: grabbing; }
  .viewport :global(canvas.gl) {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    display: block;
    opacity: 0;
    transition: opacity 0.15s;
  }
  .viewport.ready :global(canvas.gl) { opacity: 1; }

  .note {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 24px;
    color: var(--muted);
    text-align: center;
    font-size: 13.5px;
  }
  .note.error strong { color: var(--danger); }
  .note.error span { font-family: var(--mono); font-size: 12px; max-width: 60ch; }

  .spinner {
    width: 22px;
    height: 22px;
    border: 2px solid var(--border);
    border-top-color: var(--accent);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }
  @keyframes spin { to { transform: rotate(360deg); } }

  @media (max-width: 1100px) {
    .panes { grid-template-columns: minmax(0, 1fr); height: auto; }
    .code { max-height: 60vh; }
    .model { height: min(80vw, 520px); order: -1; }
  }
  @media (max-width: 520px) {
    .code .muted { display: none; }
  }
</style>
