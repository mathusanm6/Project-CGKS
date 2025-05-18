import joblib
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.pipeline import Pipeline
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.impute import SimpleImputer
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
from typing import List
from dataprep import *
from global_var import *
from sklearn.model_selection import GridSearchCV

def init_preprocessor(categorical_features, numeric_features)->Pipeline:
    # Step 6: Create preprocessing pipelines for categorical and numerical data
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

    # Step 7: Combine preprocessing steps
    preprocessor = ColumnTransformer(
        transformers=[
            ('num', numerical_transformer, numeric_features),
            ('cat', categorical_transformer, categorical_features)
        ])
    return preprocessor

def init_pipeline(categorical_features, numeric_features):
    preprocessor = init_preprocessor(categorical_features=categorical_features, numeric_features=numeric_features)    
    # Step 8: Create the full pipeline with a RandomForest classifier
    pipeline = Pipeline(steps=[
        ('preprocessor', preprocessor),
        ('classifier', RandomForestClassifier(random_state=42))
    ])

    # Step 9: Train and tuning the model
    param_grid = {
        'classifier__n_estimators': [100, 200],
        'classifier__max_depth': [None, 10, 20],
        'classifier__min_samples_split': [2, 5],
        'classifier__min_samples_leaf': [1, 2]
    }

    grid_search = GridSearchCV(
        pipeline, 
        param_grid=param_grid, 
        cv=5, 
        n_jobs=-1, 
        verbose=1, 
        scoring='accuracy'
    )
    return grid_search

def fit(X_train, y_train, pipeline):
    pipeline.fit(X_train, y_train)
    
def evaluate(X_test, y_test, pipeline, save=True):
    # Step 10: Evaluate the model
    y_pred = pipeline.predict(X_test)
    print("\nModel Evaluation:")
    print("Accuracy:", accuracy_score(y_test, y_pred))
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred))

    apply_anno = lambda pred: ANNOT_T[pred]
    y_pred_anno = list(map(apply_anno, y_pred))
    y_true_anno = list(map(apply_anno, y_test))
    # Create and plot confusion matrix
    cm = confusion_matrix(y_true_anno, y_pred_anno)
    plot_cm(cm, save)

# Plot functions
def plot_cm(cm, save):
    plt.figure(figsize=(10, 7))
    sns.heatmap(cm, annot=True, fmt='d', cmap='Blues')
    plt.title('Confusion Matrix')
    plt.ylabel('True Label')
    plt.xlabel('Predicted Label')
    if save : plt.savefig(f"{SAVED_PIPELINE_FOLDER}/{V_PIPELINE}/cm.jpg", format="jpg", dpi=300, bbox_inches='tight')


def plot_feature_importances(pipeline, numeric_features, categorical_features, X_train, save):

    # Step 12 (Optional): Feature Importance for Random Forest
    if isinstance(pipeline.named_steps['classifier'], RandomForestClassifier):
        # Get the feature names after preprocessing
        # Note: This is a simplified approach and might need adjustment depending on your data
        feature_names = []
        
        # Get transformed feature names for numerical columns (they keep their names)
        feature_names.extend(numeric_features)
        
        # Get transformed feature names for categorical columns (they get expanded)
        # This is a simplification - actual names depend on OneHotEncoder specifics
        for col in categorical_features:
            unique_values = X_train[col].unique()
            for val in unique_values:
                feature_names.append(f"{col}_{val}")
        
        # Get feature importances
        importances = pipeline.named_steps['classifier'].feature_importances_
        
        # Match importances to feature names (take only the first len(importances) features)
        feature_importance = pd.DataFrame({
            'Feature': feature_names[:len(importances)],  # This is a simplification
            'Importance': importances
        }).sort_values('Importance', ascending=False)
        
        # Plot feature importances
        plt.figure(figsize=(12, 8))
        sns.barplot(x='Importance', y='Feature', data=feature_importance.head(15))
        plt.title('Top 15 Feature Importances')
        plt.tight_layout()
        if save: plt.savefig(f"{SAVED_PIPELINE_FOLDER}/{V_PIPELINE}/feature_importances.jpg", format="jpg", dpi=300, bbox_inches='tight')
        

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
