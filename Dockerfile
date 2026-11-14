FROM node:24-bookworm-slim AS frontend
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
ENV VITE_DEMO_MODE=true
RUN npm run build
FROM eclipse-temurin:21-jdk-jammy AS backend
WORKDIR /build
COPY backend/ ./
COPY --from=frontend /frontend/dist/ ./src/main/resources/static/
RUN chmod +x mvnw && ./mvnw -B -DskipTests package
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --gid 10001 rainops && useradd --uid 10001 --gid rainops --no-create-home rainops
COPY --from=backend --chown=rainops:rainops /build/target/rainops-0.0.1-SNAPSHOT.jar app.jar
USER rainops
ENV SPRING_PROFILES_ACTIVE=demo
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
