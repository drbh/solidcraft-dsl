// Runs one playground build per message, off the main thread.
//
// The engine (site/playground/engine, built by ci/playground-engine.py) is a WASI command: a
// fresh instance per build gets an in-memory file system with the design's files under /work
// and an empty /out, runs to completion and is thrown away, so every build starts from a clean
// heap. The compiled WebAssembly.Module is sent once and reused; cancelling a build means the
// page terminates this worker and starts another.
import { WASI, WASIProcExit, File, Directory, OpenFile, PreopenDirectory, ConsoleStdout } from '@bjorn3/browser_wasi_shim';

let module = null;

function tree(files) {
  // {'examples/a.sc': text, 'examples/a_parts/b.sc': text} -> nested Directory contents.
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
  return root;
}

self.onmessage = async ({ data }) => {
  if (data.type === 'module') {
    module = data.module;
    return;
  }
  if (data.type !== 'build') return;
  const { id, files, main, checks } = data;
  const log = [];
  try {
    if (!module) throw new Error('the engine is not loaded yet');
    const out = new PreopenDirectory('/out', new Map());
    const line = (prefix) => ConsoleStdout.lineBuffered((text) => log.length < 200 && log.push(prefix + text));
    const fds = [
      new OpenFile(new File([])),
      line(''),
      line(''),
      new PreopenDirectory('/work', tree(files)),
      out,
    ];
    const args = ['sc-playground', `/work/${main}`, ...(checks ? ['--checks'] : [])];
    const wasi = new WASI(args, [], fds, { debug: false });
    const instance = await WebAssembly.instantiate(module, { wasi_snapshot_preview1: wasi.wasiImport });
    const started = performance.now();
    let exit = 0;
    try {
      exit = wasi.start(instance);
    } catch (error) {
      if (error instanceof WASIProcExit) exit = error.code;
      else throw error; // a trap: an engine panic, or out of memory
    }
    const wall = performance.now() - started;
    const read = (name) => out.dir.contents.get(name)?.data;
    const resultBytes = read('result.json');
    const result = resultBytes
      ? JSON.parse(new TextDecoder().decode(resultBytes))
      : { ok: false, stage: 'engine', diagnostics: [{ stage: 'engine', severity: 'error', message: `the engine stopped (exit ${exit}) without a result`, line: null }] };
    result.wall_ms = Math.round(wall);
    result.log = log;
    const stl = read('model.stl');
    const copy = stl ? stl.slice() : null;
    self.postMessage({ id, result, stl: copy }, copy ? [copy.buffer] : []);
  } catch (error) {
    const message = String(error?.message || error);
    self.postMessage({
      id,
      result: {
        ok: false, stage: 'engine', log,
        diagnostics: [{ stage: 'engine', severity: 'error', message: `the engine crashed: ${message}`, line: null }],
      },
      stl: null,
    });
  }
};
