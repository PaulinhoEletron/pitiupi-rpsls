# RPSLS plugin for Pitiupi
#
# Official way (same as the other plugins in the repository): NetBeans or
#     cd plugins/rpsls && ant jar        -> dist/pitiupi.rpsls.jar
# and the "rpsls" entry in plugins.registry.json, so scripts/pitiupi.sh builds
# and installs it together with the other plugins.
#
# make shortcuts (no ant needed):
#   make          -> builds the core (../../pitiupi/dist/pitiupi.jar) and the plugin (dist/pitiupi.rpsls.jar)
#   make test     -> tests (rules, protocol, state machine)
#   make install  -> copies the plugin into ../../.pitiupi/plugins/rpsls.jar
#   make run      -> install + starts Pitiupi
#   make kit      -> dist/rpsls-plugin.zip (pitiupi.rpsls.jar + install guides EN/PT)
#   make kit-full -> dist/pitiupi-com-rpsls.zip (plan B: prebuilt Pitiupi + plugin)
#   make up / down / logs -> 2 players in containers (local test)
#   make clean
#
# Note: "scripts/pitiupi.sh run" recreates .pitiupi/plugins from
# plugins.registry.json, so "make install" is only for quick local runs.

REPO_ROOT = ../..
CORE_DIR  = $(REPO_ROOT)/pitiupi
CORE_SRC  = $(CORE_DIR)/src
CORE_OUT  = $(CORE_DIR)/build/make-classes
CORE_JAR  = $(CORE_DIR)/dist/pitiupi.jar
PITIUPI_HOME ?= $(REPO_ROOT)/.pitiupi
PLUGINS   = $(PITIUPI_HOME)/plugins

OUT_DIR   = build/make-classes
JAR_NAME  = pitiupi.rpsls.jar
DIST      = dist/$(JAR_NAME)
SOURCES   = $(shell find src -name "*.java")

JAVAC     = javac --release 17 -encoding UTF-8

# copies images and other non-Java resources from src/ into a classes folder
RESOURCES = (cd src && find . -type f ! -name "*.java" ! -path "./META-INF/*" | tar -cf - -T -) | tar -C

.PHONY: all core install test kit kit-full kit-completo run up down logs clean

all: $(DIST)

$(CORE_JAR): $(shell find $(CORE_SRC) -name "*.java")
	rm -rf "$(CORE_OUT)" && mkdir -p "$(CORE_OUT)" "$(dir $(CORE_JAR))"
	$(JAVAC) -nowarn -d "$(CORE_OUT)" $(shell find $(CORE_SRC) -name "*.java")
	jar cfe "$(CORE_JAR)" pitiupi.Main -C "$(CORE_OUT)" .

core: $(CORE_JAR)

$(DIST): $(CORE_JAR) $(SOURCES)
	rm -rf "$(OUT_DIR)" && mkdir -p "$(OUT_DIR)" "$(dir $(DIST))"
	$(JAVAC) -cp "$(CORE_JAR)" -d "$(OUT_DIR)" $(SOURCES)
	cp -r src/META-INF "$(OUT_DIR)"
	$(RESOURCES) "$(OUT_DIR)" -xf -
	jar cf "$(DIST)" -C "$(OUT_DIR)" .

install: $(DIST)
	mkdir -p "$(PLUGINS)"
	cp "$(DIST)" "$(PLUGINS)/rpsls.jar"
	@echo "Plugin installed in $(PLUGINS)/rpsls.jar"

run: install
	PITIUPI_HOME="$(abspath $(PITIUPI_HOME))" java -Djava.net.preferIPv4Stack=true -jar "$(CORE_JAR)"

TEST_OUT  = build/test-classes
TESTS     = $(shell find test -name "*.java")

test: $(CORE_JAR) $(SOURCES) $(TESTS)
	rm -rf "$(TEST_OUT)" && mkdir -p "$(TEST_OUT)"
	$(JAVAC) -cp "$(CORE_JAR)" -d "$(TEST_OUT)" $(SOURCES) $(TESTS)
	$(RESOURCES) "$(TEST_OUT)" -xf -
	java -cp "$(TEST_OUT):$(CORE_JAR)" pitiupi.rpsls.RpslsTests

KIT_DIR   = dist/rpsls-plugin
FULL_DIR  = dist/pitiupi-com-rpsls

# Main kit: just the plugin, to install into an existing Pitiupi.
kit: $(DIST)
	rm -rf "$(KIT_DIR)" && mkdir -p "$(KIT_DIR)"
	cp "$(DIST)" kit/HOW-TO-INSTALL.txt kit/COMO-INSTALAR.txt "$(KIT_DIR)/"
	cd dist && rm -f rpsls-plugin.zip && zip -qr rpsls-plugin.zip rpsls-plugin
	@echo "Kit ready: dist/rpsls-plugin.zip"

# Plan B: prebuilt Pitiupi + plugin, for someone without a working Pitiupi.
kit-completo: kit-full
kit-full: $(DIST)
	rm -rf "$(FULL_DIR)" && mkdir -p "$(FULL_DIR)/plugins"
	cp "$(CORE_JAR)" "$(FULL_DIR)/pitiupi.jar"
	cp "$(DIST)" "$(FULL_DIR)/plugins/rpsls.jar"
	cp kit/completo/jogar.sh kit/completo/jogar.bat kit/completo/README.txt kit/completo/LEIA-ME.txt "$(FULL_DIR)/"
	chmod +x "$(FULL_DIR)/jogar.sh"
	cd dist && rm -f pitiupi-com-rpsls.zip && zip -qr pitiupi-com-rpsls.zip pitiupi-com-rpsls
	@echo "Full kit ready: dist/pitiupi-com-rpsls.zip"

up: $(DIST)
	docker compose -f docker/compose.yml up --build -d
	@echo ""
	@echo "  Alice -> http://localhost:6080/vnc.html"
	@echo "  Bob   -> http://localhost:6081/vnc.html"

down:
	docker compose -f docker/compose.yml down

logs:
	docker compose -f docker/compose.yml logs -f

clean:
	rm -rf build dist "$(CORE_OUT)"
