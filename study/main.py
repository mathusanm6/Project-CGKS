import os
import pandas as pd
import joblib
from dataprep import compute_metadata
from global_var import DATA_FOLDER, DATASETS_FOLDER
from pipeline_utils import (
    columns_preparation,
    init_pipeline,
    fit,
    evaluate,
    plot_feature_importances,
    SAVED_PIPELINE_FOLDER,
    V_PIPELINE
)
from sklearn.model_selection import train_test_split


if __name__ == "__main__":
    # Preprocessing
    dataframe = pd.read_csv(f"{DATASETS_FOLDER}/classes.csv")
    features, target, numeric_features, categorical_features = columns_preparation(dataframe, save=True)
    
    # Pipeline preparation
    should_save = True
    if should_save: 
        os.makedirs(name=f"{SAVED_PIPELINE_FOLDER}/{V_PIPELINE}/", exist_ok=True)
    
    X_train, X_test, y_train, y_test = train_test_split(
        features, 
        target, 
        test_size=0.2, 
        random_state=42, 
        stratify=target
    )
    
    pipeline = init_pipeline(
        numeric_features=numeric_features, 
        categorical_features=categorical_features
    )

    # Training
    fit(X_train, y_train, pipeline)

    # Evaluation
    evaluate(X_test, y_test, pipeline, should_save)
    plot_feature_importances(
        pipeline.best_estimator_, 
        numeric_features, 
        categorical_features, 
        X_train, 
        should_save
    )

    # Saving pipeline
    if should_save:
        # Save to pipeline folder
        joblib.dump(pipeline, f"{SAVED_PIPELINE_FOLDER}/{V_PIPELINE}/pipeline.pkl")
        # Save to location where API retrieves classifier
        joblib.dump(pipeline, "src/main/resources/model/pipeline.pkl")
