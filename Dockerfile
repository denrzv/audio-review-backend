FROM maven:3.9.9-amazoncorretto-17-debian AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests

FROM amazoncorretto:17-alpine
WORKDIR /opt/app

RUN addgroup --system javauser && adduser -S -s /usr/sbin/nologin -G javauser javauser

COPY --from=builder /build/target/*.jar app.jar

RUN chown -R javauser:javauser .
USER javauser

ENTRYPOINT ["java", "-jar", "app.jar"]