VENV_DIR := .venv
PYTHON := $(VENV_DIR)/bin/python
PIP := $(VENV_DIR)/bin/pip

MODEL_SCRIPT := study/main.py
FRONTEND_DIR := frontend/motif-mining-app
DOCS_DIR := frontend-docs/swagger-ui
JAVA_MAIN_CLASS := com.github.cgks.MotifMiningApplication
JAVA := /Library/Java/JavaVirtualMachines/amazon-corretto-11.jdk/Contents/Home/bin/java

.PHONY: all help venv model clean deps build api frontend docs

help:
	@echo "Available targets:"
	@echo "  make help       - Show this help message"
	@echo "  make all        - Run everything: venv, deps, build, model, frontend"
	@echo "  make venv       - Create Python virtual environment and install requirements"
	@echo "  make model      - Run the Python model training script"
	@echo "  make clean      - Clean Java build artifacts (Maven)"
	@echo "  make deps       - Resolve Java dependencies with Maven"
	@echo "  make build      - Build the Java backend with Maven"
	@echo "  make api        - Run the Java backend application"
	@echo "  make frontend   - Build the React frontend (npm install + build)"
	@echo "  make ui         - Serve the React frontend on http://localhost:3000"
	@echo "  make docs       - Serve Swagger UI docs on http://localhost:8000"
	@echo ""
	@echo "Manual activation of Python venv:"
	@echo "  source $(VENV_DIR)/bin/activate"

all: venv deps build model frontend

# === Python ===
venv:
	python3 -m venv $(VENV_DIR)
	$(PIP) install --upgrade pip
	$(PIP) install -r requirements.txt

model: $(MODEL_SCRIPT)
	$(PYTHON) $(MODEL_SCRIPT)

# === Backend (Java - Maven) ===
clean:
	mvn clean

deps:
	mvn dependency:resolve dependency:sources

build:
	mvn install

api:
	$(JAVA) -cp target/classes:target/dependency/* $(JAVA_MAIN_CLASS)

# === Frontend (React) ===
frontend:
	cd $(FRONTEND_DIR) && npm install && npm run build

ui:
	cd $(FRONTEND_DIR) && npm start

# === Documentation (Swagger UI) ===
docs:
	cd $(DOCS_DIR) && python3 -m http.server 8000
