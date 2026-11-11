#!/bin/sh
# Starts Jetty the way the official jetty:9.4 image's entrypoint did, so deployments
# moving from that image keep working:
#   - JAVA_OPTIONS is passed to the JVM (e.g. -e JAVA_OPTIONS='-Xmx1433m')
#   - options or key=value pairs passed to this script go to start.jar
#     (e.g. docker run <image> /usr/local/bin/docker-entrypoint.sh jetty.http.port=9090)
#   - any other command runs as is
# The image runs this script as its CMD, so a command given to docker run replaces it.
set -e

case "$1" in
  '' | -* | *=*)
    # java.io.tmpdir keeps JVM/Jetty temp files inside the jetty-owned base dir (the JVM
    # does not honor TMPDIR). JAVA_OPTIONS is unquoted on purpose so it splits into
    # separate JVM arguments, as in the old image.
    # shellcheck disable=SC2086
    exec java -Djava.io.tmpdir="${JETTY_BASE}/tmp" $JAVA_OPTIONS -jar "${JETTY_HOME}/start.jar" "$@"
    ;;
esac
exec "$@"
