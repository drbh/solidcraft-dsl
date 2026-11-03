// Turns one embedded model (base64 of a gzipped GLB) into GLB bytes, off the main thread.
self.onmessage = async ({ data: { id, text } }) => {
  try {
    const bytes = Uint8Array.from(atob(text.trim()), (c) => c.charCodeAt(0));
    const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream('gzip'));
    const buffer = await new Response(stream).arrayBuffer();
    self.postMessage({ id, buffer }, [buffer]);
  } catch (error) {
    self.postMessage({ id, error: String(error?.message || error) });
  }
};
