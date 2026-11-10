#!/bin/sh
# Starts a virtual screen + VNC + noVNC, then Pitiupi with the plugin.
set -e
export DISPLAY=:1

Xvfb :1 -screen 0 1280x800x24 &
sleep 1
fluxbox >/dev/null 2>&1 &
x11vnc -display :1 -forever -shared -nopw -quiet &
websockify --web /usr/share/novnc 6080 localhost:5900 >/dev/null 2>&1 &

echo "== $(hostname) IP: $(hostname -i)"
# Pitiupi loads every *.jar in PITIUPI_HOME/plugins
export PITIUPI_HOME=/app/.pitiupi
mkdir -p "$PITIUPI_HOME/plugins"
cp /jars/pitiupi.rpsls.jar "$PITIUPI_HOME/plugins/rpsls.jar"
cd /app

# If Pitiupi exits, start it again (handy while testing).
while true; do
  java -Djava.net.preferIPv4Stack=true -jar /jars/pitiupi.jar || true
  echo "== Pitiupi exited, restarting in 2s"; sleep 2
done
