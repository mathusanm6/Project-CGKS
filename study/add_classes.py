import os
import pandas as pd
from typing import Dict, List, Tuple, Set, Optional, Union
from global_var import (
    DATA_FOLDER,
    DATASETS_FOLDER,
    SAVED_PIPELINE_FOLDER

)

def calculate_file_statistics(data_folder: str) -> Dict[str, List[Union[int, float]]]:
    """
    Calculate statistics for all .dat files in the specified folder.
    
    Args:
        data_folder (str): Path to the directory containing .dat files
        
    Returns:
        Dict[str, List[Union[int, float]]]: Dictionary with filename as key and 
            [number of unique items, number of transactions, density] as value
    """
    stats = {}
    
    if not os.path.exists(data_folder):
        print(f"Data folder '{data_folder}' does not exist.")
        return stats
        
    for filename in os.listdir(data_folder):
        if filename.endswith(".dat"):
            file_path = os.path.join(data_folder, filename)
            try:
                unique_items = set()
                transaction_count = 0
                total_item_count = 0

                with open(file_path, 'r', encoding='utf-8', errors='ignore') as file:
                    for line in file:
                        tokens = line.strip().split()
                        total_item_count += len(tokens)
                        unique_items.update(tokens)
                        transaction_count += 1

                # Calculate density if the file has transactions, otherwise default to 0
                if transaction_count > 0:
                    density = (total_item_count / transaction_count) / len(unique_items)
                else:
                    density = 0.0

                # Store stats: [number of unique items, number of transactions, density]
                stats[filename] = [len(unique_items), transaction_count, density]

            except Exception as e:
                print(f"Error reading file {filename}: {e}")
                
    return stats


def determine_class(
    duration1: float, 
    duration2: float, 
    itemset1: int, 
    itemset2: int,
    time_threshold: float = 15000
) -> int:
    """
    Determine which algorithm performed better based on execution time and itemsets.
    
    Args:
        duration1 (float): Execution time of the first algorithm
        duration2 (float): Execution time of the second algorithm
        itemset1 (int): Number of itemsets found by the first algorithm
        itemset2 (int): Number of itemsets found by the second algorithm
        time_threshold (float): Threshold to determine if execution times are comparable
        
    Returns:
        int: 0 if the first algorithm performed better, 1 if the second did
    """
    if duration1 < time_threshold and duration2 < time_threshold:
        return 0 if duration1 < duration2 else 1
    else:
        return 0 if itemset1 > itemset2 else 1


def create_comparison_dataset(
    choco_file_path: str = f'{DATASETS_FOLDER}/choco.csv', 
    spmf_file_path: str = f'{DATASETS_FOLDER}/spmf.csv',
    data_folder: str = f'src/main/resources/data',
    output_file_path: str = f'{DATASETS_FOLDER}/classes.csv'
) -> pd.DataFrame:
    """
    Create a dataset comparing the performance of two algorithms.
    
    Args:
        choco_file_path (str): Path to the CSV file with choco algorithm results
        spmf_file_path (str): Path to the CSV file with spmf algorithm results
        data_folder (str): Path to the folder with .dat files
        output_file_path (str): Path where the output CSV will be saved
        
    Returns:
        pd.DataFrame: The generated comparison dataset
    """
    # Calculate statistics for all .dat files
    file_stats = calculate_file_statistics(data_folder)
    
    # Create DataFrame to store results
    columns = ['Query', 'File', 'Frequency', 'Nbitems', 'Nbtransactions', 'Density', 'Class']
    results_df = pd.DataFrame(columns=columns)
    
    # Read input files
    try:
        with open(choco_file_path, 'r') as f1, open(spmf_file_path, 'r') as f2:
            choco_lines = f1.readlines()
            spmf_lines = f2.readlines()
    except FileNotFoundError as e:
        print(f"Error: Input file not found - {e}")
        return results_df
    
    # Build the comparison dataset
    for i, (choco_line, spmf_line) in enumerate(zip(choco_lines, spmf_lines), start=1):
        try:
            choco_fields = choco_line.strip().split(',')
            spmf_fields = spmf_line.strip().split(',')
            
            # Extract fields from the data
            query = choco_fields[0]
            filename = choco_fields[1]
            frequency = float(choco_fields[3])
            duration_choco = float(choco_fields[4])
            duration_spmf = float(spmf_fields[4])
            itemset_choco = int(choco_fields[2])
            itemset_spmf = int(spmf_fields[2])
            
            if filename not in file_stats:
                print(f"Warning: Statistics not available for file {filename}")
                continue
                
            # Get file statistics
            nb_items, nb_transactions, density = file_stats[filename]
            
            # Determine which algorithm performed better
            result_class = determine_class(
                duration_choco, 
                duration_spmf, 
                itemset_choco, 
                itemset_spmf
            )
            
            # Add row to the results DataFrame
            results_df.loc[len(results_df)] = [
                query, 
                filename, 
                frequency, 
                nb_items, 
                nb_transactions, 
                density, 
                result_class
            ]
            
        except Exception as e:
            print(f"Line {i} skipped due to an error: {e}")
    
    # Save the final DataFrame
    results_df.to_csv(output_file_path, index=False)
    print(f"Comparison dataset saved to {output_file_path}")
    
    return results_df


if __name__ == "__main__":
    create_comparison_dataset()
