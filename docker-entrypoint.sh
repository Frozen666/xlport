#!/bin/sh
# Starts Jetty the way the official jetty:9.4 image's entrypoint did, so deployments
# moving from that image keep working:
#   - JAVA_OPTIONS is passed to the JVM (e.g. -e JAVA_OPTIONS='-Xmx1433m')
#   - arguments that are options or key=value pairs go to start.jar
#     (e.g. docker run <image> jetty.http.port=9090)
#   - any other command runs as is (e.g. docker run -it <image> sh)
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
