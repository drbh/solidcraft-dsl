<script>
  import { commit, examples, sections } from 'virtual:examples';
  import Example from './Example.svelte';

  // Routing is the URL hash: `#<example name>` opens that example, anything else shows the grid
  // (`#s-<n>` scrolls it to section n). Old links of the form `#78_spider_chair` still work.
  const byName = new Map(examples.map((e) => [e.name, e]));
  let hash = $state(decodeURIComponent(location.hash.slice(1)));
  let query = $state('');
  let theme = $state(readTheme());
  let navOpen = $state(false);

  const current = $derived(byName.get(hash) ?? null);
  const index = $derived(current ? examples.indexOf(current) : -1);
  const prev = $derived(index > 0 ? examples[index - 1] : null);
  const next = $derived(index >= 0 && index < examples.length - 1 ? examples[index + 1] : null);

  const matches = $derived.by(() => {
    const q = query.trim().toLowerCase();
    if (!q) return null;
    return new Set(examples
      .filter((e) => [e.name, e.title, e.summary, e.section].some((s) => s?.toLowerCase().includes(q)) ||
        e.source.toLowerCase().includes(q))
      .map((e) => e.name));
  });
  const visible = $derived(sections
    .map((s, i) => ({
      ...s,
      id: `s-${i}`,
      items: s.examples.filter((n) => !matches || matches.has(n)).map((n) => byName.get(n)),
    }))
    .filter((s) => s.items.length));

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

  const cycleTheme = () => (theme = { auto: 'light', light: 'dark', dark: 'auto' }[theme]);

  // Coming back to the grid returns to the card you left from.
  let gridScroll = 0;
  let lastOpened = null;

  function onHash() {
    const target = decodeURIComponent(location.hash.slice(1));
    if (!byName.has(hash) && byName.has(target)) gridScroll = window.scrollY;
    hash = target;
    navOpen = false;
  }

  $effect(() => {
    if (current) {
      lastOpened = current.name;
      window.scrollTo(0, 0);
      document.title = `${current.number ? current.number + ' ' : ''}${current.title} · solidcraft-dsl examples`;
      document.querySelector(`nav a[href="#${CSS.escape(current.name)}"]`)?.scrollIntoView({ block: 'nearest' });
    } else {
      document.title = 'solidcraft-dsl examples';
      requestAnimationFrame(() => {
        const section = hash.startsWith('s-') && document.getElementById(hash);
        if (section) section.scrollIntoView();
        else if (lastOpened) {
          const card = document.querySelector(`.card[href="#${CSS.escape(lastOpened)}"]`);
          if (card) card.scrollIntoView({ block: 'center' });
          else window.scrollTo(0, gridScroll);
        }
      });
    }
  });

  function onKey(event) {
    if (event.target.closest?.('input, textarea') || event.metaKey || event.ctrlKey || event.altKey) return;
    if (!current) {
      if (event.key === '/') { event.preventDefault(); document.querySelector('input[type=search]')?.focus(); }
      return;
    }
    if (event.key === 'ArrowLeft' && prev) location.hash = prev.name;
    else if (event.key === 'ArrowRight' && next) location.hash = next.name;
    else if (event.key === 'Escape') location.hash = '';
  }

  function submitFilter(event) {
    // Enter in the filter opens the first match.
    if (event.key === 'Enter' && visible[0]) location.hash = visible[0].items[0].name;
  }
</script>

<svelte:window onhashchange={onHash} onkeydown={onKey} />

