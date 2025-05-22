package com.github.cgks.spmf;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.cgks.Miner;
import com.github.cgks.MiningResult;
import com.github.cgks.exceptions.DatabaseException;
import com.github.cgks.exceptions.MiningException;
import com.github.cgks.exceptions.ParameterException;
import com.github.cgks.spmf.rpgrowth.AlgoRPGrowth;

import ca.pfv.spmf.algorithms.frequentpatterns.apriori_rare.AlgoAprioriRare;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPMax;
import ca.pfv.spmf.algorithms.frequentpatterns.lcm.AlgoLCM;
import ca.pfv.spmf.algorithms.frequentpatterns.lcm.AlgoLCMFreq;
import ca.pfv.spmf.algorithms.frequentpatterns.lcm.Dataset;
import ca.pfv.spmf.algorithms.frequentpatterns.zart.AlgoZart;
import ca.pfv.spmf.algorithms.frequentpatterns.zart.TZTableClosed;
import ca.pfv.spmf.input.transaction_database_list_integers.TransactionDatabase;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

/**
 * The {@code SpmfMiner} class implements the {@link Miner} interface and
 * provides
 * various methods for mining itemsets from datasets using algorithms from the
 * SPMF library.
 * <p>
 * Supported mining tasks include:
 * <ul>
 * <li>Frequent itemset mining</li>
 * <li>Closed itemset mining</li>
 * <li>Maximal itemset mining</li>
 * <li>Rare itemset mining</li>
 * <li>Generator itemset mining</li>
 * <li>Minimal itemset mining</li>
 * <li>Mining itemsets of specific size ranges</li>
 * <li>Mining itemsets containing or excluding specific items</li>
 * </ul>
 * <p>
 * The class provides utility methods for converting file paths to datasets and
 * for filtering itemsets based on presence or absence of items.
 * <p>
 * Each mining method expects a dataset path and a map of parameters, typically
 * including
 * minimum support and, for some methods, item constraints or size constraints.
 * <p>
 * Example usage:
 * 
 * <pre>
 * SpmfMiner miner = new SpmfMiner();
 * Map<String, String> params = new HashMap<>();
 * params.put("minSupport", "0.5");
 * List<MiningResult> results = miner.extractFrequent("dataset.txt", params, () -> false);
 * </pre>
 *
 * @author CGKS team
 * @version 1.0
 */

public class SpmfMiner implements Miner {

    private static final Logger LOGGER = Logger.getLogger(SpmfMiner.class.getName());

    /**
     * Checks if mining operation should be cancelled.
     * @param cancellationChecker Supplier function that returns true if cancellation requested
     * @throws InterruptedException if cancellation detected
     */
    private void checkCancellation(BooleanSupplier cancellationChecker) throws InterruptedException {
        if (cancellationChecker.getAsBoolean()) {
            LOGGER.info("SPMF Mining task cancelled");
            throw new InterruptedException("Mining task was cancelled by user.");
        }
    }

    
    
