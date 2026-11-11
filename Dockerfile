# Multi-stage build: the war is built inside Docker, so no local Maven setup is needed.
# Build:  docker build -t xlport/xlport-repo:v6.2 .
# For x86_64 servers (k8s, customer delivery), always build with --platform=linux/amd64:
# an image built on Apple Silicon is arm64 otherwise and fails with "exec format error".
#
# Base images are pinned by digest so a rebuild of the same commit uses the same base;
# Dependabot (.github/dependabot.yml) opens PRs when the tags move. apt-get upgrade
# below still pulls the latest Ubuntu security fixes at build time.

# The build stage runs on the build machine's own platform: the war and Jetty are
# platform-independent, so Maven and the tests never run under emulation.
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320 AS build
# Bump both together; the checksum is the one Maven Central publishes as jetty-home-<version>.tar.gz.sha512
ARG JETTY_VERSION=12.1.15
ARG JETTY_SHA512=f1e26129ae13e671b1fffaeed43111fe2ca421660680aaa665230716c1c3060b03dd1ce229e787d20c877b36182899a19459766b4058e4245db7a095e9249544
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B clean package
# Unpack the war so the runtime can deploy it as an exploded directory. This keeps
# WEB-INF/templates at a stable path that deployments can bind-mount templates into
# (a packed war would be extracted by Jetty into a temp dir at startup instead).
# $war stays a single word only if the glob matches exactly one file; with two
# matches test -f fails and the build stops instead of unpacking a partial webapp
RUN war=$(echo /build/target/xlport-*.war) && test -f "$war" \
    && mkdir /build/ROOT && cd /build/ROOT && jar -xf "$war"

# Jetty is downloaded and extracted here rather than in the runtime stage:
# ubuntu:26.04's tar needs syscalls that emulated cross-platform builds
# (e.g. --platform=linux/amd64 on Apple Silicon) don't implement yet.
RUN curl -fsSL "https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-home/${JETTY_VERSION}/jetty-home-${JETTY_VERSION}.tar.gz" -o /tmp/jetty.tgz \
    && echo "${JETTY_SHA512}  /tmp/jetty.tgz" | sha512sum -c - \
    && tar -xzf /tmp/jetty.tgz -C /opt \
    && mv "/opt/jetty-home-${JETTY_VERSION}" /opt/jetty-home \
    && rm /tmp/jetty.tgz \
    # Sign-In with Ethereum is not used; drop its module and jar so neither can be
    # enabled and the jar doesn't show up in vulnerability scans. test -e stops the
    # build if a future Jetty renames them, instead of silently keeping them.
    && test -e /opt/jetty-home/modules/ethereum.mod \
    && rm /opt/jetty-home/modules/ethereum.mod /opt/jetty-home/lib/jetty-ethereum-*.jar

# Runtime: Ubuntu 26.04 LTS + distro OpenJDK 25 JRE (LTS, patched via Ubuntu security updates)
# + Jetty 12.1 (ee10 environment for the jakarta.servlet webapp).
FROM ubuntu:26.04@sha256:6a06111c3b0a8600feadb270aec771dc239bf407b25625f93f44ab00e11f4d3c

# JETTY_BASE stays at /var/lib/jetty (the path used by the previous jetty:9.4-based
# image) so existing bind mounts like /var/lib/jetty/webapps/ROOT/WEB-INF/templates
# keep working unchanged.
# LANG: without a UTF-8 locale Java writes non-ASCII characters to the logs as '?'.
ENV JETTY_HOME=/opt/jetty-home \
    JETTY_BASE=/var/lib/jetty \
    LANG=C.UTF-8

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends openjdk-25-jre-headless curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    # pebble ships in the ubuntu:26.04 OCI image but is unused here; remove it so its
    # bundled Go dependencies don't show up in vulnerability scans
    && rm -f /usr/bin/pebble

# jetty-home stays root-owned; only the writable base dir belongs to the jetty user
COPY --from=build /opt/jetty-home ${JETTY_HOME}

# UID/GID pinned to 999, the values used by the official jetty:9.4 image, so host
# directories chowned for the old image stay readable after upgrading
RUN groupadd --system --gid 999 jetty \
    && useradd --system --uid 999 --gid jetty --no-create-home --home-dir "${JETTY_BASE}" jetty \
    && mkdir -p "${JETTY_BASE}/tmp" "${JETTY_BASE}/work" \
    && cd "${JETTY_BASE}" \
    && java -jar "${JETTY_HOME}/start.jar" --add-modules=server,http,ee10-deploy,ee10-annotations,ee10-jsp \
    && chown -R jetty:jetty "${JETTY_BASE}"

# The webapp stays root-owned (read-only for the jetty user): with JSP enabled, a
# writable webapp directory would let a compromised process plant executable JSPs
COPY --from=build /build/ROOT ${JETTY_BASE}/webapps/ROOT
RUN chown root:root "${JETTY_BASE}/webapps"

COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

USER jetty
WORKDIR ${JETTY_BASE}
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s \
    CMD curl -fs http://localhost:8080/alive || exit 1
# The start script runs Jetty and honors JAVA_OPTIONS, like the old jetty:9.4 image.
# Deliberately CMD, not ENTRYPOINT: v6 shipped with only CMD set, and deployment
# tooling that re-creates the image may carry over CMD but drop ENTRYPOINT.
CMD ["/usr/local/bin/docker-entrypoint.sh"]
