FROM amazoncorretto:17-alpine-jdk
# 2. 필수 네트워크 진단 유틸리티 및 busybox 설치
RUN apk add --no-cache \
    busybox-extras \
    netcat-openbsd \
    curl \
    wget \
    bash
WORKDIR /app
COPY biztalk-mockserver/build/libs/biztalk-mockserver-1.0.0-SNAPSHOT.jar app.jar
ENV JAVA_OPTS="-Xms128m -Xmx256m"
EXPOSE 8085
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]