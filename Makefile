.PHONY: *

prepare:
	clj -M:prepare

update:
	rm -rf resources/public/data
	cp -R resources/data resources/public/data

dev:
	clj -m figwheel.main --build dev --repl

build:
	rm -rf resources/public/js/out
	clj -m figwheel.main --build-once prod

serve:
	clj -M:serve :port 6060 :dir "resources/public"
