.PHONY: *

dev:
	clj -m figwheel.main --build dev --repl

build:
	clj -m figwheel.main --build-once prod

serve:
	clj -M:serve :port 6060 :dir "resources/public"
