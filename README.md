# Project CGKS: Declarative & Specialized Itemset Mining

This project is a full-stack application for motif mining, featuring:
- A Java backend (Spring Boot, Maven)
- A Python model (training and classifier API)
- A React frontend
- Swagger UI documentation

## Prerequisites

- **Python 3.8+** (for model training and classifier API)
- **Node.js 16+ & npm** (for frontend)
- **Java 11+** (for backend)
- **Maven** (for backend build)
- **Make** (to use provided Makefile)

## Project Structure

```
.
├── Makefile
├── requirements.txt
├── pom.xml
├── study/                # Python model code
├── src/main/java/        # Java backend code
├── src/main/python/      # Python classifier API
├── src/main/resources/   # Datasets and Python model .pkl file
├── src/test/java/        # Java test classes
├── frontend/motif-mining-app/  # React frontend
├── frontend-docs/swagger-ui/   # Swagger UI docs
````

Quick Start
1. Clone the repository
```
git clone https://github.com/Projets-FSO/projet-isd-cgks.git
cd projet-isd-cgks
```


2. Run all setup and builds
```
make prep
```

This will:

- Create a Python virtual environment and install requirements
- Resolve Java dependencies and build the backend
- Build the React frontend

3. Train the Python model (optional)
```
make model
```
This runs the training script and copies the trained model to the backend resources. Since the trained model is already p^rovided, you don't have to do it.

4. Run the backend (Java Spring Boot API)
```
# In another terminal
make api
```
The backend will start on the default port (usually 8080).

5. Run the Python classifier API
```
# In another terminal
make selector
```
This starts the Python API for classification.

6. Serve the React frontend
```
# In another terminal
make ui
```
The frontend will be available at http://localhost:3000.

7. Serve the Swagger UI documentation
```
make docs
```
The docs will be available at http://localhost:8000.


### Additional Commands
- `make test` — Run Java unit tests
- `make clean` — Clean Java build artifacts

### Manual Python venv Activation
If you need to activate the Python virtual environment manually:

### Notes
Ensure all prerequisites are installed and available in your PATH.
If you encounter issues with permissions or missing dependencies, check your environment and install any missing tools.
