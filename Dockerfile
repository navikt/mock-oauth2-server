# Stage 1: Use debian to install curl and gather its dependencies
FROM debian:11-slim AS curl-deps
RUN apt-get update && apt-get install -y curl
RUN mkdir -p /tmp/curl-deps
RUN cp $(ldd /usr/bin/curl | grep -v linux-vdso.so.1 | awk '{print $3}' | grep -v '^$') /tmp/curl-deps/
RUN cp /usr/bin/curl /tmp/curl-deps/

# Stage 2: Final distroless image
FROM gcr.io/distroless/java17-debian11
COPY --from=curl-deps /tmp/curl-deps/* /usr/lib/
COPY --from=curl-deps /usr/bin/curl /usr/bin/

# Configure the application
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
COPY build/libs/app.jar /app/app.jar
WORKDIR /app
CMD ["app.jar"]
