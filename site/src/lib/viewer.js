// One WebGL context for the whole page. Every example's viewport is a transparent DOM element; a
// single fixed canvas sits under the page content and each frame draws the visible examples into
// their element's rectangle with the scissor test, so anything painted above (buttons, sticky
// headers) covers the model as it should. Browsers cap live WebGL contexts at ~16, and
// this page has more examples than that.
//
// Models are decoded only when their element comes near the screen and released again when it is
// far away, so GPU memory tracks what is on screen rather than the whole page.
import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';

const loader = new GLTFLoader();
const views = new Set();
let renderer = null;
let dirty = true;
const edgeColor = new THREE.Color();
const modelColor = new THREE.Color();
const backColor = new THREE.Color();

// Front-biased three-quarter view (glTF is Y up, +Z is the design's front, -Y): 20 deg round from
// the front and 28 deg up, so models built along 45 deg diagonals don't land edge-on.
const VIEW_DIR = new THREE.Vector3(0.3, 0.47, 0.83).normalize();
const EDGE_TRIANGLE_LIMIT = 600_000;

function readColors() {
  const style = getComputedStyle(document.documentElement);
  modelColor.set(style.getPropertyValue('--model').trim() || '#9aa7b8');
  edgeColor.set(style.getPropertyValue('--model-edge').trim() || '#1f2933');
  backColor.set(style.getPropertyValue('--viewport').trim() || '#eef1f5');
  for (const view of views) view.applyColors();
  dirty = true;
}

function ensureRenderer() {
  if (renderer) return;
  const canvas = document.createElement('canvas');
  canvas.className = 'gl-underlay';
  document.body.appendChild(canvas);
  renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.setClearColor(0x000000, 0);
  readColors();
  matchMedia('(prefers-color-scheme: dark)').addEventListener('change', readColors);
  new MutationObserver(readColors).observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme'],
  });
  const touch = () => (dirty = true);
  window.addEventListener('scroll', touch, { passive: true, capture: true });
  window.addEventListener('resize', touch);
  requestAnimationFrame(frame);
}

function frame() {
  requestAnimationFrame(frame);
  if (!dirty) return;
  dirty = false;
  const canvas = renderer.domElement;
  const width = window.innerWidth;
  const height = window.innerHeight;
  if (canvas.width !== Math.floor(width * renderer.getPixelRatio()) ||
      canvas.height !== Math.floor(height * renderer.getPixelRatio())) {
    renderer.setSize(width, height, false);
  }
  renderer.setScissorTest(false);
  renderer.setClearColor(0x000000, 0);
  renderer.clear();
  renderer.setScissorTest(true);
  for (const view of views) {
    if (!view.model) continue;
    const r = view.el.getBoundingClientRect();
    if (r.bottom < 0 || r.top > height || r.right < 0 || r.left > width) continue;
    const bottom = height - r.bottom;
    renderer.setViewport(r.left, bottom, r.width, r.height);
    renderer.setScissor(r.left, bottom, r.width, r.height);
    renderer.setClearColor(backColor, 1);
    renderer.clear();
    if (view.camera.aspect !== r.width / r.height) {
      view.camera.aspect = r.width / r.height;
      view.camera.updateProjectionMatrix();
    }
    renderer.render(view.scene, view.camera);
  }
}

async function decode(name) {
  const tag = document.getElementById(`glb-${name}`);
  if (!tag) throw new Error('model missing from page');
  const response = await fetch(`data:application/gzip;base64,${tag.textContent.trim()}`);
  const stream = response.body.pipeThrough(new DecompressionStream('gzip'));
  return new Response(stream).arrayBuffer();
}

export async function glbBlob(name) {
  return new Blob([await decode(name)], { type: 'model/gltf-binary' });
}

