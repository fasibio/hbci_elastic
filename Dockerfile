FROM openjdk:17-alpine
RUN apk add --no-cache tzdata
ENV TZ=Europe/Berlin
RUN mkdir /home/db
COPY build/libs/hbci-app-0.0.1-SNAPSHOT.jar /home/hbci-app-0.0.1-SNAPSHOT.jar
WORKDIR /home
ENTRYPOINT ["java","-jar","./hbci-app-0.0.1-SNAPSHOT.jar"]