import { mount } from 'svelte';
import App from './App.svelte';
import './app.css';
import { tokenCss } from 'virtual:examples';

document.head.append(Object.assign(document.createElement('style'), { textContent: tokenCss }));

mount(App, { target: document.getElementById('app') });
