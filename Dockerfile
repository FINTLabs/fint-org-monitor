FROM gradle:9.4-jdk25 AS builder
USER root
COPY . .
RUN gradle --no-daemon build -x test # Do not test when building

FROM gcr.io/distroless/java25
ENV JAVA_TOOL_OPTIONS="-XX:+ExitOnOutOfMemoryError"
COPY --from=builder /home/gradle/build/libs/fint-org-monitor-*.jar /data/app.jar
CMD ["/data/app.jar"]
