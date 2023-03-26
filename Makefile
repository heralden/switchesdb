.PHONY: *

prepare:
	clj -M:prepare

dev:
	clj -m figwheel.main --build dev --repl

build:
	rm -rf resources/public/js/out
	clj -m figwheel.main --build-once prod

serve:
	clj -M:serve :port 6060 :dir "resources/public"
