package com.github.cgks.choco;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import com.github.cgks.Miner;
import com.github.cgks.MiningResult;
import com.github.cgks.exceptions.DatabaseException;
import com.github.cgks.exceptions.ParameterException;
import com.github.cgks.exceptions.MiningException;

import io.gitlab.chaver.mining.patterns.constraints.factory.ConstraintFactory;
import io.gitlab.chaver.mining.patterns.io.DatReader;
import io.gitlab.chaver.mining.patterns.io.TransactionalDatabase;

/**
 * Implementation of the Miner interface using Choco Solver.
 */
public class ChocoMiner implements Miner {

    private static final Logger LOGGER = Logger.getLogger(ChocoMiner.class.getName());

    /**
     * Helper method to check for cancellation and interrupt if needed.
     * 
     * @param cancellationChecker The supplier to check for cancellation.
     * @throws InterruptedException If cancellation is requested.
     */
    private void checkCancellation(BooleanSupplier cancellationChecker) throws InterruptedException {
        if (cancellationChecker.getAsBoolean()) {
            LOGGER.info("Cancellation requested, interrupting mining process.");
            throw new InterruptedException("Mining process cancelled by user.");
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
    private int parseMinSupport(Map<String, String> params, TransactionalDatabase database) throws ParameterException {
        try {
            double minSupportRatio = Double.parseDouble(params.get("minSupport"));
            if (minSupportRatio <= 0.0 || minSupportRatio > 1.0) {
                throw new ParameterException("minSupport must be between 0.0 (exclusive) and 1.0 (inclusive)");
            }
            return (int) Math.ceil(database.getNbTransactions() * minSupportRatio);
        } catch (NumberFormatException e) {
            throw new ParameterException("Invalid minSupport value: " + params.get("minSupport"));
        }
    }

    /**
     * Validates and parses the maximum support parameter.
     *
     * @param params   The parameters map
     * @param database The transactional database
     * @return The calculated maximum support value
     * @throws ParameterException If the maxSupport parameter is invalid
     */
    private int parseMaxSupport(Map<String, String> params, TransactionalDatabase database) throws ParameterException {
        try {
            double maxSupportRatio = Double.parseDouble(params.get("maxSupport"));
            if (maxSupportRatio <= 0.0 || maxSupportRatio > 1.0) {
                throw new ParameterException("maxSupport must be between 0.0 (exclusive) and 1.0 (inclusive)");
            }
            return (int) Math.ceil(database.getNbTransactions() * maxSupportRatio);
        } catch (NumberFormatException e) {
            throw new ParameterException("Invalid maxSupport value: " + params.get("maxSupport"));
        }
    }

        /**
     * Extracts frequent itemsets from a dataset using a constraint programming approach.
     * This method implements the mining process using the Choco Solver constraint programming library
     * to find all itemsets that meet the minimum support threshold.
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
            BooleanSupplier cancellationChecker) throws MiningException, ParameterException, DatabaseException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that all required parameters are present
            validateParams(params, "minSupport");

            // Load the dataset as a transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            
            // Parse and validate the minimum support threshold
            int minSupportCount = parseMinSupport(params, database);

            LOGGER.info("Starting frequent itemset mining with minSupport: " + minSupportCount);

            // Create a constraint programming model for mining
            Model model = new Model("Frequent Itemset Mining");
            
            // Define decision variables:
            // - x[i] is true if item i is in the itemset
            BoolVar[] itemSelectionVars = model.boolVarArray("x", database.getNbItems());
            
            // - freq represents the support count (number of transactions covering the itemset)
            IntVar supportCountVar = model.intVar("freq", 1, database.getNbTransactions());

            // Post the minimum support constraint
            model.arithm(supportCountVar, ">=", minSupportCount).post();
            
            // Post the coverage constraint (defines the relationship between items and their frequency)
            ConstraintFactory.coverSize(database, supportCountVar, itemSelectionVars).post();

            // Configure the solver
            Solver solver = model.getSolver();
            solver.setSearch(Search.inputOrderLBSearch(itemSelectionVars));
            List<MiningResult> results = new ArrayList<>();

            try {
                // Find all solutions (itemsets) that satisfy the constraints
                while (solver.solve()) {
                    // Check for cancellation after each solution
                    checkCancellation(cancellationChecker);
                    
                    // Extract the current solution as a mining result
                    MiningResult result = getMiningResult(database, itemSelectionVars, supportCountVar);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Frequent itemset mining completed. Found " + results.size() + " results.");

            // Remove any empty itemsets from the results
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Frequent itemset mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("Frequent itemset mining cancelled during operation.");
                throw new MiningException("Mining cancelled by user.", e);
            }
            throw new MiningException("Unexpected error in extractFrequent: " + e.getMessage(), e);
        }
    }


        /**
     * Extracts closed itemsets from a dataset using a constraint programming approach.
     * A closed itemset is a frequent itemset that has no proper superset with the same support.
     * This method uses the Choco Solver constraint programming library to find all closed itemsets
     * that meet the minimum support threshold.
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
            BooleanSupplier cancellationChecker) throws MiningException, ParameterException, DatabaseException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that all required parameters are present
            validateParams(params, "minSupport");

            // Load the dataset as a transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            
            // Parse and validate the minimum support threshold
            int minSupportCount = parseMinSupport(params, database);

            LOGGER.info("Starting closed itemset mining with minSupport: " + minSupportCount);

            // Create a constraint programming model for mining closed itemsets
            Model model = new Model("Closed Itemset Mining");
            
            // Define decision variables:
            // - x[i] is true if item i is in the itemset
            BoolVar[] itemSelectionVars = model.boolVarArray("x", database.getNbItems());
            
            // - freq represents the support count (number of transactions covering the itemset)
            IntVar supportCountVar = model.intVar("freq", 1, database.getNbTransactions());

            // Post the minimum support constraint
            model.arithm(supportCountVar, ">=", minSupportCount).post();
            
            // Post the coverage constraint (defines the relationship between items and their frequency)
            ConstraintFactory.coverSize(database, supportCountVar, itemSelectionVars).post();
            
            // Post the closure constraint (ensures the itemset is closed)
            // A closed itemset cannot be extended with any additional item without reducing its support
            ConstraintFactory.coverClosure(database, itemSelectionVars).post();
            
            // Configure the solver with an appropriate search strategy
            Solver solver = model.getSolver();
            solver.setSearch(Search.minDomUBSearch(itemSelectionVars));
            List<MiningResult> results = new ArrayList<>();

            try {
                // Find all solutions (closed itemsets) that satisfy the constraints
                while (solver.solve()) {
                    // Check for cancellation after each solution
                    checkCancellation(cancellationChecker);
                    
                    // Extract the current solution as a mining result
                    MiningResult result = getMiningResult(database, itemSelectionVars, supportCountVar);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Closed itemset mining completed. Found " + results.size() + " results.");

            // Remove any empty itemsets from the results
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Closed itemset mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("Closed itemset mining cancelled during operation.");
                throw new MiningException("Mining cancelled by user.", e);
            }
            throw new MiningException("Unexpected error in extractClosed: " + e.getMessage(), e);
        }
    }


        /**
     * Extracts maximal itemsets from a dataset using a constraint programming approach.
     * A maximal itemset is a frequent itemset that has no frequent proper superset.
     * This method uses the Choco Solver constraint programming library to find all maximal itemsets
     * that meet the minimum support threshold.
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
            BooleanSupplier cancellationChecker) throws MiningException, ParameterException, DatabaseException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that all required parameters are present
            validateParams(params, "minSupport");

            // Load the dataset as a transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            
            // Parse and validate the minimum support threshold
            int minSupportCount = parseMinSupport(params, database);

            LOGGER.info("Starting maximal itemset mining with minSupport: " + minSupportCount);

            // Create a constraint programming model for mining maximal itemsets
            Model model = new Model("Maximal Itemset Mining");
            
            // Define decision variables:
            // - x[i] is true if item i is in the itemset
            BoolVar[] itemSelectionVars = model.boolVarArray("x", database.getNbItems());
            
            // - freq represents the support count (number of transactions covering the itemset)
            IntVar supportCountVar = model.intVar("freq", 1, database.getNbTransactions());

            // Post the minimum support constraint
            model.arithm(supportCountVar, ">=", minSupportCount).post();
            
            // Post the coverage constraint (defines the relationship between items and their frequency)
            ConstraintFactory.coverSize(database, supportCountVar, itemSelectionVars).post();
            
            // Post the maximality constraint (ensures the itemset is maximal)
            // A maximal itemset cannot have any superset that is also frequent
            ConstraintFactory.infrequentSupers(database, minSupportCount, itemSelectionVars).post();
            
            // Configure the solver with an appropriate search strategy
            // Using upper bound search helps find larger itemsets first
            Solver solver = model.getSolver();
            solver.setSearch(Search.inputOrderUBSearch(itemSelectionVars));
            List<MiningResult> results = new ArrayList<>();

            try {
                // Find all solutions (maximal itemsets) that satisfy the constraints
                while (solver.solve()) {
                    // Check for cancellation after each solution
                    checkCancellation(cancellationChecker);
                    
                    // Extract the current solution as a mining result
                    MiningResult result = getMiningResult(database, itemSelectionVars, supportCountVar);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Maximal itemset mining completed. Found " + results.size() + " results.");

            // Remove any empty itemsets from the results
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Maximal itemset mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("Maximal itemset mining cancelled during operation.");
                throw new MiningException("Mining cancelled by user.", e);
            }
            throw new MiningException("Unexpected error in extractMaximal: " + e.getMessage(), e);
        }
    }


    /**
     * Extracts rare itemsets from a dataset using a constraint programming approach.
     * A rare itemset is one that appears in fewer transactions than the specified maximum support threshold.
     * This implementation ensures that each returned itemset contains at least one rare singleton item.
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
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            checkCancellation(cancellationChecker);
            validateParams(params, "maxSupport");

            // Load the dataset as a transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            int maxSupportCount = parseMaxSupport(params, database);

            // Ensure maxSupport is meaningful (must allow at least some transactions)
            if (maxSupportCount <= 1) {
                throw new ParameterException(
                        "For rare itemset mining, maxSupport must result in a value greater than 1");
            }

            LOGGER.info("Starting rare itemset mining with maxSupport: " + maxSupportCount);

            // PHASE 1: Identify rare singleton items (items that appear in fewer than maxSupport transactions)
            Model identificationModel = new Model("Rare Item Identification");
            BoolVar[] candidateVars = identificationModel.boolVarArray("x", database.getNbItems());
            IntVar supportVar = identificationModel.intVar("freq", 1, database.getNbTransactions());

            // Setup solver for identification phase
            Solver identificationSolver = identificationModel.getSolver();
            identificationSolver.setSearch(Search.minDomLBSearch(candidateVars));
            
            // Add constraints for rare item identification
            identificationModel.arithm(supportVar, "<", maxSupportCount).post();
            ConstraintFactory.coverSize(database, supportVar, candidateVars).post();

            // Track which items are rare singletons
            boolean[] rareItems = new boolean[database.getNbItems()];
            
            // Find all rare singleton items
            while (identificationSolver.solve()) {
                checkCancellation(cancellationChecker);
                int[] itemset = IntStream.range(0, candidateVars.length)
                        .filter(i -> candidateVars[i].getValue() == 1)
                        .map(i -> database.getItems()[i])
                        .toArray();
                        
                // If this is a singleton set, mark the item as rare
                if (itemset.length == 1) {
                    rareItems[itemset[0] - 1] = true;
                }
            }
            
            // Log rare singleton items for debugging
            int index = 0;
            for (boolean isRare : rareItems) {
                if (isRare) {
                    LOGGER.fine("Identified rare singleton item: " + (index + 1));
                }
                checkCancellation(cancellationChecker);
                index++;
            }

            // PHASE 2: Extract all rare itemsets that contain at least one rare singleton item
            Model extractionModel = new Model("Rare Itemset Mining");
            BoolVar[] itemVars = extractionModel.boolVarArray("x", database.getNbItems());
            IntVar freqVar = extractionModel.intVar("freq", 1, database.getNbTransactions());

            // Setup main constraints
            extractionModel.arithm(freqVar, "<", maxSupportCount).post();
            ConstraintFactory.coverSize(database, freqVar, itemVars).post();

            // Add constraint: at least one rare singleton item must be included in each result
            BoolVar[] rareItemVars = new BoolVar[database.getNbItems()];
            for (int i = 0; i < database.getNbItems(); i++) {
                if (rareItems[i]) {
                    rareItemVars[i] = itemVars[i];
                } else {
                    // Non-rare items don't contribute to this constraint
                    rareItemVars[i] = extractionModel.boolVar(false);
                }
            }
            extractionModel.sum(rareItemVars, ">=", 1).post();

            // Setup solver for extraction phase
            Solver extractionSolver = extractionModel.getSolver();
            extractionSolver.setSearch(Search.minDomLBSearch(itemVars));
            
            List<MiningResult> results = new ArrayList<>();

            try {
                // Find all rare itemsets that satisfy our constraints
                while (extractionSolver.solve()) {
                    checkCancellation(cancellationChecker);
                    MiningResult result = getMiningResult(database, itemVars, freqVar);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Rare itemset mining completed. Found " + results.size() + " results.");

            // Empty itemsets are not allowed
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Rare itemset mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("Rare itemset mining cancelled during operation.");
                throw new MiningException("Mining cancelled by user.", e);
            }
            throw new MiningException("Unexpected error in extractRare: " + e.getMessage(), e);
        }
    }


        /**
     * Extracts generator itemsets from a dataset using a constraint programming approach.
     * A generator is a minimal itemset among all itemsets having the same support value.
     * In other words, no proper subset of a generator can have the same support.
     * 
     * @param datasetPath The file path to the dataset to be analyzed
     * @param params A map containing algorithm parameters, must include "minSupport"
     * @param cancellationChecker A supplier that returns true if the operation should be cancelled
     * @return A list of mining results containing the discovered generator itemsets
     * @throws MiningException If any error occurs during the mining process
     * @throws ParameterException If required parameters are missing or invalid
     * @throws DatabaseException If there is an issue with the dataset
     */
    @Override
    public List<MiningResult> extractGenerators(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that all required parameters are present
            validateParams(params, "minSupport");

            // Load the dataset as a transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            
            // Parse and validate the minimum support threshold
            int minSupportCount = parseMinSupport(params, database);

            LOGGER.info("Starting generator itemset mining with minSupport: " + minSupportCount);

            // Create a constraint programming model for mining generator itemsets
            Model model = new Model("Generators Mining");
            
            // Define decision variables:
            // - x[i] is true if item i is in the itemset
            BoolVar[] itemSelectionVars = model.boolVarArray("x", database.getNbItems());
            
            // - freq represents the support count (number of transactions covering the itemset)
            IntVar supportCountVar = model.intVar("freq", 1, database.getNbTransactions());

            // Post the minimum support constraint
            model.arithm(supportCountVar, ">=", minSupportCount).post();
            
            // Post the generator constraint: no proper subset has the same support
            ConstraintFactory.generator(database, itemSelectionVars).post();
            
            // Post the coverage constraint to calculate the support of the itemset
            ConstraintFactory.coverSize(database, supportCountVar, itemSelectionVars).post();

            // Setup the search strategy - start with smaller itemsets
            Solver solver = model.getSolver();
            solver.setSearch(Search.inputOrderLBSearch(itemSelectionVars));
            
            List<MiningResult> results = new ArrayList<>();

            try {
                // Find all generator itemsets
                while (solver.solve()) {
                    checkCancellation(cancellationChecker);
                    MiningResult result = getMiningResult(database, itemSelectionVars, supportCountVar);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Generator itemset mining completed. Found " + results.size() + " results.");

            // Remove empty itemsets as they are not considered valid generators
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Generator itemset mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("Generator itemset mining cancelled during operation.");
                throw new MiningException("Mining cancelled by user.", e);
            }
            throw new MiningException("Unexpected error in extractGenerators: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts minimal rare itemsets from a dataset using a constraint programming approach.
     * A minimal rare itemset is a rare itemset (support < maxSupport) that has no rare proper subset.
     * This means all proper subsets of a minimal rare itemset are frequent.
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
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that all required parameters are present
            validateParams(params, "maxSupport");

            // Load the dataset as a transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            
            // Parse and validate the maximum support threshold
            int maxSupportCount = parseMaxSupport(params, database);

            // Ensure maxSupport is meaningful (must allow at least some transactions)
            if (maxSupportCount <= 1) {
                throw new ParameterException(
                        "For minimal itemset mining, maxSupport must result in a value greater than 1");
            }

            LOGGER.info("Starting minimal rare itemset mining with maxSupport: " + maxSupportCount);

            // Create a constraint programming model for mining minimal rare itemsets
            Model model = new Model("Minimal Rare Itemset Mining");
            
            // Define decision variables:
            // - itemVars[i] is true if item i is in the itemset
            BoolVar[] itemVars = model.boolVarArray("x", database.getNbItems());
            
            // - supportVar represents the support count (number of transactions covering the itemset)
            IntVar supportVar = model.intVar("freq", 1, database.getNbTransactions());

            // Post the rare itemset constraint (support < maxSupport)
            model.arithm(supportVar, "<", maxSupportCount).post();
            
            // Post the coverage constraint to calculate the support of the itemset
            ConstraintFactory.coverSize(database, supportVar, itemVars).post();
            
            // Post the minimality constraint: all proper subsets must be frequent
            // This is the key constraint that defines "minimal" rare itemsets
            ConstraintFactory.frequentSubs(database, maxSupportCount, itemVars).post();

            // Setup the solver
            Solver solver = model.getSolver();
            // Note: Default search strategy is used as it works well for this problem
            
            List<MiningResult> results = new ArrayList<>();

            try {
                // Find all minimal rare itemsets
                while (solver.solve()) {
                    checkCancellation(cancellationChecker);
                    MiningResult result = getMiningResult(database, itemVars, supportVar);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Minimal rare itemset mining completed. Found " + results.size() + " results.");

            // Remove empty itemsets as they are not considered valid minimal rare itemsets
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Minimal rare itemset mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("Minimal rare itemset mining cancelled during operation.");
                throw new MiningException("Mining cancelled by user.", e);
            }
            throw new MiningException("Unexpected error in extractMinimal: " + e.getMessage(), e);
        }
    }

        /**
     * Extracts closed itemsets of specific sizes from a dataset using a constraint programming approach.
     * This method finds all closed itemsets whose cardinality is between minSize and maxSize (inclusive)
     * and that meet the minimum support threshold.
     *
     * @param datasetPath The file path to the dataset to be analyzed
     * @param params A map containing algorithm parameters, must include "minSize", "maxSize", and "minSupport"
     * @param cancellationChecker A supplier that returns true if the operation should be cancelled
     * @return A list of mining results containing the discovered closed itemsets within the specified size range
     * @throws MiningException If any error occurs during the mining process
     * @throws ParameterException If required parameters are missing or invalid
     * @throws DatabaseException If there is an issue with the dataset
     */
    @Override
    public List<MiningResult> extractSizeBetween(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that all required parameters are present
            validateParams(params, "minSize", "maxSize", "minSupport");

            // Load the dataset as a transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            int minSupportCount = parseMinSupport(params, database);

            // Parse size constraints
            int minItemsetSize, maxItemsetSize;
            try {
                minItemsetSize = Integer.parseInt(params.get("minSize"));
                maxItemsetSize = Integer.parseInt(params.get("maxSize"));
            } catch (NumberFormatException e) {
                throw new ParameterException("Invalid minSize or maxSize parameters: " + e.getMessage());
            }

            // Validate size constraints
            if (minItemsetSize < 1) {
                throw new ParameterException("minSize must be at least 1");
            }

            if (maxItemsetSize < minItemsetSize) {
                throw new ParameterException("maxSize must be greater than or equal to minSize");
            }

            // Ensure maxSize doesn't exceed the number of items in the database
            if (maxItemsetSize > database.getNbItems()) {
                LOGGER.warning("maxSize is greater than the number of items in the database. " +
                        "Setting maxSize to the number of items: " + database.getNbItems());
                maxItemsetSize = database.getNbItems();
            }

            LOGGER.info("Starting size-constrained closed itemset mining with minSize: " + minItemsetSize + 
                    ", maxSize: " + maxItemsetSize + ", minSupport: " + minSupportCount);

            // Create a constraint programming model
            Model model = new Model("Size Between Itemset Mining");
            
            // Define decision variables:
            // - itemVars[i] is true if item i is in the itemset
            BoolVar[] itemVars = model.boolVarArray("x", database.getNbItems());
            
            // - supportVar represents the support count (number of transactions covering the itemset)
            IntVar supportVar = model.intVar("freq", 1, database.getNbTransactions());

            // Post size constraints
            model.sum(itemVars, ">=", minItemsetSize).post();  // Enforce minimum size
            model.sum(itemVars, "<=", maxItemsetSize).post();  // Enforce maximum size
            
            // Post minimum support constraint
            model.arithm(supportVar, ">=", minSupportCount).post();

            // Post constraints to find closed itemsets
            // 1. Coverage constraint: compute the support of the itemset
            ConstraintFactory.coverSize(database, supportVar, itemVars).post();
            
            // 2. Closure constraint: ensure the itemset is closed
            // (i.e., there's no proper superset with the same support)
            ConstraintFactory.coverClosure(database, itemVars).post();

            // Setup the solver with an appropriate search strategy
            Solver solver = model.getSolver();
            
            // Use minDomLBSearch which selects the variable with the smallest domain
            // and assigns it to its lower bound first - good for finding itemsets efficiently
            solver.setSearch(Search.minDomLBSearch(itemVars));
            
            List<MiningResult> results = new ArrayList<>();

            try {
                // Find all closed itemsets within the size constraints
                while (solver.solve()) {
                    checkCancellation(cancellationChecker);
                    MiningResult result = getMiningResult(database, itemVars, supportVar);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Size-constrained closed itemset mining completed. Found " + results.size() + " results.");

            // Remove any empty itemsets that might have been generated
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Size-constrained itemset mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("Size-constrained itemset mining cancelled during operation.");
                throw new MiningException("Mining cancelled by user.", e);
            }
            throw new MiningException("Unexpected error in extractSizeBetween: " + e.getMessage(), e);
        }
    }


        /**
     * Extracts closed itemsets that contain specific items using a constraint programming approach.
     * This method finds all closed itemsets that include the items specified in the "items" parameter
     * and meet the minimum support threshold.
     *
     * @param datasetPath The file path to the dataset to be analyzed
     * @param params A map containing algorithm parameters, must include "items" and "minSupport"
     *               The "items" parameter should be a comma-separated list of item indices that must be present
     * @param cancellationChecker A supplier that returns true if the operation should be cancelled
     * @return A list of mining results containing the discovered closed itemsets that include the specified items
     * @throws MiningException If any error occurs during the mining process
     * @throws ParameterException If required parameters are missing or invalid
     * @throws DatabaseException If there is an issue with the dataset
     */
    @Override
    public List<MiningResult> extractPresence(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that all required parameters are present
            validateParams(params, "items", "minSupport");

            // Load the dataset as a transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            int minSupportCount = parseMinSupport(params, database);

            // Parse the list of items that must be present in the discovered patterns
            int[] requiredItems = parseArrayParameter(params.get("items"), database.getNbItems());

            LOGGER.info("Starting closed itemset mining with item presence constraints and minSupport: " + minSupportCount);

            // Create a constraint programming model
            Model model = new Model("Closed Itemset Mining with Presence Constraints");
            
            // Define decision variables:
            // - itemVars[i] is true if item i is in the itemset
            BoolVar[] itemVars = model.boolVarArray("x", database.getNbItems());
            
            // - supportVar represents the support count (number of transactions covering the itemset)
            IntVar supportVar = model.intVar("freq", 1, database.getNbTransactions());

            // Post item presence constraints
            // For each required item (marked with 1 in the presence array), force it to be in the pattern
            for (int i = 0; i < requiredItems.length; i++) {
                if (requiredItems[i] == 1) {
                    // Force this item to be included in all solutions
                    itemVars[i].eq(1).post();
                }
            }

            // Post minimum support constraint
            model.arithm(supportVar, ">=", minSupportCount).post();
            
            // Post constraints to find closed itemsets
            // 1. Coverage constraint: compute the support of the itemset
            ConstraintFactory.coverSize(database, supportVar, itemVars).post();
            
            // 2. Closure constraint: ensure the itemset is closed
            // (i.e., there's no proper superset with the same support)
            ConstraintFactory.coverClosure(database, itemVars).post();

            // Setup the solver
            Solver solver = model.getSolver();
            List<MiningResult> results = new ArrayList<>();

            try {
                // Find all closed itemsets that satisfy the presence constraints
                while (solver.solve()) {
                    checkCancellation(cancellationChecker);
                    MiningResult result = getMiningResult(database, itemVars, supportVar);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Presence-constrained closed itemset mining completed. Found " + results.size() + " results.");

            // Remove any empty itemsets that might have been generated
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Presence-constrained mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("Presence-constrained mining cancelled during operation.");
                throw new MiningException("Mining cancelled by user.", e);
            }
            throw new MiningException("Unexpected error in extractPresence: " + e.getMessage(), e);
        }
    }


        /**
     * Extracts closed itemsets that specifically exclude certain items using a constraint programming approach.
     * This method finds all closed itemsets that do NOT include the items specified in the "items" parameter
     * and meet the minimum support threshold.
     *
     * @param datasetPath The file path to the dataset to be analyzed
     * @param params A map containing algorithm parameters, must include "items" and "minSupport"
     *               The "items" parameter should be a comma-separated list of item indices that must be absent
     * @param cancellationChecker A supplier that returns true if the operation should be cancelled
     * @return A list of mining results containing the discovered closed itemsets that exclude the specified items
     * @throws MiningException If any error occurs during the mining process
     * @throws ParameterException If required parameters are missing or invalid
     * @throws DatabaseException If there is an issue with the dataset
     */
    @Override
    public List<MiningResult> extractAbsence(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            // Check if operation has been cancelled before starting
            checkCancellation(cancellationChecker);
            
            // Validate that all required parameters are present
            validateParams(params, "items", "minSupport");

            // Load the dataset as a transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            int minSupportCount = parseMinSupport(params, database);

            // Parse the list of items that must be absent from the discovered patterns
            int[] forbiddenItems = parseArrayParameter(params.get("items"), database.getNbItems());

            LOGGER.info("Starting closed itemset mining with item absence constraints and minSupport: " + minSupportCount);

            // Create a constraint programming model
            Model model = new Model("Closed Itemset Mining with Absence Constraints");
            
            // Define decision variables:
            // - itemVars[i] is true if item i is in the itemset
            BoolVar[] itemVars = model.boolVarArray("x", database.getNbItems());
            
            // - supportVar represents the support count (number of transactions covering the itemset)
            IntVar supportVar = model.intVar("freq", 1, database.getNbTransactions());

            // Post item absence constraints
            // For each forbidden item (marked with 1 in the absence array), force it NOT to be in the pattern
            for (int i = 0; i < forbiddenItems.length; i++) {
                if (forbiddenItems[i] == 1) {
                    // Force this item to be excluded in all solutions
                    itemVars[i].eq(0).post();
                }
            }

            // Post minimum support constraint
            model.arithm(supportVar, ">=", minSupportCount).post();
            
            // Post constraints to find closed itemsets
            // 1. Coverage constraint: compute the support of the itemset
            ConstraintFactory.coverSize(database, supportVar, itemVars).post();
            
            // 2. Closure constraint: ensure the itemset is closed
            // (i.e., there's no proper superset with the same support)
            ConstraintFactory.coverClosure(database, itemVars).post();

            // Setup the solver with a specific search strategy
            Solver solver = model.getSolver();
            
            // Use input order strategy which processes variables in their natural order
            solver.setSearch(Search.inputOrderLBSearch(itemVars));
            
            List<MiningResult> results = new ArrayList<>();

            try {
                // Find all closed itemsets that satisfy the absence constraints
                while (solver.solve()) {
                    checkCancellation(cancellationChecker);
                    MiningResult result = getMiningResult(database, itemVars, supportVar);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Absence-constrained closed itemset mining completed. Found " + results.size() + " results.");

            // Remove any empty itemsets that might have been generated
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Absence-constrained mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("Absence-constrained mining cancelled during operation.");
                throw new MiningException("Mining cancelled by user.", e);
            }
            throw new MiningException("Unexpected error in extractAbsence: " + e.getMessage(), e);
        }
    }


    /**
     * Reads the transactional database from the given path.
     *
     * @param datasetPath The path to the dataset file
     * @return The transactional database
     * @throws DatabaseException If there is an error reading the database
     */
    private TransactionalDatabase readTransactionalDatabase(String datasetPath) throws DatabaseException {
        try {
            if (datasetPath == null || datasetPath.trim().isEmpty()) {
                throw new DatabaseException("Dataset path cannot be null or empty");
            }

            // First try as a file system path
            File file = new File(datasetPath);
            if (file.exists() && file.isFile()) {
                LOGGER.info("Reading transactional database from file system: " + file.getAbsolutePath());
                try {
                    return new DatReader(file.getAbsolutePath()).read();
                } catch (Exception e) {
                    throw new DatabaseException("Error reading database from file: " + e.getMessage(), e);
                }
            }

            // If not found as a file, try as a resource
            URL url = ChocoMiner.class.getResource(datasetPath);
            if (url == null) {
                throw new DatabaseException("Dataset file not found: " + datasetPath);
            }

            String path = URLDecoder.decode(url.getPath(), "UTF-8");
            LOGGER.info("Reading transactional database from resource: " + path);

            try {
                return new DatReader(path).read();
            } catch (Exception e) {
                throw new DatabaseException("Error reading database from resource: " + e.getMessage(), e);
            }
        } catch (UnsupportedEncodingException e) {
            // This should never happen with UTF-8
            throw new DatabaseException("Unsupported encoding: " + e.getMessage(), e);
        }
    }

    /**
     * Parse array parameters from a comma-separated string.
     * * @param param The parameter string (comma-separated indices)
     * 
     * @param nbItems The number of items in the database
     * @return An array where selected indices are set to 1
     */
    private int[] parseArrayParameter(String param, int nbItems) {
        int[] array = new int[nbItems];
        if (param != null && !param.isEmpty()) {
            String[] indices = param.split(",");
            for (String idxStr : indices) {
                try {
                    int idx = Integer.parseInt(idxStr.trim()) - 1; // Convert to 0-based index
                    if (idx >= 0 && idx < array.length) {
                        array[idx] = 1;
                    } else {
                        LOGGER.warning("Index " + (idx + 1) + " is out of range (valid range: 1-" + nbItems + ")");
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warning("Invalid index value: " + idxStr);
                }
            }
        }
        return array;
    }

    /**
     * Creates a MiningResult from the current solution.
     *
     * @param database The transactional database
     * @param x        The boolean variables representing the items
     * @param freq     The frequency variable
     * @return The mining result
     */
    private MiningResult getMiningResult(TransactionalDatabase database, BoolVar[] x, IntVar freq) {
        List<Integer> itemset = new ArrayList<>();
        for (int i = 0; i < x.length; i++) {
            if (x[i].getValue() == 1) {
                itemset.add(database.getItems()[i]);
            }
        }
        return new MiningResult(itemset, freq.getValue());
    }

    /**
     * Filters out empty itemsets from the results.
     *
     * @param results The list of mining results
     * @return The filtered list of mining results
     */
    private void filterOutEmptyItemsets(List<MiningResult> results) {
        results.removeIf(result -> result.getPattern().isEmpty());
    }
}