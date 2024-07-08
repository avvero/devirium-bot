# Makefile

# Variables for Docker image names
IMAGE_NAME := devirium-bot
DOCKER_REPO := avvero
VERSION := $(shell grep '^version=' gradle.properties | cut -d '=' -f2)

test:
	./gradlew test

run:
	./gradlew devirium-bot:bootRun

run-with-agent:
	./gradlew devirium-bot:installBootDist
	java -agentlib:native-image-agent=config-output-dir=devirium-bot/src/main/resources/META-INF/native-image -jar devirium-bot/build/libs/devirium-bot-${VERSION}.jar

# Docker build command for standard Dockerfile
docker-build:
	docker build -t $(DOCKER_REPO)/$(IMAGE_NAME):latest -f Dockerfile .
	docker tag $(DOCKER_REPO)/$(IMAGE_NAME):latest $(DOCKER_REPO)/$(IMAGE_NAME):$(VERSION)

# Docker push command for standard image
docker-push:
	docker push $(DOCKER_REPO)/$(IMAGE_NAME):latest
	docker push $(DOCKER_REPO)/$(IMAGE_NAME):$(VERSION)

# Docker build command for native Dockerfile
docker-build-native:
	docker build -t $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):latest -f Dockerfile.native .
	docker tag $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):latest $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):$(VERSION)

# Docker push command for native image
docker-push-native:
	docker push $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):latest
	docker push $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):$(VERSION)

.PHONY: test run run-with-agent native-build docker-build docker-build-native docker-push docker-push-native