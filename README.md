# cljpyoung.landoflisp

- use [bun](https://bun.sh/)

``` sh
  $ bun run watch
    npx shadow-cljs watch app

  $ bun run clean
    rimraf resources/public/js

  $ bun run build
    npm run clean && npx shadow-cljs release :app
```