class View {
  constructor(el, name, { onState }) {
    this.el = el;
    this.name = name;
    this.onState = onState;
    this.model = null;
    this.loading = null;
    this.showEdges = true;
    this.scene = new THREE.Scene();
    this.camera = new THREE.PerspectiveCamera(30, 1, 0.0001, 100);
    // Lights ride on the camera so every side of the model is lit the same way while orbiting.
    this.scene.add(new THREE.HemisphereLight(0xffffff, 0x9aa0aa, 1.9));
    const key = new THREE.DirectionalLight(0xffffff, 2.2);
    key.position.set(-0.5, 1, 0.8);
    const fill = new THREE.DirectionalLight(0xffffff, 0.8);
    fill.position.set(1, -0.3, 0.5);
    this.camera.add(key, fill);
    this.scene.add(this.camera);

    this.controls = new OrbitControls(this.camera, el);
    this.controls.enableZoom = false; // page scroll wins until the model is clicked
    this.controls.addEventListener('change', () => (dirty = true));
    el.addEventListener('pointerdown', () => (this.controls.enableZoom = true));
    el.addEventListener('pointerleave', () => (this.controls.enableZoom = false));
    el.addEventListener('dblclick', () => this.frame());

    this.near = new IntersectionObserver(
      ([entry]) => entry.isIntersecting && this.load(),
      { rootMargin: '100% 0px' },
    );
    this.far = new IntersectionObserver(
      ([entry]) => !entry.isIntersecting && this.unload(),
      { rootMargin: '400% 0px' },
    );
    this.near.observe(el);
    this.far.observe(el);
  }

  async load() {
    if (this.model || this.loading) return;
    this.onState('loading');
    this.loading = (async () => {
      const buffer = await decode(this.name);
      const gltf = await loader.parseAsync(buffer, '');
      const model = gltf.scene;
      this.material = new THREE.MeshStandardMaterial({
        color: modelColor, roughness: 0.55, metalness: 0.05,
        polygonOffset: true, polygonOffsetFactor: 1, polygonOffsetUnits: 1,
      });
      this.edgeMaterial = new THREE.LineBasicMaterial({
        color: edgeColor, transparent: true, opacity: 0.65,
      });
      const meshes = [];
      model.traverse((o) => o.isMesh && meshes.push(o));
      for (const mesh of meshes) {
        mesh.material = this.material;
        const tris = (mesh.geometry.index?.count ?? mesh.geometry.attributes.position.count) / 3;
        if (tris <= EDGE_TRIANGLE_LIMIT) {
          const edges = new THREE.LineSegments(
            new THREE.EdgesGeometry(mesh.geometry, 25), this.edgeMaterial);
          edges.name = 'edges';
          mesh.add(edges);
        }
      }
      return model;
    })();
    try {
      const model = await this.loading;
      if (!views.has(this)) return dispose(model);
      this.model = model;
      this.scene.add(model);
      this.setEdges(this.showEdges);
      if (!this.framed) this.frame();
      this.framed = true;
      this.onState('ready');
    } catch (error) {
      console.error(this.name, error);
      this.onState('error', String(error.message || error));
    } finally {
      this.loading = null;
      dirty = true;
    }
  }

  unload() {
    if (!this.model) return;
    this.scene.remove(this.model);
    dispose(this.model);
    this.material?.dispose();
    this.edgeMaterial?.dispose();
    this.model = null;
    this.onState('idle');
  }

  frame() {
    if (!this.model) return;
    const box = new THREE.Box3().setFromObject(this.model);
    const sphere = box.getBoundingSphere(new THREE.Sphere());
    const fov = THREE.MathUtils.degToRad(this.camera.fov);
    const distance = (sphere.radius / Math.sin(fov / 2)) * 1.05;
    this.camera.position.copy(sphere.center).addScaledVector(VIEW_DIR, distance);
    this.camera.near = distance / 100;
    this.camera.far = distance * 100;
    this.camera.updateProjectionMatrix();
    this.controls.target.copy(sphere.center);
    this.controls.update();
    dirty = true;
  }

  setEdges(on) {
    this.showEdges = on;
    this.model?.traverse((o) => o.name === 'edges' && (o.visible = on));
    dirty = true;
  }

  applyColors() {
    this.material?.color.copy(modelColor);
    this.edgeMaterial?.color.copy(edgeColor);
  }

  destroy() {
    views.delete(this);
    this.near.disconnect();
    this.far.disconnect();
    this.controls.dispose();
    this.unload();
  }
}

function dispose(object) {
  object.traverse((o) => o.geometry?.dispose());
}

export function createView(el, name, options) {
  ensureRenderer();
  const view = new View(el, name, options);
  views.add(view);
  return view;
}
