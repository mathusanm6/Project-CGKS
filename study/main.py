import pandas as pd
from dataprep import compute_metadata
from global_var import DATA_FOLDER, DATASETS_FOLDER
from pipeline_utils import *
from sklearn.model_selection import train_test_split


if __name__ == "__main__":
    # Compute dataset and save it
    #compute_metadata(DATA_FOLDER, path=f"{DATASETS_FOLDER}/choco_meta.csv", path_to_save=f"{DATASETS_FOLDER}/choco_meta.csv")

    # Preprocessing
    df = pd.read_csv(f"{DATASETS_FOLDER}/classes.csv")
    X, y, numeric_features, categorical_features = columns_preparation(df, save=True)
    
    # Pipeline preparation
    save = True
    if save: 
        os.makedirs(name=f"{SAVED_PIPELINE_FOLDER}/{V_PIPELINE}/", exist_ok=True)
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)
    pipeline = init_pipeline(numeric_features=numeric_features, categorical_features=categorical_features)

    fit(X_train, y_train, pipeline) # Training

    # Evaluation
    evaluate(X_test, y_test, pipeline, save)
    plot_feature_importances(pipeline.best_estimator_, numeric_features, categorical_features, X_train, save)

    # Saving pipeline
    if save:
        joblib.dump(pipeline, f"{SAVED_PIPELINE_FOLDER}/{V_PIPELINE}/pipeline.pkl")
