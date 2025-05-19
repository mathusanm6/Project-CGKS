# Imports
import pandas as pd
import numpy as np
import joblib
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent


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
    

if __name__ == '__main__':
    # Params retrieval
    model = load_model(f"{BASE_DIR}/pipeline.pkl")
    metadata = pd.read_csv(f"{BASE_DIR}/metadata.csv")
    request = {
        'Query': 'Q1',
        'File': 'contextPasquier99.dat',
        'Frequency': 0.34
    }
    request_pd = pd.DataFrame({k: [v] for k, v in request.items()})
    input = pd.merge(request_pd, metadata, on='File', how='inner')
    output= model.predict(input)[0] # Prediction
