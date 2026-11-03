import { mount } from 'svelte';
import App from './App.svelte';
import './app.css';
import { tokenCss } from 'virtual:examples';
import { renderThumb } from './lib/viewer.js';

document.head.append(Object.assign(document.createElement('style'), { textContent: tokenCss }));

// For ci/examples-site.py's thumbnail pass (site/scripts/thumbs.mjs).
window.__solidcraftThumb = renderThumb;

mount(App, { target: document.getElementById('app') });
