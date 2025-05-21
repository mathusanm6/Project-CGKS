import pandas as pd
import joblib
from flask import Flask, request, jsonify
from pathlib import Path

# Joblib model loader
def load_model(model_path):
    """
    Load a saved model from disk.
    
    Parameters:
    -----------
    model_path : str
        Path to the saved model file (.joblib or .pkl)
        
    Returns:
    --------
    model : object
        The loaded model
    """
    try:
        model = joblib.load(model_path)
        return model
    except FileNotFoundError:
        raise FileNotFoundError(f"Model file not found: {model_path}")
    except Exception as e:
        raise Exception(f"Error loading model: {str(e)}")

# Define constants and initialize the application
BASE_DIR = Path(__file__).resolve().parent.parent
app = Flask(__name__)

# Load model and metadata only once at startup
MODEL = load_model(f"{BASE_DIR}/resources/model/pipeline.pkl")
METADATA = pd.read_csv(f"{BASE_DIR}/resources/model/metadata.csv")


@app.route('/predict', methods=['POST'])
def predict():
    """
    API endpoint to make predictions using the pre-loaded model.
    
    Expects a JSON payload with at least a 'File' key that matches 
    entries in the metadata file and features: Query and Frequency.
    
    Returns:
    --------
    JSON response with prediction results
    """
    # Get data from request
    request_data = request.json
    
    # Convert JSON data to a pandas DataFrame with one row
    request_df = pd.DataFrame({k: [v] for k, v in request_data.items()})
    
    # Merge with metadata using the 'File' field as the join key
    input_features = pd.merge(request_df, METADATA, on='File', how='inner')
    
    # Generate prediction using the pre-loaded model
    prediction_result = MODEL.predict(input_features)
    
    # Return prediction as JSON response
    return jsonify({'prediction': prediction_result.tolist()})


if __name__ == '__main__':
    # Start the Flask application when script is run directly
    app.run(host='0.0.0.0', port=5000, debug=True)
