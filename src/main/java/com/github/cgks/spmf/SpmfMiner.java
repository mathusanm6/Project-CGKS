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

    private void checkCancellation(BooleanSupplier cancellationChecker) throws InterruptedException {
        if (cancellationChecker.getAsBoolean()) {
            LOGGER.info("SPMF Mining task cancelled");
            throw new InterruptedException("Mining task was cancelled by user.");
        }
    }

    @Override
    public List<MiningResult> extractFrequent(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            checkCancellation(cancellationChecker);
            validateParams(params, "minSupport");
            Dataset dataset = pathToDataset(datasetPath);
            Double minSupport = parseMinSupport(params);
            AlgoLCMFreq algo = new AlgoLCMFreq();
            checkCancellation(cancellationChecker);
            Itemsets itemsets = algo.runAlgorithm(minSupport, dataset, null);
            checkCancellation(cancellationChecker);
            return ConvertToMiningResult.convertItemsetsToMiningResults(itemsets);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            throw new MiningException("Unexpected error in extractFrequent: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MiningResult> extractClosed(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            checkCancellation(cancellationChecker);
            validateParams(params, "minSupport");
            Dataset dataset = pathToDataset(datasetPath);
            Double minSupport = parseMinSupport(params);
            AlgoLCM algo = new AlgoLCM();
            checkCancellation(cancellationChecker);
            Itemsets itemsets = algo.runAlgorithm(minSupport, dataset, null);
            checkCancellation(cancellationChecker);
            return ConvertToMiningResult.convertItemsetsToMiningResults(itemsets);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            throw new MiningException("Unexpected error in extractClosed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MiningResult> extractMaximal(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            checkCancellation(cancellationChecker);
            validateParams(params, "minSupport");
            Double minSupport = parseMinSupport(params);
            String inputPath = fileToPath(datasetPath); // Resolve path once
            AlgoFPMax algo = new AlgoFPMax();
            checkCancellation(cancellationChecker);
            Itemsets itemsets = algo.runAlgorithm(inputPath, null, minSupport);
            checkCancellation(cancellationChecker);
            return ConvertToMiningResult.convertItemsetsToMiningResults(itemsets);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            throw new MiningException("Unexpected error in extractMaximal: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MiningResult> extractRare(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            checkCancellation(cancellationChecker);
            validateParams(params, "maxSupport"); // RPGrowth uses maxSupport
            Double maxSupport = parseMaxSupport(params);
            AlgoRPGrowth algo = new AlgoRPGrowth();
            checkCancellation(cancellationChecker);
            Itemsets itemsets = algo.runAlgorithm(fileToPath(datasetPath), null, maxSupport, 0);
            checkCancellation(cancellationChecker);
            return ConvertToMiningResult.convertItemsetsToMiningResults(itemsets);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            throw new MiningException("Unexpected error in extractRare: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MiningResult> extractGenerators(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            checkCancellation(cancellationChecker);
            validateParams(params, "minSupport");
            TransactionDatabase context = readTransactionDatabase(datasetPath);
            Double minSupport = parseMinSupport(params);
            AlgoZart algo = new AlgoZart();
            checkCancellation(cancellationChecker);
            TZTableClosed results = algo.runAlgorithm(context, minSupport);
            checkCancellation(cancellationChecker);
            Itemsets itemsets = new Itemsets("Generator itemset");
            for (int i = 1; i < results.levels.size(); i++) {
                checkCancellation(cancellationChecker);
                for (Itemset closed : results.levels.get(i)) {
                    List<Itemset> generators = results.mapGenerators.get(closed);
                    // if there are some generators
                    if (generators.size() != 0) {
                        for (Itemset generator : generators) {
                            itemsets.addItemset(generator, i);
                        }
                    } else {
                        // otherwise the closed itemset is a generator
                        itemsets.addItemset(closed, i);
                    }
                }
            }
            return ConvertToMiningResult.convertItemsetsToMiningResults(itemsets);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            throw new MiningException("Unexpected error in extractGenerators: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MiningResult> extractMinimal(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            checkCancellation(cancellationChecker);
            validateParams(params, "maxSupport");
            Double maxSupport = parseMaxSupport(params);
            AlgoAprioriRare algo = new AlgoAprioriRare();
            checkCancellation(cancellationChecker);
            Itemsets itemsets = algo.runAlgorithm(maxSupport, fileToPath(datasetPath), null);
            checkCancellation(cancellationChecker);
            return ConvertToMiningResult.convertItemsetsToMiningResults(itemsets);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            throw new MiningException("Unexpected error in extractMinimal: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MiningResult> extractSizeBetween(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            checkCancellation(cancellationChecker);
            validateParams(params, "minSize", "maxSize", "minSupport");
            Dataset dataset = pathToDataset(datasetPath);
            int datasetSize = dataset.getTransactions().size();
            Double minSupport = parseMinSupport(params);

            int minSize, maxSize;
            try {
                minSize = Integer.parseInt(params.get("minSize"));
                maxSize = Integer.parseInt(params.get("maxSize"));
            } catch (NumberFormatException e) {
                throw new ParameterException("Invalid minSize or maxSize parameters: " + e.getMessage());
            }

            if (minSize < 1) {
                throw new ParameterException("minSize must be at least 1");
            }

            if (maxSize < minSize) {
                throw new ParameterException("maxSize must be greater than or equal to minSize");
            }

            if (maxSize > datasetSize) {
                LOGGER.warning("maxSize is greater than the number of items in the database. " +
                        "Setting maxSize to the number of items: " + datasetSize);
                maxSize = datasetSize;
            }

            AlgoLCM algo = new AlgoLCM();
            checkCancellation(cancellationChecker);
            Itemsets itemsets = algo.runAlgorithm(minSupport, dataset, null);
            checkCancellation(cancellationChecker);

            Itemsets results = new Itemsets(String.format("Itemsets de taille %d à %d", minSize, maxSize));
            for (List<Itemset> level : itemsets.getLevels()) {
                checkCancellation(cancellationChecker);
                for (Itemset itemset : level) {
                    int size = itemset.size();
                    if (size >= minSize && size <= maxSize) {
                        results.addItemset(itemset, size);
                    }
                }
            }
            return ConvertToMiningResult.convertItemsetsToMiningResults(results);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            throw new MiningException("Unexpected error in extractSizeBetween: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MiningResult> extractPresence(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            checkCancellation(cancellationChecker);
            validateParams(params, "minSupport", "items");
            Double minSupport = parseMinSupport(params);
            String param = params.get("items");
            List<Integer> requiredItems = parseItems(param);
            Dataset dataset = pathToDataset(datasetPath);
            AlgoLCM algo = new AlgoLCM();
            checkCancellation(cancellationChecker);
            Itemsets itemsets = algo.runAlgorithm(minSupport, dataset, null);
            checkCancellation(cancellationChecker);

            Itemsets results = new Itemsets("Itemsets contenant tous: " + requiredItems);

            Set<Integer> datasetItems = dataset.getUniqueItems();

            // Filter required items to only include those in the dataset
            List<Integer> filteredRequiredItems = requiredItems.stream()
                    .filter(datasetItems::contains)
                    .collect(Collectors.toList());

            // Cas spécial: si aucun item requis, on retourne tout
            if (filteredRequiredItems == null || filteredRequiredItems.isEmpty()) {
                return ConvertToMiningResult.convertItemsetsToMiningResults(itemsets);
            }

            // Tri des items requis pour la recherche binaire
            List<Integer> sortedRequired = new ArrayList<>(filteredRequiredItems);
            Collections.sort(sortedRequired);

            // Filter itemsets, checking for cancellation periodically
            for (List<Itemset> level : itemsets.getLevels()) {
                checkCancellation(cancellationChecker);
                for (Itemset itemset : level) {
                    if (containsAllRequired(itemset, sortedRequired)) {
                        results.addItemset(itemset, itemset.size());
                    }
                }
            }

            return ConvertToMiningResult.convertItemsetsToMiningResults(results);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            throw new MiningException("Unexpected error in extractPresence: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MiningResult> extractAbsence(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            checkCancellation(cancellationChecker);
            validateParams(params, "minSupport", "items");
            Double minSupport = parseMinSupport(params);
            List<Integer> excludedItems = parseItems(params.get("items"));
            Dataset dataset = pathToDataset(datasetPath);

            AlgoLCM algo = new AlgoLCM();
            checkCancellation(cancellationChecker);
            Itemsets itemsets = algo.runAlgorithm(minSupport, dataset, null);
            checkCancellation(cancellationChecker);

            Itemsets results = new Itemsets("Itemsets sans: " + excludedItems);

            if (excludedItems == null || excludedItems.isEmpty()) {
                return ConvertToMiningResult.convertItemsetsToMiningResults(itemsets);
            }

            List<Integer> sortedExcluded = new ArrayList<>(excludedItems);
            Collections.sort(sortedExcluded);

            for (List<Itemset> level : itemsets.getLevels()) {
                checkCancellation(cancellationChecker);
                for (Itemset itemset : level) {
                    if (!containsAny(itemset.getItems(), sortedExcluded)) {
                        results.addItemset(itemset, itemset.size());
                    }
                }
            }

            return ConvertToMiningResult.convertItemsetsToMiningResults(results);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MiningException("Mining task was cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                throw new MiningException("Mining task was cancelled during operation.", e);
            }
            throw new MiningException("Unexpected error in extractAbsence: " + e.getMessage(), e);
        }
    }

    /**
     * Vérifie si un itemset contient tous les items requis (version optimisée
     * List<Integer>)
     */
    private static boolean containsAllRequired(Itemset itemset, List<Integer> requiredItems) {
        List<Integer> itemsetItems = Arrays.stream(itemset.getItems())
                .boxed()
                .collect(Collectors.toList());
        return itemsetItems.containsAll(requiredItems);
    }

    /**
     * Vérifie si un tableau contient au moins un item interdit (version optimisée
     * List<Integer>)
     */
    private static boolean containsAny(int[] items, List<Integer> excludedItems) {
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
