# Multi-stage build: the war is built inside Docker, so no local Maven setup is needed.
# Build:  docker build -t xlport/xlport-repo:v6 .
# For customer delivery on x86_64 servers, build with: docker build --platform=linux/amd64 ...

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B clean package

# Runtime: Ubuntu 26.04 LTS + distro OpenJDK 25 JRE (LTS, patched via Ubuntu security updates)
# + Jetty 12.1 (ee10 environment for the jakarta.servlet webapp).
FROM ubuntu:26.04

ARG JETTY_VERSION=12.1.11
ENV JETTY_HOME=/opt/jetty-home \
    JETTY_BASE=/opt/jetty-base

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends openjdk-25-jre-headless curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    # pebble ships in the ubuntu:26.04 OCI image but is unused here; remove it so its
    # bundled Go dependencies don't show up in vulnerability scans
    && rm -f /usr/bin/pebble

RUN curl -fsSL "https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-home/${JETTY_VERSION}/jetty-home-${JETTY_VERSION}.tar.gz" -o /tmp/jetty.tgz \
    && tar -xzf /tmp/jetty.tgz -C /opt \
    && mv "/opt/jetty-home-${JETTY_VERSION}" "${JETTY_HOME}" \
    && rm /tmp/jetty.tgz

RUN useradd --system --user-group --no-create-home --home-dir "${JETTY_BASE}" jetty \
    && mkdir -p "${JETTY_BASE}/tmp" "${JETTY_BASE}/work" \
    && cd "${JETTY_BASE}" \
    && java -jar "${JETTY_HOME}/start.jar" --add-modules=server,http,ee10-deploy,ee10-annotations,ee10-jsp \
    && chown -R jetty:jetty "${JETTY_BASE}"

COPY --from=build --chown=jetty:jetty /build/target/xlport-*.war ${JETTY_BASE}/webapps/ROOT.war

USER jetty
WORKDIR ${JETTY_BASE}
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s \
    CMD curl -fs http://localhost:8080/alive || exit 1
# java.io.tmpdir keeps JVM/Jetty temp files inside the jetty-owned base dir
# (the JVM does not honor the TMPDIR environment variable)
CMD ["java", "-Djava.io.tmpdir=/opt/jetty-base/tmp", "-jar", "/opt/jetty-home/start.jar"]