<div class="layout">
  <aside class:open={navOpen}>
    <div class="brand">
      <a href="#/" class="home">
        <svg viewBox="0 0 32 32" width="22" height="22" aria-hidden="true">
          <path d="M16 3 28 10v12L16 29 4 22V10z" fill="var(--accent)" />
          <path d="M16 3 28 10 16 17 4 10z" fill="var(--accent-soft)" opacity="0.9" />
        </svg>
        <span>solidcraft-dsl</span>
      </a>
      <button class="icon" onclick={cycleTheme} title="Theme: {theme}" aria-label="Theme: {theme}">
        {#if theme === 'light'}☀{:else if theme === 'dark'}☾{:else}◐{/if}
      </button>
    </div>
    <input
      type="search"
      placeholder="Filter examples or source…  /"
      bind:value={query}
      onkeydown={submitFilter}
      aria-label="Filter examples"
    />
    <nav>
      <a href="#/" class="all" class:active={!current}>All examples <span class="count">{examples.length}</span></a>
      {#each visible as s (s.title)}
        <div class="group">{s.title}</div>
        {#each s.items as e (e.name)}
          <a href="#{e.name}" class:active={current === e}>
            <span class="num">{e.number ?? '·'}</span>
            <span class="label">{e.title}</span>
            {#if !e.hasModel}<span class="fail" title="No model">!</span>{/if}
          </a>
        {/each}
      {:else}
        <p class="empty">No matches.</p>
      {/each}
    </nav>
  </aside>

  <main>
    <button class="menu" onclick={() => (navOpen = !navOpen)} aria-label="Examples">☰</button>
    {#if current}
      {#key current.name}
        <Example example={current} {prev} {next} />
      {/key}
    {:else}
      <header class="intro">
        <h1>Examples</h1>
        <p>
          Every <code>.sc</code> file in <code>examples/</code>, grouped by what it is. Open one to
          read its source beside the live model; <kbd>←</kbd> <kbd>→</kbd> step through them.
        </p>
        <p class="meta">
          {examples.length} examples · {built} built · {lines.toLocaleString()} lines
          {#if commit}· <code>{commit}</code>{/if}
        </p>
        <div class="chips">
          {#each visible as s (s.title)}
            <a href="#{s.id}">{s.title} <span>{s.items.length}</span></a>
          {/each}
        </div>
      </header>

      {#each visible as s (s.title)}
        <section class="group-section" id={s.id}>
          <h2>{s.title}</h2>
          {#if s.blurb}<p class="blurb">{s.blurb}</p>{/if}
          <div class="grid">
            {#each s.items as e (e.name)}
              <a class="card" href="#{e.name}">
                <div class="thumb">
                  {#if e.thumb}
                    <img src={e.thumb} alt="" loading="lazy" decoding="async" width="480" height="360" />
                  {:else if !e.hasModel}
                    <span class="nomodel">No model</span>
                  {/if}
                </div>
                <div class="text">
                  <div class="title">
                    {#if e.number}<span class="num">{e.number}</span>{/if}
                    {e.title}
                  </div>
                  {#if e.summary}<p>{e.summary}</p>{/if}
                </div>
              </a>
            {/each}
          </div>
        </section>
      {:else}
        <p class="empty">No examples match “{query}”.</p>
      {/each}
    {/if}
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

  .brand { display: flex; align-items: center; gap: 8px; padding: 0 6px; }
  .home {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8px;
    color: inherit;
    text-decoration: none;
    font-weight: 650;
    letter-spacing: -0.01em;
  }

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
    padding: 0 4px 12px;
  }
  nav a {
    display: flex;
    align-items: baseline;
    gap: 8px;
    padding: 4px 8px;
    border-radius: 6px;
    color: var(--muted);
    text-decoration: none;
    font-size: 13.5px;
    line-height: 1.35;
  }
  nav a:hover { background: var(--bg); color: var(--text); }
  nav a.active { background: var(--accent-soft); color: var(--text); }
  nav a.all { font-weight: 600; color: var(--text); justify-content: space-between; }
  .count { font: 12px var(--mono); color: var(--muted); }
  .group {
    margin: 14px 8px 4px;
    font-size: 11.5px;
    font-weight: 650;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    color: var(--muted);
  }
  nav .num {
    flex: none;
    width: 1.9em;
    text-align: right;
    font: 12px var(--mono);
    opacity: 0.7;
  }
  .label { flex: 1; min-width: 0; }
  .fail { color: var(--danger); font-weight: 700; }
  .empty { color: var(--muted); padding: 0 8px; font-size: 13.5px; }

  main {
    position: relative;
    min-width: 0;
    padding: 28px 32px 80px;
    max-width: 1600px;
  }

  .intro { margin-bottom: 8px; }
  h1 { margin: 0 0 6px; font-size: 28px; letter-spacing: -0.02em; }
  .intro p { margin: 0 0 4px; color: var(--muted); max-width: 72ch; }
  .intro .meta { font-size: 13px; }
  code { font-family: var(--mono); font-size: 0.9em; }
  kbd {
    padding: 0 5px;
    border: 1px solid var(--border);
    border-bottom-width: 2px;
    border-radius: 4px;
    font: 12px var(--mono);
  }

  .chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 14px; }
  .chips a {
    padding: 4px 10px;
    border: 1px solid var(--border);
    border-radius: 999px;
    background: var(--panel);
    color: var(--text);
    text-decoration: none;
    font-size: 13px;
  }
  .chips a:hover { border-color: var(--accent); }
  .chips span { color: var(--muted); font: 12px var(--mono); margin-left: 2px; }

  .group-section { padding-top: 28px; scroll-margin-top: 0; }
  h2 { margin: 0; font-size: 20px; letter-spacing: -0.01em; }
  .blurb { margin: 2px 0 14px; color: var(--muted); }

  .grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 14px;
  }
  .card {
    display: flex;
    flex-direction: column;
    border: 1px solid var(--border);
    border-radius: 10px;
    overflow: hidden;
    background: var(--panel);
    color: inherit;
    text-decoration: none;
    transition: border-color 0.12s, transform 0.12s;
  }
  .card:hover { border-color: var(--accent); transform: translateY(-1px); }
  .card:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
  .thumb {
    position: relative;
    aspect-ratio: 4 / 3;
    background: var(--viewport);
    display: grid;
    place-items: center;
  }
  .thumb img { width: 100%; height: 100%; object-fit: contain; display: block; }
  .nomodel { color: var(--danger); font-size: 13px; }
  .text { padding: 9px 12px 12px; border-top: 1px solid var(--border); }
  .title { font-weight: 600; font-size: 14.5px; line-height: 1.3; }
  .title .num {
    display: inline-block;
    margin-right: 4px;
    color: var(--accent);
    font: 600 12px var(--mono);
  }
  .text p {
    margin: 4px 0 0;
    color: var(--muted);
    font-size: 12.5px;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .menu {
    display: none;
    width: 36px;
    height: 36px;
    margin-bottom: 12px;
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
    .grid { grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 10px; }
  }
</style>
