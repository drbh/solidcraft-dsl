" SolidCraft .sc design files.
" Vim and Neovim already map *.sc to Scala / SuperCollider, so this must override
" (`set filetype=`), not yield (`setfiletype`).
autocmd BufRead,BufNewFile *.sc set filetype=solidcraft
