# ====== STAGE 1: build (compila o JAR) ======
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos só o que ajuda a usar cache de dependências
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw mvnw
RUN chmod +x mvnw

# Baixa dependências sem rodar testes (acelera builds seguintes)
RUN ./mvnw -q -DskipTests dependency:go-offline

# Agora sim, copia o código-fonte e empacota
COPY src/ src/
RUN ./mvnw -q -DskipTests clean package

# ====== STAGE 2: runtime (só o necessário pra rodar) ======
FROM eclipse-temurin:21-jre
WORKDIR /opt/app

# (Opcional, mas boa prática) parâmetros de JVM “seguros” p/ containers
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

# Copia o JAR gerado do estágio de build
# Se seu nome de JAR mudar, pode usar *.jar
COPY --from=build /app/target/rhydon-0.0.1-SNAPSHOT.jar app.jar

# Exponha a porta da aplicação
EXPOSE 8080

# Inicia a app
ENTRYPOINT ["java","-jar","/opt/app/app.jar"]
