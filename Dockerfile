FROM openjdk:8-jdk-alpine
EXPOSE 8090
WORKDIR /app

# Copy maven executable to the image
COPY mvnw .
COPY .mvn .mvn

# Copy the pom.xml file
COPY pom.xml .

# Copy the project source
COPY ./src ./src
COPY ./pom.xml ./pom.xml

RUN chmod 755 /app/mvnw

RUN ./mvnw dependency:go-offline -B

RUN ./mvnw package
#RUN ls -al
ENTRYPOINT ["java","-jar","target/microservicios-futfem-squads-temp-0.0.1-SNAPSHOT.jar"]


#ADD target/microservicios-futfem-squads-temp-0.0.1-SNAPSHOT.jar futfem_squads-temp.jar
#ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/futfem_squads-temp.jar"]


