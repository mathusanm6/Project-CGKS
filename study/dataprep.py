import pandas as pd
import numpy as np
import os
import matplotlib.pyplot as plt
from global_var import *
from typing import List, Any, Dict, Tuple

def statistics(folder_path: str, res: List[Dict[str, Any]]):
    """
    Calculates metadata for each .dat file in the specified folder.
    
    The function iterates through each .dat file, counting unique items, 
    number of transactions, and calculating density. These metadata 
    are added to the provided 'res' list.
    
    Args:
        folder_path (str): Path to the directory containing .dat files
        res (list): List to store the calculated metadata for each file
        
    Returns:
        None: Results are appended to the provided 'res' list
    """
    if not os.path.isdir(folder_path):
        print(f"Folder '{folder_path}' does not exist.")
        return
    for filename in os.listdir(folder_path):
        if filename.endswith(".dat"):
            file_path = os.path.join(folder_path, filename)
            try:
                items = set()  # Set to store unique items
                line_count = 0  # Counter for transactions
                nb_items_in_lines = 0  # Total number of items across all lines
                with open(file_path, 'r', encoding='utf-8', errors='ignore') as file:
                    for line in file:
                        line = line.strip('\n')
                        x = line.split(" ")  # Split line into items
                        
                        nb_items_in_lines += len(x)  # Count items in current line
                        items = items.union(set(x))  # Add to unique items set
                        line_count += 1
                
                # Calculate density: average items per line divided by total unique items
                density = (nb_items_in_lines/line_count)/len(items)
                print(f"{filename}: {len(items)} unique items, {line_count} transactions, {density} density")
                
                # Create and append statistics for the current file
                row = {
                    "File": filename, 
                    "Density": density, 
                    "Nb_unique_itemset": len(items), 
                    "Nb_transactions": line_count
                }
                res.append(row)
            except Exception as e:
                print(f"Error reading {filename}: {e}")


def compute_metadata(folder_path: str, path:str ="choco.csv", path_to_save:str ="choco_meta.csv"):
    """
    Computes metadata for files in the specified folder and merges with existing data.
    
    This function calculates statistics for all .dat files in the folder_path,
    then merges these statistics with an existing CSV file, saving the result
    to a new CSV file.
    
    Args:
        folder_path (str): Path to the directory containing .dat files
        path (str): Path to the existing CSV file to merge with (default: "choco.csv")
        path_to_save (str): Path where the merged results will be saved (default: "choco_meta.csv")
        
    Returns:
        None: Results are saved to the specified file
    """
    res = []
    statistics(folder_path, res)  # Calculate statistics for all files

    metadata = pd.DataFrame(res)  # Convert statistics to DataFrame
    df = pd.read_csv(path)  # Read existing data
    new = pd.merge(df, metadata, on="File")  # Merge data on the "File" column
    new.to_csv(path_to_save, index=False)  # Save merged data to CSV without index


def columns_preparation(df: pd.DataFrame, save: bool)-> Tuple[pd.DataFrame, pd.core.series.Series, List[str], List[str]]:
    
    #df[LABEL_NAME] = [ "choco" if p>=0.5 else "spmf" for p in np.random.random(df.shape[0])]
    if len(DROP_COLS)>0: df.drop(DROP_COLS, inplace=True, axis=1)
    # Basic exploration
    print("Dataset shape:", df.shape)
    print("\nData types:")
    print(df.dtypes)
    print("\nSummary statistics:")
    print(df.describe())
    print("\nMissing values:")
    print(df.isnull().sum())

    # Visualize the target distribution

    plt.figure(figsize=(6, 6))
    plt.hist(df[LABEL_NAME])
    plt.title(f'Distribution of {LABEL_NAME}')
    if save:
        plt.savefig(f"{SAVED_PIPELINE_FOLDER}/{PLOT_FOLDER}/target_distribution.jpg", format="jpg", dpi=300, bbox_inches='tight')

    # Identify numerical and categorical columns
    # Modify these lists according to your actual dataset
    numeric_features = df.select_dtypes(include=['int64', 'float64']).columns.tolist()
    categorical_features = df.select_dtypes(include=['object']).columns.tolist()

    # Remove the target variable from features
    if LABEL_NAME in categorical_features:
        categorical_features.remove(LABEL_NAME)
    if LABEL_NAME in numeric_features:
        numeric_features.remove(LABEL_NAME)

    # Prepare features and target
    X = df.drop(LABEL_NAME, axis=1)
    y = df[LABEL_NAME]

    return (X, y, numeric_features, categorical_features)