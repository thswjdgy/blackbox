FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon || true
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# 한글 PDF용 NanumGothic 폰트 다운로드
RUN apk add --no-cache wget && \
    wget -q "https://github.com/google/fonts/raw/main/ofl/nanumgothic/NanumGothic-Regular.ttf" \
         -O /tmp/NanumGothic.ttf

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN mkdir -p /data/uploads /app/fonts
COPY --from=builder /app/build/libs/*.jar app.jar
COPY --from=builder /tmp/NanumGothic.ttf /app/fonts/NanumGothic.ttf
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]