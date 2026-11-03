// Times each example's build in the playground engine, so the playground can tell fast examples
// (fine to rebuild on every edit) from slow ones.
//
//   node scripts/timings.mjs <engine.wasm> <examples dir> <out.json> [--jobs N] [--limit S] <name>...
//
// Each build runs in its own worker thread through the same WASI shim the page uses, and is
// stopped after --limit seconds (default 20): past that it only matters that it is slow. Writes
// {name: {ms, ok, timedOut}} for the names given, merged into <out.json> if it exists.
import { existsSync, readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { Worker, isMainThread, parentPort, workerData } from 'node:worker_threads';
import { availableParallelism } from 'node:os';

if (!isMainThread) {
  const { wasm, files, main } = workerData;
  const { WASI, WASIProcExit, File, Directory, OpenFile, PreopenDirectory, ConsoleStdout } =
    await import('@bjorn3/browser_wasi_shim');
  const root = new Map();
  const encoder = new TextEncoder();
  for (const [path, text] of Object.entries(files)) {
    const parts = path.split('/');
    let dir = root;
    for (const part of parts.slice(0, -1)) {
      if (!dir.has(part)) dir.set(part, new Directory(new Map()));
      dir = dir.get(part).contents;
    }
    dir.set(parts.at(-1), new File(encoder.encode(text)));
  }
  const out = new PreopenDirectory('/out', new Map());
  const quiet = ConsoleStdout.lineBuffered(() => {});
  const wasi = new WASI(['sc-playground', `/work/${main}`], [], [new OpenFile(new File([])), quiet, quiet, new PreopenDirectory('/work', root), out], { debug: false });
  const instance = await WebAssembly.instantiate(wasm, { wasi_snapshot_preview1: wasi.wasiImport });
  const started = performance.now();
  try { wasi.start(instance); } catch (e) { if (!(e instanceof WASIProcExit)) throw e; }
  const ms = Math.round(performance.now() - started);
  const result = out.dir.contents.get('result.json');
  const ok = result ? JSON.parse(new TextDecoder().decode(result.data)).ok : false;
  parentPort.postMessage({ ms, ok });
} else {
  const args = process.argv.slice(2);
  const flag = (name, fallback) => {
    const i = args.indexOf(name);
    if (i < 0) return fallback;
    const [, value] = args.splice(i, 2);
    return Number(value);
  };
  const jobs = flag('--jobs', Math.max(1, Math.min(8, availableParallelism() - 2)));
  const limit = flag('--limit', 20) * 1000;
  const [wasmPath, examplesDir, outPath, ...names] = args;

  const files = {};
  const walk = (dir, prefix) => {
    for (const n of readdirSync(dir).sort()) {
      const full = join(dir, n);
      if (statSync(full).isDirectory()) walk(full, `${prefix}${n}/`);
      else if (/\.(sc|svg|dxf)$/.test(n)) files[`examples/${prefix}${n}`] = readFileSync(full, 'utf8');
    }
  };
  walk(examplesDir, '');
  const wasm = await WebAssembly.compile(readFileSync(wasmPath));
  const results = existsSync(outPath) ? JSON.parse(readFileSync(outPath, 'utf8')) : {};

  const time = (name) => new Promise((resolve) => {
    const worker = new Worker(new URL(import.meta.url), { workerData: { wasm, files, main: `examples/${name}.sc` } });
    const timer = setTimeout(() => { worker.terminate(); resolve({ ms: limit, ok: false, timedOut: true }); }, limit);
    worker.once('message', (r) => { clearTimeout(timer); worker.terminate(); resolve(r); });
    worker.once('error', (e) => { clearTimeout(timer); resolve({ ms: 0, ok: false, error: String(e.message || e) }); });
  });

  const queue = [...names];
  let done = 0;
  await Promise.all(Array.from({ length: jobs }, async () => {
    while (queue.length) {
      const name = queue.shift();
      const r = await time(name);
      results[name] = r;
      done++;
      console.error(`  ${String(done).padStart(3)}/${names.length} ${r.timedOut ? `>${limit / 1000} s` : `${(r.ms / 1000).toFixed(1)} s`}${r.ok ? '' : ' (failed)'}  ${name}`);
      writeFileSync(outPath, JSON.stringify(results, null, 1));
    }
  }));
}
