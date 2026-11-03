// The playground's link to its engine: loads the embedded module once, and runs builds in a
// worker one at a time. Starting a build while one runs cancels it (the worker is terminated
// and replaced), so the newest source always wins.
import EngineWorker from './engine.worker.js?worker&inline';
import { engineUrl } from 'virtual:playground';

let modulePromise = null;
let worker = null;
let current = null; // { id, resolve, reject }
let nextId = 1;

/** Compile the engine: fetched from `engineUrl` on the hosted site, else from the page's
 *  <script id="engine-wasm"> (base64 of the gzipped module). */
export function loadEngine() {
  modulePromise ??= (async () => {
    if (engineUrl) {
      // Hosted: the gzipped module is a file beside the page.
      const response = await fetch(engineUrl);
      if (!response.ok) throw new Error(`${engineUrl}: ${response.status}`);
      const bytes = new Uint8Array(await response.arrayBuffer());
      if (bytes[0] !== 0x1f || bytes[1] !== 0x8b) return WebAssembly.compile(bytes); // already decoded
      const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream('gzip'));
      return WebAssembly.compile(await new Response(stream).arrayBuffer());
    }
    const tag = document.getElementById('engine-wasm');
    if (!tag) throw new Error('this page was built without the engine (run ci/examples-site.py --playground)');
    const bytes = Uint8Array.from(atob(tag.textContent.trim()), (c) => c.charCodeAt(0));
    const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream('gzip'));
    const wasm = await new Response(stream).arrayBuffer();
    tag.remove(); // the text is no longer needed once compiled
    return WebAssembly.compile(wasm);
  })();
  return modulePromise;
}

async function freshWorker() {
  const module = await loadEngine();
  const w = new EngineWorker();
  w.onmessage = ({ data }) => {
    if (!current || data.id !== current.id) return;
    const { resolve } = current;
    current = null;
    resolve({ ...data.result, stl: data.stl ? new Uint8Array(data.stl) : null });
  };
  w.onerror = (event) => {
    if (!current) return;
    const { resolve } = current;
    current = null;
    resolve({ ok: false, stage: 'engine', diagnostics: [{ stage: 'engine', severity: 'error', message: `the engine crashed: ${event.message}`, line: null }] });
    worker?.terminate();
    worker = null;
  };
  w.postMessage({ type: 'module', module });
  return w;
}

/** Stop the running build, if any. Its promise resolves with `{ cancelled: true }`. */
export function cancel() {
  if (!current) return;
  current.resolve({ cancelled: true });
  current = null;
  worker?.terminate();
  worker = null;
}

export const busy = () => current !== null;

/**
 * Build `main` (a path under examples/, e.g. 'examples/01_spacer.sc') from `files`
 * ({path: text}). Resolves with the engine's result: {ok, diagnostics, checks, timings, stl, …}.
 */
export async function build({ files, main, checks = false }) {
  cancel();
  worker ??= await freshWorker();
  const id = nextId++;
  return new Promise((resolve, reject) => {
    current = { id, resolve, reject };
    worker.postMessage({ type: 'build', id, files, main, checks });
  });
}
