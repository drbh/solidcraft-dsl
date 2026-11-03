// The model viewer. Only one example is open at a time, so there is one WebGL renderer for the
// page; its canvas moves into whichever viewport element is showing. Models are embedded in the
// page as gzipped, meshopt-compressed GLBs (see ci/examples-site.py) and are decoded only when an
// example is opened: the gunzip runs in a worker, meshopt decoding in the decoder's own workers.
// The GLBs carry positions only, so smooth normals are rebuilt here with a crease angle: curved
// faces shade smoothly while CAD edges stay sharp.
import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import { MeshoptDecoder } from 'three/examples/jsm/libs/meshopt_decoder.module.js';
import { mergeVertices, toCreasedNormals } from 'three/examples/jsm/utils/BufferGeometryUtils.js';
import GunzipWorker from './gunzip.worker.js?worker&inline';

// Front-biased three-quarter view (glTF is Y up, +Z is the design's front, -Y): 20 deg round from
// the front and 28 deg up, so models built along 45 deg diagonals don't land edge-on.
const VIEW_DIR = new THREE.Vector3(0.3, 0.47, 0.83).normalize();
const CREASE = THREE.MathUtils.degToRad(30);
const EDGE_ANGLE = 25;
const EDGE_TRIANGLE_LIMIT = 600_000;

MeshoptDecoder.useWorkers?.(2);
const loader = new GLTFLoader().setMeshoptDecoder(MeshoptDecoder);

// ---- Decoding: base64 gzip in a <script type="text/plain"> tag -> GLB bytes, off the main thread.
let worker = null;
let nextJob = 0;
const jobs = new Map();

function gunzip(text, buffer) {
  if (!worker) {
    worker = new GunzipWorker();
    worker.onmessage = ({ data }) => {
      const job = jobs.get(data.id);
      jobs.delete(data.id);
      if (data.error) job.reject(new Error(data.error));
      else job.resolve(data.buffer);
    };
  }
  return new Promise((resolve, reject) => {
    const id = nextJob++;
    jobs.set(id, { resolve, reject });
    worker.postMessage({ id, text, buffer }, buffer ? [buffer] : []);
  });
}

/// GLB bytes for an example: fetched from `url` on the hosted site, else from its inline tag.
export async function glbBytes(name, url = null) {
  if (url) {
    const response = await fetch(url);
    if (!response.ok) throw new Error(`${url}: ${response.status}`);
    return gunzip(null, await response.arrayBuffer());
  }
  const tag = document.getElementById(`glb-${name}`);
  if (!tag) return Promise.reject(new Error('model missing from page'));
  return gunzip(tag.textContent);
}

export async function glbBlob(name, url = null) {
  return new Blob([await glbBytes(name, url)], { type: 'model/gltf-binary' });
}

export async function loadModel(name, url = null) {
  const gltf = await loader.parseAsync(await glbBytes(name, url), '');
  const meshes = [];
  gltf.scene.traverse((o) => o.isMesh && meshes.push(o));
  for (const mesh of meshes) prepareMesh(mesh);
  return gltf.scene;
}

/// A model from the engine's binary STL (millimetres, Z up), placed as the GLBs are: glTF's Y up,
/// metres, (x, y, z) -> (x, z, -y) / 1000.
export function modelFromStl(bytes) {
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  const count = view.getUint32(80, true);
  if (bytes.byteLength < 84 + count * 50) throw new Error('truncated STL');
  const positions = new Float32Array(count * 9);
  for (let t = 0; t < count; t++) {
    for (let v = 0; v < 3; v++) {
      const at = 84 + t * 50 + 12 + v * 12;
      const o = t * 9 + v * 3;
      positions[o] = view.getFloat32(at, true) / 1000;
      positions[o + 1] = view.getFloat32(at + 8, true) / 1000;
      positions[o + 2] = -view.getFloat32(at + 4, true) / 1000;
    }
  }
  let geometry = new THREE.BufferGeometry();
  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
  geometry = mergeVertices(geometry, 1e-7);
  const mesh = new THREE.Mesh(geometry);
  prepareMesh(mesh);
  const group = new THREE.Group();
  group.add(mesh);
  return group;
}

