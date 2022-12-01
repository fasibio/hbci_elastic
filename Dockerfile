FROM openjdk:17-alpine
COPY build/libs/hbci-app-0.0.1-SNAPSHOT.jar /home/hbci-app-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/home/hbci-app-0.0.1-SNAPSHOT.jar"]