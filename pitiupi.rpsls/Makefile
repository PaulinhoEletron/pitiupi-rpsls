# RPSLS plugin for Pitiupi
#
# Official way (same as the other plugins in the repository): NetBeans or
#     cd pitiupi.rpsls && ant jar        -> dist/pitiupi.rpsls.jar
#
# make shortcuts (no ant needed):
#   make          -> builds the core (../pitiupi/dist/pitiupi.jar) and the plugin (dist/pitiupi.rpsls.jar)
#   make install  -> creates ../pitiupi/plugins/ and copies the plugin there
#   make run      -> install + starts Pitiupi from the ../pitiupi folder
#   make test     -> tests (rules, protocol, state machine)
#   make kit      -> dist/rpsls-plugin.zip (pitiupi.rpsls.jar + install guides EN/PT)
#   make kit-full -> dist/pitiupi-com-rpsls.zip (plan B: prebuilt Pitiupi + plugin)
#   make up / down / logs -> 2 players in containers (local test)
#   make clean

CORE_DIR  = ../pitiupi
CORE_SRC  = $(CORE_DIR)/src
CORE_OUT  = $(CORE_DIR)/build/make-classes
CORE_JAR  = $(CORE_DIR)/dist/pitiupi.jar
PLUGINS   = $(CORE_DIR)/plugins

OUT_DIR   = build/make-classes
JAR_NAME  = pitiupi.rpsls.jar
DIST      = dist/$(JAR_NAME)
SOURCES   = $(shell find src -name "*.java")
STUB      = pitiupi/plugin

JAVAC     = javac --release 17 -encoding UTF-8

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
	rm -rf "$(OUT_DIR)/$(STUB)"          # compile-only stub is not packaged
	cp -r src/META-INF "$(OUT_DIR)"
	(cd src && find . -type f ! -name "*.java" ! -path "./META-INF/*" | tar -cf - -T -) | tar -C "$(OUT_DIR)" -xf -   # images and other resources
	jar cf "$(DIST)" -C "$(OUT_DIR)" .

# Pitiupi looks for plugins in "plugins/" relative to the folder it is started
# from, i.e. pitiupi/plugins/ (the folder is not in git; it is created here).
install: $(DIST)
	mkdir -p "$(PLUGINS)"
	cp "$(DIST)" "$(PLUGINS)/"
	@echo "Plugin installed in $(PLUGINS)/$(JAR_NAME)"

run: install
	cd "$(CORE_DIR)" && java -Djava.net.preferIPv4Stack=true -jar dist/pitiupi.jar

TEST_OUT  = build/test-classes
TESTS     = $(shell find test -name "*.java")

test: $(CORE_JAR) $(SOURCES) $(TESTS)
	rm -rf "$(TEST_OUT)" && mkdir -p "$(TEST_OUT)"
	$(JAVAC) -cp "$(CORE_JAR)" -d "$(TEST_OUT)" $(SOURCES) $(TESTS)
	rm -rf "$(TEST_OUT)/$(STUB)"
	(cd src && find . -type f ! -name "*.java" ! -path "./META-INF/*" | tar -cf - -T -) | tar -C "$(TEST_OUT)" -xf -   # images and other resources
	java -cp "$(TEST_OUT):$(CORE_JAR)" pitiupi.rpsls.RpslsTests

KIT_DIR   = dist/rpsls-plugin
FULL_DIR  = dist/pitiupi-com-rpsls

# Main kit: just the plugin, to drop into the professor's Pitiupi.
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
	cp "$(DIST)" "$(FULL_DIR)/plugins/"
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