function prepareMesh(mesh) {
  let geometry = mesh.geometry;
  // Meshopt leaves positions quantized (integers plus a node scale); creasing needs floats.
  if (geometry.attributes.position.normalized || !(geometry.attributes.position.array instanceof Float32Array)) {
    const p = geometry.attributes.position;
    const floats = new Float32Array(p.count * 3);
    for (let i = 0; i < p.count; i++) {
      floats[i * 3] = p.getX(i); floats[i * 3 + 1] = p.getY(i); floats[i * 3 + 2] = p.getZ(i);
    }
    geometry.setAttribute('position', new THREE.BufferAttribute(floats, 3));
  }
  const tris = (geometry.index?.count ?? geometry.attributes.position.count) / 3;
  const edges = tris <= EDGE_TRIANGLE_LIMIT ? new THREE.EdgesGeometry(geometry, EDGE_ANGLE) : null;
  if (!geometry.attributes.normal) {
    const creased = toCreasedNormals(geometry, CREASE);
    geometry.dispose();
    geometry = creased;
  }
  mesh.geometry = geometry;
  if (edges) {
    const lines = new THREE.LineSegments(edges);
    lines.name = 'edges';
    mesh.add(lines);
  }
}

function dispose(object) {
  object.traverse((o) => o.geometry?.dispose());
}

// ---- Colours follow the page theme.
function themeColors() {
  const style = getComputedStyle(document.documentElement);
  const read = (name, fallback) => new THREE.Color(style.getPropertyValue(name).trim() || fallback);
  return {
    model: read('--model', '#a9b6c8'),
    edge: read('--model-edge', '#27303b'),
    back: read('--viewport', '#eef1f5'),
  };
}

function makeMaterials(colors) {
  return {
    surface: new THREE.MeshStandardMaterial({
      color: colors.model, roughness: 0.55, metalness: 0.05,
      polygonOffset: true, polygonOffsetFactor: 1, polygonOffsetUnits: 1,
    }),
    edge: new THREE.LineBasicMaterial({ color: colors.edge, transparent: true, opacity: 0.65 }),
  };
}

function applyMaterials(model, materials) {
  model.traverse((o) => {
    if (o.isMesh) o.material = materials.surface;
    else if (o.isLineSegments) o.material = materials.edge;
  });
}

function makeScene(camera) {
  const scene = new THREE.Scene();
  // Lights ride on the camera so every side of the model is lit the same way while orbiting.
  scene.add(new THREE.HemisphereLight(0xffffff, 0x9aa0aa, 1.9));
  const key = new THREE.DirectionalLight(0xffffff, 2.2);
  key.position.set(-0.5, 1, 0.8);
  const fill = new THREE.DirectionalLight(0xffffff, 0.8);
  fill.position.set(1, -0.3, 0.5);
  camera.add(key, fill);
  scene.add(camera);
  return scene;
}

function frameCamera(camera, model, controls) {
  const box = new THREE.Box3().setFromObject(model);
  const sphere = box.getBoundingSphere(new THREE.Sphere());
  const fov = THREE.MathUtils.degToRad(camera.fov);
  const distance = (sphere.radius / Math.sin(fov / 2)) * 1.05;
  camera.position.copy(sphere.center).addScaledVector(VIEW_DIR, distance);
  camera.near = distance / 100;
  camera.far = distance * 100;
  camera.updateProjectionMatrix();
  if (controls) {
    controls.target.copy(sphere.center);
    controls.update();
  } else {
    camera.lookAt(sphere.center);
  }
}

// ---- The interactive view.
let renderer = null;

function sharedRenderer() {
  if (!renderer) {
    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.domElement.className = 'gl';
  }
  return renderer;
}

