FROM amazoncorretto:17-alpine-jdk
# 2. 필수 네트워크 진단 유틸리티 및 busybox 설치
RUN apk add --no-cache \
    busybox-extras \
    netcat-openbsd \
    curl \
    wget \
    bash
WORKDIR /app
COPY biztalk-api/build/libs/biztalk-api-1.0.0-SNAPSHOT.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]