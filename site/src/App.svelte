<script>
  import { commit, examples } from 'virtual:examples';
  import Example from './Example.svelte';

  let query = $state('');
  let active = $state(examples[0]?.name);
  let theme = $state(readTheme());
  let navOpen = $state(false);

  const filtered = $derived.by(() => {
    const q = query.trim().toLowerCase();
    if (!q) return examples;
    return examples.filter((e) =>
      [e.name, e.title, e.summary].some((s) => s?.toLowerCase().includes(q)) ||
      e.source.toLowerCase().includes(q));
  });

  const built = examples.filter((e) => e.hasModel).length;
  const lines = examples.reduce((n, e) => n + e.source.split('\n').length, 0);

  function readTheme() {
    try { return localStorage.getItem('theme') || 'auto'; } catch { return 'auto'; }
  }

  $effect(() => {
    if (theme === 'auto') delete document.documentElement.dataset.theme;
    else document.documentElement.dataset.theme = theme;
    try { localStorage.setItem('theme', theme); } catch {}
  });

  function cycleTheme() {
    theme = { auto: 'light', light: 'dark', dark: 'auto' }[theme];
  }

  // Highlight the example nearest the top of the screen in the sidebar.
  $effect(() => {
    filtered; // re-observe when the list changes
    const sections = [...document.querySelectorAll('section.example')];
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) if (entry.isIntersecting) active = entry.target.id;
      },
      { rootMargin: '-20% 0px -70% 0px' },
    );
    sections.forEach((s) => observer.observe(s));
    return () => observer.disconnect();
  });

  $effect(() => {
    document.querySelector(`nav a[href="#${CSS.escape(active)}"]`)
      ?.scrollIntoView({ block: 'nearest' });
  });
</script>

<div class="layout">
  <aside class:open={navOpen}>
    <div class="brand">
      <svg viewBox="0 0 32 32" width="22" height="22" aria-hidden="true">
        <path d="M16 3 28 10v12L16 29 4 22V10z" fill="var(--accent)" />
        <path d="M16 3 28 10 16 17 4 10z" fill="var(--accent-soft)" opacity="0.9" />
      </svg>
      <span>solidcraft-dsl</span>
      <button class="icon" onclick={cycleTheme} title="Theme: {theme}" aria-label="Theme: {theme}">
        {#if theme === 'light'}☀{:else if theme === 'dark'}☾{:else}◐{/if}
      </button>
    </div>
    <input
      type="search"
      placeholder="Filter examples or source…"
      bind:value={query}
      aria-label="Filter examples"
    />
    <nav>
      {#each filtered as e (e.name)}
        <a href="#{e.name}" class:active={active === e.name} onclick={() => (navOpen = false)}>
          <span class="num">{e.number ?? '·'}</span>
          <span class="label">{e.title}</span>
          {#if !e.hasModel}<span class="fail" title="No model">!</span>{/if}
        </a>
      {:else}
        <p class="empty">No matches.</p>
      {/each}
    </nav>
  </aside>

  <main>
    <header class="intro">
      <button class="menu" onclick={() => (navOpen = !navOpen)} aria-label="Examples">☰</button>
      <div>
        <h1>Examples</h1>
        <p>
          Every <code>.sc</code> file in <code>examples/</code>, next to the model it compiles to.
          Drag to orbit, right-drag to pan, click then scroll to zoom, double-click to reset.
        </p>
        <p class="meta">
          {examples.length} examples · {built} built · {lines.toLocaleString()} lines
          {#if commit}· <code>{commit}</code>{/if}
        </p>
      </div>
    </header>

    {#each filtered as e (e.name)}
      <Example example={e} />
    {/each}
  </main>
</div>

<style>
  .layout {
    display: grid;
    grid-template-columns: var(--sidebar) minmax(0, 1fr);
    min-height: 100vh;
  }

  aside {
    position: sticky;
    top: 0;
    height: 100vh;
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 18px 12px 12px;
    background: var(--panel);
    border-right: 1px solid var(--border);
  }

  .brand {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0 6px;
    font-weight: 650;
    letter-spacing: -0.01em;
  }
  .brand span { flex: 1; }

  .icon {
    border: 1px solid var(--border);
    background: transparent;
    color: var(--muted);
    width: 28px;
    height: 28px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
  }
  .icon:hover { color: var(--text); }

  input[type='search'] {
    width: 100%;
    padding: 7px 10px;
    border: 1px solid var(--border);
    border-radius: 7px;
    background: var(--bg);
    color: var(--text);
    font: inherit;
    font-size: 13.5px;
  }
  input[type='search']:focus { outline: 2px solid var(--accent); outline-offset: -1px; }

  nav {
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 1px;
    margin: 0 -4px;
    padding: 0 4px;
  }
  nav a {
    display: flex;
    align-items: baseline;
    gap: 8px;
    padding: 5px 8px;
    border-radius: 6px;
    color: var(--muted);
    text-decoration: none;
    font-size: 13.5px;
    line-height: 1.35;
  }
  nav a:hover { background: var(--bg); color: var(--text); }
  nav a.active { background: var(--accent-soft); color: var(--text); }
  .num {
    flex: none;
    width: 1.7em;
    text-align: right;
    font: 12px var(--mono);
    opacity: 0.7;
  }
  .label { flex: 1; min-width: 0; }
  .fail { color: var(--danger); font-weight: 700; }
  .empty { color: var(--muted); padding: 0 8px; font-size: 13.5px; }

  main {
    min-width: 0;
    padding: 28px 32px 80px;
    max-width: 1600px;
  }

  .intro { display: flex; gap: 12px; margin-bottom: 28px; }
  h1 { margin: 0 0 6px; font-size: 28px; letter-spacing: -0.02em; }
  .intro p { margin: 0 0 4px; color: var(--muted); max-width: 70ch; }
  .intro .meta { font-size: 13px; }
  code { font-family: var(--mono); font-size: 0.9em; }

  .menu {
    display: none;
    flex: none;
    width: 36px;
    height: 36px;
    border: 1px solid var(--border);
    border-radius: 8px;
    background: var(--panel);
    color: var(--text);
    font-size: 16px;
  }

  @media (max-width: 860px) {
    .layout { grid-template-columns: minmax(0, 1fr); }
    aside {
      position: fixed;
      z-index: 10;
      inset: 0 auto 0 0;
      width: min(300px, 85vw);
      transform: translateX(-100%);
      transition: transform 0.2s;
      box-shadow: 0 0 40px rgb(0 0 0 / 0.2);
    }
    aside.open { transform: none; }
    .menu { display: block; }
    main { padding: 16px 16px 60px; }
  }
</style>