/// A live view in `el`. `show(promise)` puts a model in it: the first is framed, later ones keep
/// the camera (so a rebuilt model stays where you were looking) unless `{ frame: true }`.
export function createView(el, { onState = () => {} } = {}) {
  const gl = sharedRenderer();
  el.prepend(gl.domElement);
  const camera = new THREE.PerspectiveCamera(30, 1, 0.0001, 100);
  const scene = makeScene(camera);
  const controls = new OrbitControls(camera, gl.domElement);
  let model = null;
  let materials = makeMaterials(themeColors());
  let showEdges = true;
  let alive = true;
  let queued = false;

  const render = () => {
    if (queued || !alive) return;
    queued = true;
    requestAnimationFrame(() => {
      queued = false;
      if (!alive) return;
      const { width, height } = el.getBoundingClientRect();
      if (!width || !height) return;
      gl.setSize(width, height, false);
      camera.aspect = width / height;
      camera.updateProjectionMatrix();
      gl.setClearColor(themeColors().back, 1);
      gl.render(scene, camera);
    });
  };

  const recolor = () => {
    const colors = themeColors();
    materials.surface.color.copy(colors.model);
    materials.edge.color.copy(colors.edge);
    render();
  };
  const scheme = matchMedia('(prefers-color-scheme: dark)');
  scheme.addEventListener('change', recolor);
  const themeWatch = new MutationObserver(recolor);
  themeWatch.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
  const resize = new ResizeObserver(render);
  resize.observe(el);
  controls.addEventListener('change', render);
  const reset = () => model && (frameCamera(camera, model, controls), render());
  gl.domElement.addEventListener('dblclick', reset);

  let shown = 0;
  let framed = false;
  function show(promise, { frame = false } = {}) {
    const ticket = ++shown;
    onState('loading');
    return Promise.resolve(promise).then(
      (m) => {
        if (!alive || ticket !== shown) return dispose(m);
        if (model) {
          scene.remove(model);
          dispose(model);
        }
        model = m;
        applyMaterials(model, materials);
        model.traverse((o) => o.name === 'edges' && (o.visible = showEdges));
        scene.add(model);
        if (frame || !framed) frameCamera(camera, model, controls);
        framed = true;
        onState('ready');
        render();
      },
      (error) => {
        console.error(error);
        if (alive && ticket === shown) onState('error', String(error.message || error));
      },
    );
  }

  /** Empty the view (switching designs); the next model shown is framed afresh. */
  function clear() {
    shown++;
    if (model) {
      scene.remove(model);
      dispose(model);
      model = null;
    }
    framed = false;
    onState('idle');
    render();
  }

  return {
    show,
    clear,
    frame: reset,
    setEdges(on) {
      showEdges = on;
      model?.traverse((o) => o.name === 'edges' && (o.visible = on));
      render();
    },
    destroy() {
      alive = false;
      resize.disconnect();
      themeWatch.disconnect();
      scheme.removeEventListener('change', recolor);
      gl.domElement.removeEventListener('dblclick', reset);
      controls.dispose();
      if (model) dispose(model);
      materials.surface.dispose();
      materials.edge.dispose();
      gl.domElement.remove();
    },
  };
}

// ---- Thumbnails, rendered by ci/examples-site.py through a headless browser at build time.
// Transparent background and fixed mid-tone colours, so one image reads on light and dark pages.
export async function renderThumb(name, width = 480, height = 360) {
  const gl = new THREE.WebGLRenderer({ antialias: true, alpha: true, preserveDrawingBuffer: true });
  gl.setPixelRatio(1);
  gl.setSize(width, height, false);
  gl.setClearColor(0x000000, 0);
  const camera = new THREE.PerspectiveCamera(30, width / height, 0.0001, 100);
  const scene = makeScene(camera);
  const model = await loadModel(name);
  const materials = makeMaterials({
    model: new THREE.Color('#a3b0c2'), edge: new THREE.Color('#27303b'),
  });
  applyMaterials(model, materials);
  scene.add(model);
  frameCamera(camera, model);
  gl.render(scene, camera);
  const url = gl.domElement.toDataURL('image/webp', 0.82);
  dispose(model);
  materials.surface.dispose();
  materials.edge.dispose();
  gl.dispose();
  gl.forceContextLoss();
  return url;
}
