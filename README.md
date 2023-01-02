# SwitchesDB

A webapp for analysing and comparing keyboard switches based on force-distance measurements from integrated datasets.

Consists of an SPA which only depends on static resources generated at build-time. This means once built, you can host it with any static web server (e.g. nginx).

## Building & development

Requires [java](https://adoptium.net/), [clojure](https://clojure.org/guides/install_clojure) and [babashka](https://github.com/babashka/babashka#installation).

Run this once after cloning to populate the submodules: `git submodule update --init`

**Development:** Use `make dev` or your favourite Clojure REPL integration.


**Building:** The first command will scrape the CSV from HaaTa's plot.ly charts, which can take a while. The *bb* commands only need to be run the first time, and from then on whenever you wish to pull in updates to the datasets.

```
bb haata
bb prepare
make build
```

You can now serve the contents of *resources/public*. If you just want to try it out, use `make serve` (not recommended for production deployments).

## Credits

This wouldn't be possible without the hard work of the dataset authors:
- https://github.com/ThereminGoat/force-curves
- https://plot.ly/~haata
