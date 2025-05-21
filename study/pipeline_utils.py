import os
import joblib
import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd
from sklearn.pipeline import Pipeline
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.impute import SimpleImputer
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
from typing import List, Tuple, Dict, Any
from dataprep import *
from global_var import SAVED_PIPELINE_FOLDER, V_PIPELINE, ANNOT_T
from sklearn.model_selection import GridSearchCV


def init_preprocessor(categorical_features: List[str], numeric_features: List[str]) -> ColumnTransformer:
    """
    Initialize a preprocessor for handling categorical and numeric features.
    
    Parameters:
    -----------
    categorical_features : List[str]
        List of categorical feature names
    numeric_features : List[str]
        List of numeric feature names
        
    Returns:
    --------
    ColumnTransformer
        The configured preprocessor
    """
    # Numerical preprocessing: impute missing values with median and scale
    numerical_transformer = Pipeline(steps=[
        ('imputer', SimpleImputer(strategy='median')),
        ('scaler', StandardScaler())
    ])

    # Categorical preprocessing: impute missing values with most frequent and one-hot encode
    categorical_transformer = Pipeline(steps=[
        ('imputer', SimpleImputer(strategy='most_frequent')),
        ('encoder', OneHotEncoder(handle_unknown='ignore'))
    ])

    # Combine preprocessing steps
    preprocessor = ColumnTransformer(
        transformers=[
            ('num', numerical_transformer, numeric_features),
            ('cat', categorical_transformer, categorical_features)
        ])
    
    return preprocessor


def init_pipeline(categorical_features: List[str], numeric_features: List[str]) -> GridSearchCV:
    """
    Initialize a machine learning pipeline with preprocessing and classifier.
    
    Parameters:
    -----------
    categorical_features : List[str]
        List of categorical feature names
    numeric_features : List[str]
        List of numeric feature names
        
    Returns:
    --------
    GridSearchCV
        The configured grid search pipeline
    """
    preprocessor = init_preprocessor(
        categorical_features=categorical_features, 
        numeric_features=numeric_features
    )    
    
    # Create the full pipeline with a RandomForest classifier
    pipeline = Pipeline(steps=[
        ('preprocessor', preprocessor),
        ('classifier', RandomForestClassifier(random_state=42))
    ])

    # Define hyperparameter grid for tuning
    param_grid = {
        'classifier__n_estimators': [100, 200],
        'classifier__max_depth': [None, 10, 20],
        'classifier__min_samples_split': [2, 5],
        'classifier__min_samples_leaf': [1, 2]
    }

    # Create grid search
    grid_search = GridSearchCV(
        pipeline, 
        param_grid=param_grid, 
        cv=5, 
        n_jobs=-1, 
        verbose=1, 
        scoring='accuracy'
    )
    
    return grid_search


def fit(X_train: pd.DataFrame, y_train: pd.Series, pipeline: GridSearchCV) -> None:
    """
    Fit the pipeline to the training data.
    
    Parameters:
    -----------
    X_train : pd.DataFrame
        Training features
    y_train : pd.Series
        Training target
    pipeline : GridSearchCV
        The pipeline to fit
    """
    pipeline.fit(X_train, y_train)
    

def evaluate(X_test: pd.DataFrame, y_test: pd.Series, pipeline: GridSearchCV, 
             should_save: bool = True) -> None:
    """
    Evaluate the model on test data and print metrics.
    
    Parameters:
    -----------
    X_test : pd.DataFrame
        Test features
    y_test : pd.Series
        Test target
    pipeline : GridSearchCV
        The fitted pipeline
    should_save : bool, default=True
        Whether to save the confusion matrix plot
    """
    # Make predictions and evaluate
    y_pred = pipeline.predict(X_test)
    print("\nModel Evaluation:")
    print(f"Accuracy: {accuracy_score(y_test, y_pred)}")
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred))

    # Convert predictions and ground truth to annotated labels
    def apply_annotation(pred):
        return ANNOT_T[pred]
        
    y_pred_annotated = list(map(apply_annotation, y_pred))
    y_true_annotated = list(map(apply_annotation, y_test))
    
    # Create and plot confusion matrix
    confusion_mat = confusion_matrix(y_true_annotated, y_pred_annotated)
    plot_confusion_matrix(confusion_mat, should_save)


def plot_confusion_matrix(confusion_mat: np.ndarray, should_save: bool) -> None:
    """
    Plot a confusion matrix.
    
    Parameters:
    -----------
    confusion_mat : np.ndarray
        The confusion matrix to plot
    should_save : bool
        Whether to save the plot
    """
    plt.figure(figsize=(10, 7))
    sns.heatmap(confusion_mat, annot=True, fmt='d', cmap='Blues')
    plt.title('Confusion Matrix')
    plt.ylabel('True Label')
    plt.xlabel('Predicted Label')
    
    if should_save:
        save_path = f"{SAVED_PIPELINE_FOLDER}/{V_PIPELINE}/confusion_matrix.jpg"
        plt.savefig(save_path, format="jpg", dpi=300, bbox_inches='tight')
        plt.close()


def plot_feature_importances(classifier, numeric_features: List[str], 
                             categorical_features: List[str], 
                             X_train: pd.DataFrame, should_save: bool) -> None:
    """
    Plot feature importances for a Random Forest model.
    
    Parameters:
    -----------
    classifier : object
        The trained classifier with feature_importances_ attribute
    numeric_features : List[str]
        List of numeric feature names
    categorical_features : List[str]
        List of categorical feature names
    X_train : pd.DataFrame
        Training data used to determine unique values for categorical features
    should_save : bool
        Whether to save the plot
    """
    # Check if classifier is a RandomForestClassifier
    if isinstance(classifier, RandomForestClassifier):
        # Get the feature names after preprocessing
        feature_names = []
        
        # Get transformed feature names for numerical columns
        feature_names.extend(numeric_features)
        
        # Get transformed feature names for categorical columns
        for col in categorical_features:
            unique_values = X_train[col].unique()
            for val in unique_values:
                feature_names.append(f"{col}_{val}")
        
        # Get feature importances
        importances = classifier.feature_importances_
        
        # Match importances to feature names (take only the first len(importances) features)
        feature_importance = pd.DataFrame({
            'Feature': feature_names[:len(importances)],
            'Importance': importances
        }).sort_values('Importance', ascending=False)
        
        # Plot feature importances
        plt.figure(figsize=(12, 8))
        sns.barplot(x='Importance', y='Feature', data=feature_importance.head(15))
        plt.title('Top 15 Feature Importances')
        plt.tight_layout()
        
        if should_save:
            save_path = f"{SAVED_PIPELINE_FOLDER}/{V_PIPELINE}/feature_importances.jpg"
            plt.savefig(save_path, format="jpg", dpi=300, bbox_inches='tight')
            plt.close()
        

def load_model(model_path: str):
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
