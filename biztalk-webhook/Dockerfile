FROM amazoncorretto:17-alpine-jdk
WORKDIR /app
COPY biztalk-webhook/build/libs/biztalk-webhook-1.0.0-SNAPSHOT.jar app.jar
ENV JAVA_OPTS="-Xms128m -Xmx256m"
EXPOSE 8082
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]