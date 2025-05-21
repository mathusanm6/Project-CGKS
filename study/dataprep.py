import pandas as pd
import numpy as np
import os
import matplotlib.pyplot as plt
from global_var import DROP_COLS, LABEL_NAME, SAVED_PIPELINE_FOLDER, PLOT_FOLDER
from typing import List, Any, Dict, Tuple, Set


def calculate_file_statistics(folder_path: str, results: List[Dict[str, Any]]) -> None:
    """
    Calculates metadata for each .dat file in the specified folder.
    
    The function iterates through each .dat file, counting unique items, 
    number of transactions, and calculating density. These metadata 
    are added to the provided 'results' list.
    
    Args:
        folder_path (str): Path to the directory containing .dat files
        results (List[Dict[str, Any]]): List to store the calculated metadata for each file
        
    Returns:
        None: Results are appended to the provided 'results' list
    """
    if not os.path.isdir(folder_path):
        print(f"Folder '{folder_path}' does not exist.")
        return
        
    for filename in os.listdir(folder_path):
        if filename.endswith(".dat"):
            file_path = os.path.join(folder_path, filename)
            try:
                unique_items = set()  # Set to store unique items
                transaction_count = 0  # Counter for transactions
                total_item_count = 0  # Total number of items across all lines
                
                with open(file_path, 'r', encoding='utf-8', errors='ignore') as file:
                    for line in file:
                        line = line.strip('\n')
                        items = line.split(" ")  # Split line into items
                        
                        total_item_count += len(items)  # Count items in current line
                        unique_items = unique_items.union(set(items))  # Add to unique items set
                        transaction_count += 1
                
                # Calculate density: average items per line divided by total unique items
                avg_items_per_transaction = total_item_count / transaction_count
                density = avg_items_per_transaction / len(unique_items)
                
                # Create and append statistics for the current file
                file_stats = {
                    "File": filename, 
                    "Density": density, 
                    "Nb_unique_itemset": len(unique_items), 
                    "Nb_transactions": transaction_count
                }
                results.append(file_stats)
            except Exception as e:
                print(f"Error reading {filename}: {e}")


def compute_metadata(folder_path: str, 
                     input_csv_path: str = "choco.csv", 
                     output_csv_path: str = "choco_meta.csv") -> None:
    """
    Computes metadata for files in the specified folder and merges with existing data.
    
    This function calculates statistics for all .dat files in the folder_path,
    then merges these statistics with an existing CSV file, saving the result
    to a new CSV file.
    
    Args:
        folder_path (str): Path to the directory containing .dat files
        input_csv_path (str): Path to the existing CSV file to merge with (default: "choco.csv")
        output_csv_path (str): Path where the merged results will be saved (default: "choco_meta.csv")
        
    Returns:
        None: Results are saved to the specified file
    """
    statistics_results = []
    calculate_file_statistics(folder_path, statistics_results)  # Calculate statistics for all files

    metadata_df = pd.DataFrame(statistics_results)  # Convert statistics to DataFrame
    input_df = pd.read_csv(input_csv_path)  # Read existing data
    merged_df = pd.merge(input_df, metadata_df, on="File")  # Merge data on the "File" column
    merged_df.to_csv(output_csv_path, index=False)  # Save merged data to CSV without index


def columns_preparation(
        dataframe: pd.DataFrame, 
        save: bool = False
    ) -> Tuple[pd.DataFrame, pd.Series, List[str], List[str]]:
    """
    Prepare the dataset by identifying feature types and separating features from target.
    
    Args:
        dataframe (pd.DataFrame): The input DataFrame to process
        save (bool): Whether to save visualization of target distribution
        
    Returns:
        Tuple containing:
        - X (pd.DataFrame): Features DataFrame
        - y (pd.Series): Target variable Series
        - numeric_features (List[str]): List of numeric feature names
        - categorical_features (List[str]): List of categorical feature names
    """
    # Drop specified columns if any
    if len(DROP_COLS) > 0:
        processed_df = dataframe.drop(DROP_COLS, inplace=False, axis=1)
    else:
        processed_df = dataframe.copy()

    # Visualize the target distribution
    plt.figure(figsize=(6, 6))
    plt.hist(processed_df[LABEL_NAME])
    plt.title(f'Distribution of {LABEL_NAME}')
    
    if save:
        save_path = f"{SAVED_PIPELINE_FOLDER}/{PLOT_FOLDER}/target_distribution.jpg"
        os.makedirs(os.path.dirname(save_path), exist_ok=True)
        plt.savefig(save_path, format="jpg", dpi=300, bbox_inches='tight')
        plt.close()

    # Identify numerical and categorical columns
    numeric_features = processed_df.select_dtypes(include=['int64', 'float64']).columns.tolist()
    categorical_features = processed_df.select_dtypes(include=['object']).columns.tolist()

    # Remove the target variable from features
    if LABEL_NAME in categorical_features:
        categorical_features.remove(LABEL_NAME)
    if LABEL_NAME in numeric_features:
        numeric_features.remove(LABEL_NAME)

    # Prepare features and target
    features = processed_df.drop(LABEL_NAME, axis=1)
    target = processed_df[LABEL_NAME]

    return features, target, numeric_features, categorical_features
