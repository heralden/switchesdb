# SwitchesDB

A webapp for analysing and comparing keyboard switches based on force-distance measurements from integrated datasets.

Consists of an SPA which only depends on static resources generated at build-time. This means once built, you can host it with any static web server (e.g. nginx).

## Building & development

Requires [java](https://adoptium.net/), [clojure](https://clojure.org/guides/install_clojure) and [babashka](https://github.com/babashka/babashka#installation).

Once after cloning the repository you'll have to populate the submodules using: `git submodule update --init`

**Development:** Use `make dev` or your favourite Clojure REPL integration.


**Building:**
```
make prepare
make build
```

You can now serve the contents of `resources/public`. If you just want to try it out, use `make serve` (not recommended for production deployments).
