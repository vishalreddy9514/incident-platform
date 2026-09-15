# Multi-stage build: compile with the full JDK/Maven toolchain, run on a
# minimal JRE-only image so the shipped artifact is just the jar and its
# runtime, not a build environment.

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# Maven wrapper isn't checked in (see pom.xml/README) — use the same Maven
# version as local/CI development via the official image's toolchain, but
# keep this stage's own concerns (mvn itself) separate from the app's.
RUN apk add --no-cache maven

# Dependency layer cached separately from source so a source-only change
# doesn't re-download the world.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
# Tests run in CI (Phase 11), not the image build; spotless:check is bound to
# `verify`, which `package` doesn't reach, so this also never fails a build
# on formatting alone.
RUN mvn -q -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Runs as a non-root user rather than the image's default root (Phase 1 §10
# treats least-privilege as a design-in, not a retrofit).
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/*.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
