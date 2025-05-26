# Variables for the application containers
APP_CONTAINER_IMAGE=my-rabbitmq-app
APP_CONTAINER_NAME=rabbitmq-app
APP_DOCKER_CONTEXT=.
APP_DOCKERFILE=./src/main/docker/app/Dockerfile
APP_PORT=8081

# Variables for the RabbitMQ container
RABBITMQ_CONTAINER_IMAGE=my-rabbitmq
RABBITMQ_CONTAINER_NAME=rabbitmq-server
RABBITMQ_DEFAULT_USER=spring_user
RABBITMQ_DEFAULT_PASS=P@ssw0rd
RABBITMQ_DOCKER_CONTEXT=.
RABBITMQ_DOCKERFILE=./src/main/docker/rabbitmq/Dockerfile
RABBITMQ_MANAGEMENT_PORT=15672
RABBITMQ_PORT=5672

# Network for the application and RabbitMQ containers
NETWORK=app-network

# Running in development mode
dev:
	@echo "Running in development mode..."
	./mvnw spring-boot:run

# Building the application as a JAR file
# This will run Maven Lifecycle phase "package": clean → validate → compile → test → package, 
# which cleans the target directory, compiles the code, runs tests, and packages the application into a JAR file.
package:
	@echo "Building the application as a JAR file..."
	./mvnw clean package -DskipTests


# Docker related targets
# Create a Docker network if it does not exist
docker-create-network:
	docker network inspect $(NETWORK) >NUL 2>&1 || docker network create $(NETWORK)

# Remove the Docker network if it exists
docker-remove-network:
	docker network rm $(NETWORK)

# Build RabbitMQ Docker image with management plugin
docker-build-rabbitmq:
	docker build -f $(RABBITMQ_DOCKERFILE) -t $(RABBITMQ_CONTAINER_IMAGE) $(RABBITMQ_DOCKER_CONTEXT)

# Run RabbitMQ in Docker
docker-run-rabbitmq:
	docker run --name $(RABBITMQ_CONTAINER_NAME) --network $(NETWORK)  \
	-p $(RABBITMQ_PORT):$(RABBITMQ_PORT) -p $(RABBITMQ_MANAGEMENT_PORT):$(RABBITMQ_MANAGEMENT_PORT) \
	-e RABBITMQ_DEFAULT_USER=$(RABBITMQ_DEFAULT_USER) \
	-e RABBITMQ_DEFAULT_PASS=$(RABBITMQ_DEFAULT_PASS) \
	-d $(RABBITMQ_CONTAINER_IMAGE)


# Remove the RabbitMQ container
docker-remove-rabbitmq:
	docker stop $(RABBITMQ_CONTAINER_NAME)
	docker rm $(RABBITMQ_CONTAINER_NAME)

# Build the application in Docker
docker-build-app:
	docker build -f $(APP_DOCKERFILE) -t $(APP_CONTAINER_IMAGE) $(APP_DOCKER_CONTEXT)

# Wait for RabbitMQ to be ready before running the application
docker-wait-for-rabbitmq-on-windows:
	@cmd /c "wait-rabbitmq-on-windows.bat localhost $(RABBITMQ_PORT)"

# Run the application in Docker
docker-run-app: 
	docker run --name $(APP_CONTAINER_NAME) --network $(NETWORK) -p $(APP_PORT):$(APP_PORT) \
	-e SERVER_PORT=$(APP_PORT) \
	-d $(APP_CONTAINER_IMAGE)

# Remove the application container
docker-remove-app:
	docker stop $(APP_CONTAINER_NAME)
	docker rm $(APP_CONTAINER_NAME)

# Start all services: RabbitMQ and the application
docker-start-all: docker-create-network docker-build-rabbitmq docker-run-rabbitmq docker-build-app docker-wait-for-rabbitmq-on-windows docker-run-app

# Stop all services: RabbitMQ and the application
docker-stop-all: docker-remove-app docker-remove-rabbitmq docker-remove-network

.PHONY: dev package \
	docker-create-network docker-remove-network \
	docker-build-rabbitmq docker-run-rabbitmq docker-remove-rabbitmq \
	docker-build-app docker-wait-for-rabbitmq-on-windows docker-run-app docker-remove-app \
	docker-start-all docker-stop-all