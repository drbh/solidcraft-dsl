// Turns one model (a gzipped GLB) into GLB bytes, off the main thread: `text` is base64 from the
// one-file page, `buffer` the bytes as fetched from the hosted site. Bytes that aren't gzip
// (a server that already decoded them) are passed through.
const isGzip = (b) => b.length > 2 && b[0] === 0x1f && b[1] === 0x8b;

self.onmessage = async ({ data: { id, text, buffer } }) => {
  try {
    const bytes = buffer ? new Uint8Array(buffer) : Uint8Array.from(atob(text.trim()), (c) => c.charCodeAt(0));
    let out = bytes.buffer;
    if (isGzip(bytes)) {
      const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream('gzip'));
      out = await new Response(stream).arrayBuffer();
    }
    self.postMessage({ id, buffer: out }, [out]);
  } catch (error) {
    self.postMessage({ id, error: String(error?.message || error) });
  }
};
