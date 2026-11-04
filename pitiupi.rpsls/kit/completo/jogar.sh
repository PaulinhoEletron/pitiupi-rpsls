#!/bin/sh
# Starts Pitiupi with the RPSLS plugin (Linux / macOS). Requires Java 17 or newer.
# Abre o Pitiupi com o plugin RPSLS (Linux / macOS). Requer Java 17 ou mais novo.
cd "$(dirname "$0")" || exit 1
if ! command -v java >/dev/null 2>&1; then
  echo "Java not found / Java não encontrado. Install Java 17+ (https://adoptium.net)."
  exit 1
fi
exec java -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -jar pitiupi.jar
