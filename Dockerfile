##
## Build Image Layer ##
##
FROM clojure:lein AS builder

WORKDIR /opt

# Copy deps file for caching next layer
COPY project.clj .

# Cache deps
RUN lein deps

# Copy source
COPY . .

# Build the app
RUN lein uberjar

##
## RUNTIME IMAGE ##
##
FROM eclipse-temurin:19 AS runtime

# Copy the resources folder
COPY --from=builder /opt/resources /resources

COPY --from=builder /opt/target/uberjar/dub-box.jar /app.jar

CMD ["java", "-jar", "/app.jar"]