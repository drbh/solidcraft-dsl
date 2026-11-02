<script>
  import { createView, glbBlob } from './lib/viewer.js';

  let { example } = $props();

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
    view = createView(viewport, example.name, {
      onState: (s, m = '') => { status = s; message = m; },
    });
    return () => view.destroy();
  });

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
  <header>
    <h2>
      <a href="#{example.name}">
        {#if example.number}<span class="num">{example.number}</span>{/if}
        {example.title}
      </a>
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
  .example { margin: 0 0 56px; scroll-margin-top: 16px; }

  header { margin-bottom: 12px; }
  h2 { margin: 0; font-size: 19px; letter-spacing: -0.01em; }
  h2 a { color: inherit; text-decoration: none; }
  h2 a:hover { color: var(--accent); }
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
    height: clamp(380px, min(68vh, var(--fit)), 760px);
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
  .bar button[aria-pressed='true'] {
    color: var(--accent);
    border-color: color-mix(in srgb, var(--accent) 45%, var(--border));
  }

  .scroll { flex: 1; overflow: auto; min-height: 0; }

  /* Transparent once the model is drawn: the page's shared canvas shows through underneath. */
  .viewport {
    position: relative;
    flex: 1;
    min-height: 0;
    background: var(--viewport);
    cursor: grab;
    touch-action: none;
  }
  .viewport.ready { background: transparent; }
  .viewport:active { cursor: grabbing; }

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
    .model { height: min(70vw, 460px); }
  }
  @media (max-width: 520px) {
    .code .muted { display: none; }
  }
</style>
