import { mount } from 'svelte';
import Playground from './Playground.svelte';
import '../app.css';

mount(Playground, { target: document.getElementById('app') });