        /**
     * Extracts frequent itemsets from a dataset using the LCM algorithm.
     * This method implements the extraction of frequent patterns according to
     * the specified minimum support threshold.
     *
     * @param datasetPath The file path to the dataset to be analyzed
     * @param params A map containing algorithm parameters, must include "minSupport"
     * @param cancellationChecker A supplier that returns true if the operation should be cancelled
     * @return A list of mining results containing the discovered frequent itemsets
     * @throws MiningException If any error occurs during the mining process
     * @throws ParameterException If required parameters are missing or invalid
     * @throws DatabaseException If there is an issue with the dataset
     */
    @Override
    public List<MiningResult> extractFrequent(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws  MiningException, ParameterException, DatabaseException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that required parameters are present
            validateParams(params, "minSupport");
            
            // Load dataset from the provided path
            Dataset dataset = pathToDataset(datasetPath);
            
            // Parse the minimum support threshold parameter
            double minSupportThreshold = parseMinSupport(params);
            
            // Initialize the LCM Frequent Itemsets algorithm
            AlgoLCMFreq algorithm = new AlgoLCMFreq();
            
            // Check if operation has been cancelled before running algorithm
            checkCancellation(cancellationChecker);
            
            // Execute the algorithm with the specified parameters
            Itemsets discoveredItemsets = algorithm.runAlgorithm(minSupportThreshold, dataset, null);
            
            // Check if operation has been cancelled before processing results
            checkCancellation(cancellationChecker);
            
            // Convert the SPMF-specific format to the application's result format
            return ConvertToMiningResult.convertItemsetsToMiningResults(discoveredItemsets);
        } catch (InterruptedException e) {
            // Restore the interrupted status
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            // Check if the operation was cancelled during execution
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            // Handle unexpected errors
            throw new MiningException("Unexpected error in extractFrequent: " + e.getMessage(), e);
        }
    }

        /**
     * Extracts closed itemsets from a dataset using the LCM algorithm.
     * Closed itemsets are itemsets that have no superset with the same support,
     * making them a concise representation of frequent patterns.
     *
     * @param datasetPath The file path to the dataset to be analyzed
     * @param params A map containing algorithm parameters, must include "minSupport"
     * @param cancellationChecker A supplier that returns true if the operation should be cancelled
     * @return A list of mining results containing the discovered closed itemsets
     * @throws MiningException If any error occurs during the mining process
     * @throws ParameterException If required parameters are missing or invalid
     * @throws DatabaseException If there is an issue with the dataset
     */
    @Override
    public List<MiningResult> extractClosed(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws  MiningException, ParameterException, DatabaseException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that required parameters are present
            validateParams(params, "minSupport");
            
            // Load dataset from the provided path
            Dataset dataset = pathToDataset(datasetPath);
            
            // Parse the minimum support threshold parameter
            double minSupportThreshold = parseMinSupport(params);
            
            // Initialize the LCM Closed Itemsets algorithm
            AlgoLCM algorithm = new AlgoLCM();
            
            // Check if operation has been cancelled before running algorithm
            checkCancellation(cancellationChecker);
            
            // Execute the algorithm with the specified parameters
            Itemsets closedItemsets = algorithm.runAlgorithm(minSupportThreshold, dataset, null);
            
            // Check if operation has been cancelled before processing results
            checkCancellation(cancellationChecker);
            
            // Convert the SPMF-specific format to the application's result format
            return ConvertToMiningResult.convertItemsetsToMiningResults(closedItemsets);
        } catch (InterruptedException e) {
            // Restore the interrupted status
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            // Check if the operation was cancelled during execution
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            // Handle unexpected errors
            throw new MiningException("Unexpected error in extractClosed: " + e.getMessage(), e);
        }
    }


        /**
     * Extracts maximal itemsets from a dataset using the FPMax algorithm.
     * Maximal itemsets are frequent itemsets that have no frequent supersets,
     * providing a compact representation of the frequent pattern space.
     *
     * @param datasetPath The file path to the dataset to be analyzed
     * @param params A map containing algorithm parameters, must include "minSupport"
     * @param cancellationChecker A supplier that returns true if the operation should be cancelled
     * @return A list of mining results containing the discovered maximal itemsets
     * @throws MiningException If any error occurs during the mining process
     * @throws ParameterException If required parameters are missing or invalid
     * @throws DatabaseException If there is an issue with the dataset
     */
    @Override
    public List<MiningResult> extractMaximal(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws  MiningException, ParameterException, DatabaseException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that required parameters are present
            validateParams(params, "minSupport");
            
            // Parse the minimum support threshold parameter
            double minSupportThreshold = parseMinSupport(params);
            
            // Resolve the path to the dataset file
            String resolvedFilePath = fileToPath(datasetPath);
            
            // Initialize the FPMax algorithm for maximal itemset mining
            AlgoFPMax algorithm = new AlgoFPMax();
            
            // Check if operation has been cancelled before running algorithm
            checkCancellation(cancellationChecker);
            
            // Execute the algorithm with the specified parameters
            Itemsets maximalItemsets = algorithm.runAlgorithm(resolvedFilePath, null, minSupportThreshold);
            
            // Check if operation has been cancelled before processing results
            checkCancellation(cancellationChecker);
            
            // Convert the SPMF-specific format to the application's result format
            return ConvertToMiningResult.convertItemsetsToMiningResults(maximalItemsets);
        } catch (InterruptedException e) {
            // Restore the interrupted status
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            // Check if the operation was cancelled during execution
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            // Handle unexpected errors
            throw new MiningException("Unexpected error in extractMaximal: " + e.getMessage(), e);
        }
    }


        /**
     * Extracts rare itemsets from a dataset using the RPGrowth algorithm.
     * Rare itemsets are those with a support lower than the specified maximum support threshold,
     * representing patterns that occur infrequently in the dataset.
     *
     * @param datasetPath The file path to the dataset to be analyzed
     * @param params A map containing algorithm parameters, must include "maxSupport"
     * @param cancellationChecker A supplier that returns true if the operation should be cancelled
     * @return A list of mining results containing the discovered rare itemsets
     * @throws MiningException If any error occurs during the mining process
     * @throws ParameterException If required parameters are missing or invalid
     * @throws DatabaseException If there is an issue with the dataset
     */
    @Override
    public List<MiningResult> extractRare(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws  MiningException, ParameterException, DatabaseException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that required parameters are present (RPGrowth uses maxSupport)
            validateParams(params, "maxSupport");
            
            // Parse the maximum support threshold parameter
            double maxSupportThreshold = parseMaxSupport(params);
            
            // Initialize the RPGrowth algorithm for rare itemset mining
            AlgoRPGrowth algorithm = new AlgoRPGrowth();
            
            // Check if operation has been cancelled before running algorithm
            checkCancellation(cancellationChecker);
            
            // Execute the algorithm with the specified parameters
            // Last parameter 0 represents minimum support (0 to get all rare itemsets)
            Itemsets rareItemsets = algorithm.runAlgorithm(
                fileToPath(datasetPath), 
                null, 
                maxSupportThreshold, 
                0 // Minimum rare support threshold of 0
            );
            
            // Check if operation has been cancelled before processing results
            checkCancellation(cancellationChecker);
            
            // Convert the SPMF-specific format to the application's result format
            return ConvertToMiningResult.convertItemsetsToMiningResults(rareItemsets);
        } catch (InterruptedException e) {
            // Restore the interrupted status
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            // Check if the operation was cancelled during execution
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            // Handle unexpected errors
            throw new MiningException("Unexpected error in extractRare: " + e.getMessage(), e);
        }
    }


        /**
     * Extracts minimal generators from a dataset using the ZART algorithm.
     * Minimal generators are minimal itemsets that determine a closed itemset,
     * providing a non-redundant representation of association rules.
     *
     * @param datasetPath The file path to the dataset to be analyzed
     * @param params A map containing algorithm parameters, must include "minSupport"
     * @param cancellationChecker A supplier that returns true if the operation should be cancelled
     * @return A list of mining results containing the discovered minimal generators
     * @throws MiningException If any error occurs during the mining process
     * @throws ParameterException If required parameters are missing or invalid
     * @throws DatabaseException If there is an issue with the dataset
     */
    @Override
    public List<MiningResult> extractGenerators(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws  MiningException, ParameterException, DatabaseException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that required parameters are present
            validateParams(params, "minSupport");
            
            // Load and prepare transaction database from the provided path
            TransactionDatabase transactionDatabase = readTransactionDatabase(datasetPath);
            
            // Parse the minimum support threshold parameter
            double minSupportThreshold = parseMinSupport(params);
            
            // Initialize the ZART algorithm for mining closed itemsets and their generators
            AlgoZart algorithm = new AlgoZart();
            
            // Check if operation has been cancelled before running algorithm
            checkCancellation(cancellationChecker);
            
            // Execute the algorithm with the specified parameters
            TZTableClosed algorithmResults = algorithm.runAlgorithm(transactionDatabase, minSupportThreshold);
            
            // Check if operation has been cancelled before processing results
            checkCancellation(cancellationChecker);
            
            // Create container for generator itemsets
            Itemsets generatorItemsets = new Itemsets("Generator itemset");
            
            // Process the results, extracting generators for each level except empty set level 0
            for (int level = 1; level < algorithmResults.levels.size(); level++) {
                // Check if operation has been cancelled during processing
                checkCancellation(cancellationChecker);
                
                // Process each closed itemset at current level
                for (Itemset closedItemset : algorithmResults.levels.get(level)) {
                    // Get the generators associated with this closed itemset
                    List<Itemset> generators = algorithmResults.mapGenerators.get(closedItemset);
                    
                    // If generators exist for this closed itemset, add them all
                    if (!generators.isEmpty()) {
                        for (Itemset generator : generators) {
                            generatorItemsets.addItemset(generator, level);
                        }
                    } else {
                        // If no generators, the closed itemset itself is a generator
                        generatorItemsets.addItemset(closedItemset, level);
                    }
                }
            }
            
            // Convert the SPMF-specific format to the application's result format
            return ConvertToMiningResult.convertItemsetsToMiningResults(generatorItemsets);
        } catch (InterruptedException e) {
            // Restore the interrupted status
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            // Check if the operation was cancelled during execution
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            // Handle unexpected errors
            throw new MiningException("Unexpected error in extractGenerators: " + e.getMessage(), e);
        }
    }


        /**
     * Extracts minimal rare itemsets from a dataset using the Apriori-Rare algorithm.
     * Minimal rare itemsets are those that are rare (with support below the maximum threshold)
     * but all their proper subsets are frequent, providing the most concise representation
     * of rare patterns.
     *
     * @param datasetPath The file path to the dataset to be analyzed
     * @param params A map containing algorithm parameters, must include "maxSupport"
     * @param cancellationChecker A supplier that returns true if the operation should be cancelled
     * @return A list of mining results containing the discovered minimal rare itemsets
     * @throws MiningException If any error occurs during the mining process
     * @throws ParameterException If required parameters are missing or invalid
     * @throws DatabaseException If there is an issue with the dataset
     */
    @Override
    public List<MiningResult> extractMinimal(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException, ParameterException, DatabaseException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that required parameters are present
            validateParams(params, "maxSupport");
            
            // Parse the maximum support threshold parameter
            double maxSupportThreshold = parseMaxSupport(params);
            
            // Initialize the Apriori-Rare algorithm for minimal rare itemset mining
            AlgoAprioriRare algorithm = new AlgoAprioriRare();
            
            // Check if operation has been cancelled before running algorithm
            checkCancellation(cancellationChecker);
            
            // Execute the algorithm with the specified parameters
            Itemsets minimalRareItemsets = algorithm.runAlgorithm(
                maxSupportThreshold, 
                fileToPath(datasetPath), 
                null // No output file specified, results kept in memory
            );
            
            // Check if operation has been cancelled before processing results
            checkCancellation(cancellationChecker);
            
            // Convert the SPMF-specific format to the application's result format
            return ConvertToMiningResult.convertItemsetsToMiningResults(minimalRareItemsets);
        } catch (InterruptedException e) {
            // Restore the interrupted status
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            // Check if the operation was cancelled during execution
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            // Handle unexpected errors
            throw new MiningException("Unexpected error in extractMinimal: " + e.getMessage(), e);
        }
    }


        /**
     * Extracts itemsets with sizes falling within a specified range using the LCM algorithm.
     * This method first mines closed itemsets, then filters them based on their size,
     * keeping only those within the specified size range.
     *
     * @param datasetPath The file path to the dataset to be analyzed
     * @param params A map containing algorithm parameters, must include "minSize", "maxSize", and "minSupport"
     * @param cancellationChecker A supplier that returns true if the operation should be cancelled
     * @return A list of mining results containing the discovered itemsets within the size range
     * @throws MiningException If any error occurs during the mining process
     * @throws ParameterException If required parameters are missing or invalid
     * @throws DatabaseException If there is an issue with the dataset
     */
    @Override
    public List<MiningResult> extractSizeBetween(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws  MiningException, ParameterException, DatabaseException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that all required parameters are present
            validateParams(params, "minSize", "maxSize", "minSupport");
            
            // Load and prepare dataset from the provided path
            Dataset dataset = pathToDataset(datasetPath);
            int datasetSize = dataset.getTransactions().size();
            
            // Parse the minimum support threshold parameter
            double minSupportThreshold = parseMinSupport(params);

            // Parse and validate the size range parameters
            int minimumItemsetSize, maximumItemsetSize;
            try {
                minimumItemsetSize = Integer.parseInt(params.get("minSize"));
                maximumItemsetSize = Integer.parseInt(params.get("maxSize"));
            } catch (NumberFormatException e) {
                throw new ParameterException("Invalid minSize or maxSize parameters: " + e.getMessage());
            }

            // Validate minimum size constraint
            if (minimumItemsetSize < 1) {
                throw new ParameterException("minSize must be at least 1");
            }

            // Validate that maximum size is greater than or equal to minimum size
            if (maximumItemsetSize < minimumItemsetSize) {
                throw new ParameterException("maxSize must be greater than or equal to minSize");
            }

            // Cap the maximum size to the dataset size if necessary
            if (maximumItemsetSize > datasetSize) {
                LOGGER.warning("maxSize is greater than the number of items in the database. " +
                        "Setting maxSize to the number of items: " + datasetSize);
                maximumItemsetSize = datasetSize;
            }

            // Initialize the LCM algorithm for closed itemset mining
            AlgoLCM algorithm = new AlgoLCM();
            
            // Check if operation has been cancelled before running algorithm
            checkCancellation(cancellationChecker);
            
            // Execute the algorithm to find all closed itemsets
            Itemsets allClosedItemsets = algorithm.runAlgorithm(minSupportThreshold, dataset, null);
            
            // Check if operation has been cancelled before filtering results
            checkCancellation(cancellationChecker);

            // Create a new container for filtered itemsets
            Itemsets filteredItemsets = new Itemsets(
                String.format("Itemsets de taille %d à %d", minimumItemsetSize, maximumItemsetSize)
            );
            
            // Filter itemsets based on size criteria
            for (List<Itemset> level : allClosedItemsets.getLevels()) {
                checkCancellation(cancellationChecker);
                for (Itemset itemset : level) {
                    int itemsetSize = itemset.size();
                    if (itemsetSize >= minimumItemsetSize && itemsetSize <= maximumItemsetSize) {
                        filteredItemsets.addItemset(itemset, itemsetSize);
                    }
                }
            }
            
            // Convert the SPMF-specific format to the application's result format
            return ConvertToMiningResult.convertItemsetsToMiningResults(filteredItemsets);
        } catch (InterruptedException e) {
            // Restore the interrupted status
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            // Check if the operation was cancelled during execution
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            // Handle unexpected errors
            throw new MiningException("Unexpected error in extractSizeBetween: " + e.getMessage(), e);
        }
    }


    /**
     * Extracts itemsets that contain specific items of interest using the LCM algorithm.
     * This method mines closed itemsets, then filters them to keep only those containing all specified items.
     *
     * @param datasetPath The file path to the dataset to be analyzed
     * @param params A map containing algorithm parameters, must include "minSupport" and "items"
     * @param cancellationChecker A supplier that returns true if the operation should be cancelled
     * @return A list of mining results containing the discovered itemsets that contain all required items
     * @throws MiningException If any error occurs during the mining process
     * @throws ParameterException If required parameters are missing or invalid
     * @throws DatabaseException If there is an issue with the dataset
     */
    @Override
    public List<MiningResult> extractPresence(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException, ParameterException, DatabaseException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that all required parameters are present
            validateParams(params, "minSupport", "items");
            
            // Parse the minimum support threshold parameter
            double minSupportThreshold = parseMinSupport(params);
            
            // Parse the list of items that must be present in results
            String itemsParameter = params.get("items");
            List<Integer> requiredItems = parseItems(itemsParameter);
            
            // Load and prepare dataset from the provided path
            Dataset dataset = pathToDataset(datasetPath);
            
            // Initialize the LCM algorithm for closed itemset mining
            AlgoLCM algorithm = new AlgoLCM();
            
            // Check if operation has been cancelled before running algorithm
            checkCancellation(cancellationChecker);
            
            // Execute the algorithm to find all closed itemsets
            Itemsets allClosedItemsets = algorithm.runAlgorithm(minSupportThreshold, dataset, null);
            
            // Check if operation has been cancelled before filtering results
            checkCancellation(cancellationChecker);

            // Create a new container for filtered itemsets
            Itemsets filteredItemsets = new Itemsets("Itemsets contenant tous: " + requiredItems);

            // Get the set of all unique items in the dataset
            Set<Integer> datasetItems = dataset.getUniqueItems();

            // Filter required items to only include those present in the dataset
            List<Integer> filteredRequiredItems = requiredItems.stream()
                    .filter(datasetItems::contains)
                    .collect(Collectors.toList());

            // Special case: if no required items remain after filtering, return all itemsets
            if (filteredRequiredItems.isEmpty()) {
                return ConvertToMiningResult.convertItemsetsToMiningResults(allClosedItemsets);
            }

            // Sort the required items list for more efficient containment checking
            List<Integer> sortedRequiredItems = new ArrayList<>(filteredRequiredItems);
            Collections.sort(sortedRequiredItems);

            // Filter itemsets to keep only those containing all required items
            for (List<Itemset> level : allClosedItemsets.getLevels()) {
                checkCancellation(cancellationChecker);
                for (Itemset itemset : level) {
                    if (containsAllRequired(itemset, sortedRequiredItems)) {
                        filteredItemsets.addItemset(itemset, itemset.size());
                    }
                }
            }

            // Convert the SPMF-specific format to the application's result format
            return ConvertToMiningResult.convertItemsetsToMiningResults(filteredItemsets);
        } catch (InterruptedException e) {
            // Restore the interrupted status
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            // Check if the operation was cancelled during execution
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            // Handle unexpected errors
            throw new MiningException("Unexpected error in extractPresence: " + e.getMessage(), e);
        }
    }


    /**
     * Extracts itemsets that do not contain any of the specified excluded items using the LCM algorithm.
     * This method mines closed itemsets, then filters them to keep only those containing none of the excluded items.
     *
     * @param datasetPath The file path to the dataset to be analyzed
     * @param params A map containing algorithm parameters, must include "minSupport" and "items" (to exclude)
     * @param cancellationChecker A supplier that returns true if the operation should be cancelled
     * @return A list of mining results containing the discovered itemsets that exclude all specified items
     * @throws MiningException If any error occurs during the mining process
     * @throws ParameterException If required parameters are missing or invalid
     * @throws DatabaseException If there is an issue with the dataset
     */
    @Override
    public List<MiningResult> extractAbsence(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException, ParameterException, DatabaseException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that all required parameters are present
            validateParams(params, "minSupport", "items");
            
            // Parse the minimum support threshold parameter
            double minSupportThreshold = parseMinSupport(params);
            
            // Parse the list of items that must be absent from results
            List<Integer> excludedItems = parseItems(params.get("items"));
            
            // Load and prepare dataset from the provided path
            Dataset dataset = pathToDataset(datasetPath);

            // Initialize the LCM algorithm for closed itemset mining
            AlgoLCM algorithm = new AlgoLCM();
            
            // Check if operation has been cancelled before running algorithm
            checkCancellation(cancellationChecker);
            
            // Execute the algorithm to find all closed itemsets
            Itemsets allClosedItemsets = algorithm.runAlgorithm(minSupportThreshold, dataset, null);
            
            // Check if operation has been cancelled before filtering results
            checkCancellation(cancellationChecker);

            // Create a new container for filtered itemsets
            Itemsets filteredItemsets = new Itemsets("Itemsets sans: " + excludedItems);

            // Special case: if no excluded items, return all itemsets
            if (excludedItems.isEmpty()) {
                return ConvertToMiningResult.convertItemsetsToMiningResults(allClosedItemsets);
            }

            // Sort the excluded items list for more efficient containment checking
            List<Integer> sortedExcludedItems = new ArrayList<>(excludedItems);
            Collections.sort(sortedExcludedItems);

            // Filter itemsets to keep only those not containing any excluded items
            for (List<Itemset> level : allClosedItemsets.getLevels()) {
                checkCancellation(cancellationChecker);
                for (Itemset itemset : level) {
                    if (!containsAny(itemset.getItems(), sortedExcludedItems)) {
                        filteredItemsets.addItemset(itemset, itemset.size());
                    }
                }
            }

            // Convert the SPMF-specific format to the application's result format
            return ConvertToMiningResult.convertItemsetsToMiningResults(filteredItemsets);
        } catch (InterruptedException e) {
            // Restore the interrupted status
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            // Check if the operation was cancelled during execution
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            // Handle unexpected errors
            throw new MiningException("Unexpected error in extractAbsence: " + e.getMessage(), e);
        }
    }


        /**
     * Determines if an itemset contains all the required items.
     * This method checks whether every item in the requiredItems list is present in the given itemset.
     * 
     * @param itemset The itemset to check, containing items to be verified
     * @param requiredItems A sorted list of items that must all be present in the itemset
     * @return true if the itemset contains all required items, false otherwise
     */
    private static boolean containsAllRequired(Itemset itemset, List<Integer> requiredItems) {
        // Convert the primitive int array to a List<Integer> for easier comparison
        List<Integer> itemsetItems = Arrays.stream(itemset.getItems())
                .boxed()
                .collect(Collectors.toList());
        
        // Check if all required items are present in the itemset
        return itemsetItems.containsAll(requiredItems);
    }

    /**
     * Determines if an array of items contains any of the excluded items.
     * This method uses binary search for efficient checking, assuming the excludedItems list is sorted.
     * 
     * @param items The array of items to check against the excluded items
     * @param excludedItems A sorted list of items that should be absent from the array
     * @return true if any excluded item is found in the array, false if none are present
     */
    private static boolean containsAny(int[] items, List<Integer> excludedItems) {
        // Use binary search for efficient containment checking
        // This requires that excludedItems is already sorted
        return Arrays.stream(items)
                .anyMatch(item -> Collections.binarySearch(excludedItems, item) >= 0);
    }


    /**
     * Converts a resource file path to an absolute file system path.
     * This method locates the resource using the class loader and decodes its URL
     * path.
     *
     * @param file The relative path to the resource file (e.g., "/data/input.txt")
     * @return The absolute file system path to the resource
     * @throws DatabaseException If the resource cannot be found or decoded
     */
    private static String fileToPath(String file) throws DatabaseException {
        try {
            URL url = TestSPMF.class.getResource(file);
            return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
        } catch (Exception e) {
            throw new DatabaseException("Error loading dataset: " + file + e.getMessage(), e);
        }
    }

    /**
     * Loads a dataset from the specified path and returns a Dataset object.
     * This method resolves the file path and initializes the Dataset.
     *
     * @param path The relative path to the dataset file
     * @return The loaded Dataset object
     * @throws DatabaseException If the dataset cannot be loaded or parsed
     */
    private static Dataset pathToDataset(String path) throws DatabaseException {
        try {
            String input = fileToPath(path);
            Dataset dataset = new Dataset(input);
            return dataset;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DatabaseException("Error loading dataset : " + path + e.getMessage(), e);
        }
    }

    /**
     * Loads a transaction database from the specified dataset path.
     * This method reads the file and initializes a TransactionDatabase object.
     *
     * @param datasetPath The relative path to the dataset file
     * @return The loaded TransactionDatabase object
     * @throws DatabaseException If the file cannot be loaded or read
     */
    private static TransactionDatabase readTransactionDatabase(String datasetPath) throws DatabaseException {
        try {
            TransactionDatabase context = new TransactionDatabase();
            context.loadFile(fileToPath(datasetPath));
            return context;
        } catch (IOException e) {
            throw new DatabaseException("Error loading dataset: " + datasetPath + e.getMessage(), e);
        }
    }

    /**
     * Validates that required parameters are present.
     * * @param params The parameters map
     * 
     * @param requiredParams The required parameter names
     * @throws ParameterException If a required parameter is missing
     */
    private void validateParams(Map<String, String> params, String... requiredParams) throws ParameterException {
        for (String param : requiredParams) {
            if (!params.containsKey(param) || params.get(param) == null || params.get(param).trim().isEmpty()) {
                throw new ParameterException("Required parameter '" + param + "' is missing or empty");
            }
        }
    }

    /**
     * Validates and parses the minimum support parameter.
     *
     * @param params   The parameters map
     * @param database The transactional database
     * @return The calculated minimum support value
     * @throws ParameterException If the minSupport parameter is invalid
     */
    private Double parseMinSupport(Map<String, String> params) throws ParameterException {
        try {
            Double minSupportRatio = Double.parseDouble(params.get("minSupport"));
            if (minSupportRatio <= 0.0 || minSupportRatio > 1.0) {
                throw new ParameterException("minSupport must be between 0.0 (exclusive) and 1.0 (inclusive)");
            }
            return minSupportRatio;
        } catch (NumberFormatException e) {
            throw new ParameterException("Invalid minSupport value: " + params.get("minSupport"));
        }
    }

    /**
     * Validates and parses the maximum support parameter.
     *
     * @param params The parameters map
     * @return The calculated maximum support value
     * @throws ParameterException If the maxSupport parameter is invalid
     */
    private Double parseMaxSupport(Map<String, String> params) throws ParameterException {
        try {
            Double maxSupportRatio = Double.parseDouble(params.get("maxSupport"));
            if (maxSupportRatio <= 0.0 || maxSupportRatio > 1.0) {
                throw new ParameterException("maxSupport must be between 0.0 (exclusive) and 1.0 (inclusive)");
            }
            return maxSupportRatio;
        } catch (NumberFormatException e) {
            throw new ParameterException("Invalid maxSupport value: " + params.get("maxSupport"));
        }
    }

    /**
     * Validates and parses the required/exculded items.
     *
     * @param param The items parameter
     * @return An array of integers representing the items
     * @throws ParameterException If the items parameter is invalid
     */
    private List<Integer> parseItems(String param) {
        List<Integer> result = new ArrayList<>();
        for (String item : param.split(",")) {
            String trimmed = item.trim();
            try {
                result.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException e) {
                LOGGER.warning("Skipping invalid item: " + trimmed);
            }
        }
        return result;
    }
}