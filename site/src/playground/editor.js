// The playground's editor: CodeMirror 6 with a small `.sc` mode. The word lists come from the
// repository's TextMate grammar at build time (see vite.config.js), so the editor and the
// gallery's highlighting agree on what is a keyword, a feature, a key or a modifier.
import { EditorState, StateEffect, StateField, RangeSetBuilder } from '@codemirror/state';
import {
  EditorView, Decoration, keymap, lineNumbers, highlightActiveLine, highlightActiveLineGutter,
  drawSelection, gutter, GutterMarker,
} from '@codemirror/view';
import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands';
import { StreamLanguage, HighlightStyle, syntaxHighlighting, bracketMatching, indentUnit } from '@codemirror/language';
import { tags as t } from '@lezer/highlight';

export function scLanguage(words) {
  const sets = Object.fromEntries(Object.entries(words).map(([k, v]) => [k, new Set(v)]));
  const units = /^(mm|cm|m|in|ft|deg|rad)\b/;
  return StreamLanguage.define({
    name: 'solidcraft',
    token(stream) {
      if (stream.eatSpace()) return null;
      if (stream.match('//')) { stream.skipToEnd(); return 'comment'; }
      if (stream.match(/^"[^"]*"?/)) return 'string';
      if (stream.match(/^-?(?:\d+(?:\.\d+)?|\.\d+)(?:[eE][-+]?\d+)?/)) {
        stream.match(/^\s*/, false);
        const save = stream.pos;
        stream.eatSpace();
        if (!stream.match(units)) stream.pos = save;
        return 'number';
      }
      if (stream.match('->')) return 'operator';
      const word = stream.match(/^[A-Za-z_][A-Za-z0-9_]*/);
      if (word) {
        const w = word[0];
        if (stream.peek() === '.') { // a reference such as o.1.end
          stream.match(/^(?:\.(?:[A-Za-z_][A-Za-z0-9_]*|\d+))+/);
          return 'variableName.special';
        }
        if (sets.keyword.has(w)) return 'keyword';
        if (sets.feature.has(w)) return 'typeName';
        if (sets.key.has(w)) return 'propertyName';
        if (sets.modifier.has(w)) return 'modifier';
        if (sets.constant.has(w)) return 'atom';
        return 'variableName';
      }
      stream.next();
      return /[-+*/=]/.test(stream.string[stream.pos - 1]) ? 'operator' : 'punctuation';
    },
    languageData: { commentTokens: { line: '//' } },
  });
}

// Colours are CSS variables (see app.css), so the editor follows the page theme.
const highlight = HighlightStyle.define([
  { tag: t.comment, color: 'var(--hl-comment)', fontStyle: 'italic' },
  { tag: t.string, color: 'var(--hl-string)' },
  { tag: t.number, color: 'var(--hl-number)' },
  { tag: t.keyword, color: 'var(--hl-keyword)', fontWeight: '600' },
  { tag: t.typeName, color: 'var(--hl-feature)' },
  { tag: t.propertyName, color: 'var(--hl-key)' },
  { tag: t.modifier, color: 'var(--hl-modifier)' },
  { tag: t.atom, color: 'var(--hl-constant)' },
  { tag: t.special(t.variableName), color: 'var(--hl-ref)' },
  { tag: t.operator, color: 'var(--hl-operator)' },
]);

const theme = EditorView.theme({
  '&': { height: '100%', fontSize: '12.5px', backgroundColor: 'var(--code-bg)', color: 'var(--text)' },
  '.cm-scroller': { fontFamily: 'var(--mono)', lineHeight: '1.6' },
  '.cm-content': { caretColor: 'var(--accent)', padding: '10px 0' },
  '.cm-gutters': { backgroundColor: 'var(--code-bg)', color: 'var(--muted)', border: 'none' },
  '.cm-activeLine': { backgroundColor: 'color-mix(in srgb, var(--accent) 6%, transparent)' },
  '.cm-activeLineGutter': { backgroundColor: 'transparent', color: 'var(--text)' },
  '&.cm-focused': { outline: 'none' },
  '&.cm-focused .cm-selectionBackground, .cm-selectionBackground, ::selection': {
    backgroundColor: 'color-mix(in srgb, var(--accent) 22%, transparent) !important',
  },
  '.cm-cursor': { borderLeftColor: 'var(--accent)' },
  '.cm-line.diag-error': { backgroundColor: 'color-mix(in srgb, var(--danger) 12%, transparent)' },
  '.cm-line.diag-warning': { backgroundColor: 'color-mix(in srgb, #d29922 14%, transparent)' },
  '.cm-diag-gutter': { width: '10px' },
  '.diag-dot': { display: 'inline-block', width: '7px', height: '7px', borderRadius: '50%', marginTop: '7px' },
  '.diag-dot.error': { backgroundColor: 'var(--danger)' },
  '.diag-dot.warning': { backgroundColor: '#d29922' },
});

// Diagnostics: a line background plus a gutter dot, replaced wholesale after every build.
const setDiagnostics = StateEffect.define();

class Dot extends GutterMarker {
  constructor(severity, message) { super(); this.severity = severity; this.message = message; }
  eq(other) { return other.severity === this.severity && other.message === this.message; }
  toDOM() {
    const el = document.createElement('span');
    el.className = `diag-dot ${this.severity}`;
    el.title = this.message;
    return el;
  }
}

const diagnosticsField = StateField.define({
  create: () => ({ lines: Decoration.none, dots: [] }),
  update(value, tr) {
    for (const effect of tr.effects) if (effect.is(setDiagnostics)) return effect.value;
    if (!tr.docChanged) return value;
    return { lines: value.lines.map(tr.changes), dots: [] };
  },
  provide: (field) => EditorView.decorations.from(field, (v) => v.lines),
});

function buildDiagnostics(doc, list) {
  const byLine = new Map();
  for (const d of list) {
    if (!d.line || d.line > doc.lines) continue;
    const prev = byLine.get(d.line);
    if (!prev || (prev.severity !== 'error' && d.severity === 'error')) byLine.set(d.line, d);
  }
  const builder = new RangeSetBuilder();
  const dots = [];
  for (const n of [...byLine.keys()].sort((a, b) => a - b)) {
    const d = byLine.get(n);
    const line = doc.line(n);
    builder.add(line.from, line.from, Decoration.line({ class: `diag-${d.severity}` }));
    dots.push({ from: line.from, marker: new Dot(d.severity, d.message) });
  }
  return { lines: builder.finish(), dots };
}

const diagnosticGutter = gutter({
  class: 'cm-diag-gutter',
  markers: (view) => {
    const builder = new RangeSetBuilder();
    for (const d of view.state.field(diagnosticsField).dots) builder.add(d.from, d.from, d.marker);
    return builder.finish();
  },
});

/**
 * Create the editor in `parent`. `onChange(text)` fires on every edit; `onRun()` on Mod-Enter,
 * `onCheck()` on Shift-Mod-Enter.
 */
export function createEditor(parent, { words, doc = '', onChange, onRun, onCheck }) {
  const language = scLanguage(words);
  const extensions = [
    lineNumbers(),
    diagnosticGutter,
    highlightActiveLineGutter(),
    history(),
    drawSelection(),
    bracketMatching(),
    highlightActiveLine(),
    EditorView.lineWrapping,
    indentUnit.of('  '),
    EditorState.tabSize.of(2),
    language,
    syntaxHighlighting(highlight),
    diagnosticsField,
    theme,
    keymap.of([
      { key: 'Mod-Enter', run: () => (onRun?.(), true) },
      { key: 'Shift-Mod-Enter', run: () => (onCheck?.(), true) },
      indentWithTab,
      ...defaultKeymap,
      ...historyKeymap,
    ]),
    EditorView.updateListener.of((u) => u.docChanged && onChange?.(u.state.doc.toString())),
  ];
  const view = new EditorView({ parent, state: EditorState.create({ doc, extensions }) });
  return {
    view,
    text: () => view.state.doc.toString(),
    /** Replace the whole document (switching files or examples): a fresh history. */
    load(text) {
      view.setState(EditorState.create({ doc: text, extensions }));
    },
    /** Replace one line's text as an edit, so it can be undone. */
    replaceLine(n, text) {
      const line = view.state.doc.line(n);
      view.dispatch({ changes: { from: line.from, to: line.to, insert: text } });
    },
    setDiagnostics(list) {
      view.dispatch({ effects: setDiagnostics.of(buildDiagnostics(view.state.doc, list)) });
    },
    goto(n) {
      if (!n || n > view.state.doc.lines) return;
      const line = view.state.doc.line(n);
      view.dispatch({ selection: { anchor: line.from }, effects: EditorView.scrollIntoView(line.from, { y: 'center' }) });
      view.focus();
    },
    destroy: () => view.destroy(),
  };
}
