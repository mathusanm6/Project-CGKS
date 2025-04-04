.PHONY: all clean build deps

all: clean deps build

clean:
	mvn clean

deps:
	mvn dependency:resolve
	mvn dependency:sources

build:
	mvn install