# Image of one "player": Java + a virtual screen (Xvfb) reachable from the browser (noVNC).
# The .jar files are NOT baked into the image: they are mounted as volumes, so
# just rebuild on the host (make) and restart the containers (make up).
FROM eclipse-temurin:21-jre
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8

RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        xvfb x11vnc fluxbox novnc websockify fonts-dejavu iproute2 tcpdump \
 && rm -rf /var/lib/apt/lists/*

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

WORKDIR /app
EXPOSE 6080
ENTRYPOINT ["/entrypoint.sh"]
