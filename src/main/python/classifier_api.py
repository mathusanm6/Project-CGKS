from flask import Flask, request, jsonify
import pickle
import pandas as pd
import numpy as np
import joblib
from pathlib import Path

# Joblib manip
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

# Const def
BASE_DIR = Path(__file__).resolve().parent.parent
app = Flask(__name__)
MODEL = load_model(f"{BASE_DIR}/resources/model/pipeline.pkl")
META = pd.read_csv(f"{BASE_DIR}/resources/model/metadata.csv")


@app.route('/predict', methods=['POST'])
def predict():
    # Get data from request
    #data = request.json
    #print(data)
    request = {
        'Query': 'Q1',
        'File': 'contextPasquier99.dat',
        'Frequency': 0.34
    }
    request_pd = pd.DataFrame({k: [v] for k, v in request.items()})
    input = pd.merge(request_pd, META, on='File', how='inner')
    prediction = MODEL.predict(input) # Prediction    
    return jsonify({'prediction': prediction.tolist()})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
    
