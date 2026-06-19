# Etapa 1: Compilação (Build)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copia os arquivos do projeto para dentro do contêiner
COPY pom.xml .
COPY src ./src

# Compila o projeto gerando o .jar (pulando testes para economizar tempo)
RUN mvn clean package -DskipTests

# Etapa 2: Execução
FROM eclipse-temurin:21-jre
WORKDIR /app

# Pega o .jar gerado na etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Libera a porta da API
EXPOSE 8080

# Comando que inicializa a sua API
ENTRYPOINT ["java", "-jar", "app.jar"]