FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN apt-get update && \
    apt-get install -y maven && \
    mkdir -p /root/.m2 && \
    echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"><mirrors><mirror><id>google-maven-central</id><name>GCS Maven Central mirror</name><url>https://maven-central.storage-download.googleapis.com/maven2/</url><mirrorOf>central</mirrorOf></mirror></mirrors></settings>' > /root/.m2/settings.xml && \
    mvn -f pom.xml -DskipTests package
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/target/menu-service-0.0.1-SNAPSHOT.jar"]
