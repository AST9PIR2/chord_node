FROM openjdk:latest

LABEL authors="Pirmin Aster"

WORKDIR /app

#ENV CHORD_ADDRESS="0x3a7b2c98dfe45a1b7e982f15ab674cfe7894c72a"

COPY build/libs/chord_node-1.0-SNAPSHOT.jar /app

WORKDIR /app

EXPOSE 8991

CMD ["java", "-jar", "chord_node-1.0-SNAPSHOT.jar"]
