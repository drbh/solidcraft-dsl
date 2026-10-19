# Editor support for `.sc` files

Syntax highlighting for the SolidCraft design language (see `../README.md`, "The language").
Nothing here is installed automatically.

## VS Code (`vscode/`)

A minimal extension: language id `solidcraft`, extension `.sc`, TextMate scope `source.solidcraft`.
Either copy or symlink the folder into the extensions directory and restart VS Code:

```sh
ln -s "$(pwd)/vscode" ~/.vscode/extensions/solidcraft-dsl
```

or, with `vsce` installed, package and install it:

```sh
cd vscode && vsce package && code --install-extension solidcraft-dsl-0.1.0.vsix
```

The grammar uses standard TextMate scopes (`keyword.control`, `storage.type`, `support.function`,
`storage.modifier`, `support.type.property-name`, `constant.language`, `constant.numeric`,
`keyword.other.unit`, `entity.name.*`, `variable.other.*`, `string`, `comment`), so any theme
colours it without configuration.

## Vim / Neovim (`vim/`)

Copy `syntax/` and `ftdetect/` into your runtime path:

```sh
cp -r vim/syntax vim/ftdetect ~/.vim/                 # Vim
cp -r vim/syntax vim/ftdetect ~/.config/nvim/         # Neovim
```

or add this directory to the runtime path (`set runtimepath+=/path/to/solidcraft-dsl/editors/vim`).
`*.sc` then gets `filetype=solidcraft`; use `:setfiletype solidcraft` for other names.
