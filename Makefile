.PHONY: all clean build deps backend frontend docs serve-docs serve-frontend

# Default target
all: clean deps build

# Backend (Java - Maven)
clean:
	cd backend && mvn clean

deps:
	cd backend && mvn dependency:resolve dependency:sources

build:
	cd backend && mvn install

# Frontend (React - npm)
# frontend:
# 	cd frontend/my-ui && npm install && npm run build

# serve-frontend:
# 	cd frontend/my-ui && npm run dev

# Swagger UI Docs
docs:
	cd frontend-docs/swagger-ui/ && python3 -m http.server 8000
