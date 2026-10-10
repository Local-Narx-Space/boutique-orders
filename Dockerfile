FROM maven:3.9-eclipse-temurin-26@sha256:b2c1ad85954592f9928e84327c65201f308ad9b5d8ed7d5b823717c97bf23fbb AS test
WORKDIR /src
COPY pom.xml .
RUN mvn -B -Dmaven.repo.local=/opt/maven-cache dependency:go-offline
COPY src ./src
COPY ci ./ci
RUN chmod -R a+rX /src /opt/maven-cache
CMD ["sh", "ci/test.sh"]

FROM test AS build
RUN mvn -B -Dmaven.repo.local=/opt/maven-cache -DskipTests package
FROM eclipse-temurin:21-jre-alpine@sha256:51ab5e3302e7141ce665ca3ea85e8b5cd648eafbc3c0c90dd79d6537684e4555
RUN addgroup -g 10001 app && adduser -D -u 10001 -G app app
WORKDIR /app
COPY --from=build /src/target/orders-1.0.0.jar app.jar
COPY --chown=app:app certs /app/certs
USER 10001:10001
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8083
ENTRYPOINT ["java","-jar","app.jar"]
