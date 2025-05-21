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

    @Override
    public List<MiningResult> extractFrequent(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            checkCancellation(cancellationChecker);
            validateParams(params, "minSupport");

            // Read the transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            int minSupport = parseMinSupport(params, database);

            LOGGER.info("Starting frequent itemset mining with minSupport: " + minSupport);

            Model model = new Model("Frequent Itemset Mining");
            BoolVar[] x = model.boolVarArray("x", database.getNbItems());
            IntVar freq = model.intVar("freq", 1, database.getNbTransactions());

            // Post constraint
            model.arithm(freq, ">=", minSupport).post();
            ConstraintFactory.coverSize(database, freq, x).post();

            Solver solver = model.getSolver();
            List<MiningResult> results = new ArrayList<>();

            try {
                while (solver.solve()) {
                    checkCancellation(cancellationChecker);
                    MiningResult result = getMiningResult(database, x, freq);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Frequent itemset mining completed. Found " + results.size() + " results.");

            // Empty itemsets are not allowed
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Frequent itemset mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("Frequent itemset mining cancelled during operation.");
                throw new MiningException("Mining cancelled by user.", e);
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

            // Read the transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            int minSupport = parseMinSupport(params, database);

            LOGGER.info("Starting closed itemset mining with minSupport: " + minSupport);

            Model model = new Model("Closed Itemset Mining");
            BoolVar[] x = model.boolVarArray("x", database.getNbItems());
            IntVar freq = model.intVar("freq", 1, database.getNbTransactions());

            // Post constraints
            model.arithm(freq, ">=", minSupport).post();
            ConstraintFactory.coverSize(database, freq, x).post();
            ConstraintFactory.coverClosure(database, x).post();
            Solver solver = model.getSolver();
            List<MiningResult> results = new ArrayList<>();

            try {
                while (solver.solve()) {
                    checkCancellation(cancellationChecker);
                    MiningResult result = getMiningResult(database, x, freq);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Closed itemset mining completed. Found " + results.size() + " results.");

            // Empty itemsets are not allowed
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Closed itemset mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("Closed itemset mining cancelled during operation.");
                throw new MiningException("Mining cancelled by user.", e);
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

            // Read the transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            int minSupport = parseMinSupport(params, database);

            LOGGER.info("Starting maximal itemset mining with minSupport: " + minSupport);

            Model model = new Model("Maximal Itemset Mining");
            BoolVar[] x = model.boolVarArray("x", database.getNbItems());
            IntVar freq = model.intVar("freq", 1, database.getNbTransactions());

            // Post constraints
            model.arithm(freq, ">=", minSupport).post();
            ConstraintFactory.coverSize(database, freq, x).post();
            ConstraintFactory.infrequentSupers(database, minSupport, x).post();

            Solver solver = model.getSolver();
            List<MiningResult> results = new ArrayList<>();

            try {
                while (solver.solve()) {
                    checkCancellation(cancellationChecker);
                    MiningResult result = getMiningResult(database, x, freq);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Maximal itemset mining completed. Found " + results.size() + " results.");

            // Empty itemsets are not allowed
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Maximal itemset mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("Maximal itemset mining cancelled during operation.");
                throw new MiningException("Mining cancelled by user.", e);
            }
            throw new MiningException("Unexpected error in extractMaximal: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MiningResult> extractRare(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            checkCancellation(cancellationChecker);
            validateParams(params, "maxSupport");

            // Read the transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            int maxSupport = parseMaxSupport(params, database);

            // maxSupport is excluded as upperbound
            if (maxSupport <= 1) {
                throw new ParameterException(
                        "For rare itemset mining, maxSupport must result in a value greater than 1");
            }

            LOGGER.info("Starting rare itemset mining with maxSupport: " + maxSupport);

            Model model = new Model("Rare Itemset Mining");
            BoolVar[] x = model.boolVarArray("x", database.getNbItems());
            IntVar freq = model.intVar("freq", 1, database.getNbTransactions());

            Solver solver = model.getSolver();

            // Post constraint
            model.arithm(freq, "<", maxSupport).post();
            ConstraintFactory.coverSize(database, freq, x).post();

            // We need to add a constraint to ensure that at least one rare item is included
            // First, identify rare items (items with support < maxSupport)
            boolean[] rareItems = new boolean[database.getNbItems()];
            while (solver.solve()) {
                checkCancellation(cancellationChecker);
                int[] itemset = IntStream.range(0, x.length)
                        .filter(i -> x[i].getValue() == 1)
                        .map(i -> database.getItems()[i])
                        .toArray();
                if (itemset.length <= 1)
                    rareItems[itemset[0] - 1] = true;
            }
            int index = 0;
            for (boolean b : rareItems) {
                if (b) {
                    System.err.println("rare set of size 1 : " + (index + 1));
                }
                checkCancellation(cancellationChecker);
                index++;
            }
            solver.reset();

            // Add constraint: at least one rare item must be included
            BoolVar[] rareItemVars = new BoolVar[database.getNbItems()];
            for (int i = 0; i < database.getNbItems(); i++) {
                if (rareItems[i]) {
                    rareItemVars[i] = x[i];
                } else {
                    // Non-rare items don't contribute to this constraint
                    rareItemVars[i] = model.boolVar(false);
                }
            }
            model.sum(rareItemVars, ">=", 1).post();

            List<MiningResult> results = new ArrayList<>();

            try {
                while (solver.solve()) {
                    checkCancellation(cancellationChecker);
                    MiningResult result = getMiningResult(database, x, freq);
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

    @Override
    public List<MiningResult> extractGenerators(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            checkCancellation(cancellationChecker);
            validateParams(params, "minSupport");

            // Read the transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            int minSupport = parseMinSupport(params, database);

            LOGGER.info("Starting generators mining with minSupport: " + minSupport);

            Model model = new Model("Generators Mining");
            BoolVar[] x = model.boolVarArray("x", database.getNbItems());
            IntVar freq = model.intVar("freq", 1, database.getNbTransactions());

            // Post constraints
            model.arithm(freq, ">=", minSupport).post();
            ConstraintFactory.generator(database, x).post();
            ConstraintFactory.coverSize(database, freq, x).post();

            Solver solver = model.getSolver();
            List<MiningResult> results = new ArrayList<>();

            try {
                while (solver.solve()) {
                    checkCancellation(cancellationChecker);
                    MiningResult result = getMiningResult(database, x, freq);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Generators mining completed. Found " + results.size() + " results.");

            // Empty itemsets are not allowed
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Generators mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("Generators mining cancelled during operation.");
                throw new MiningException("Mining cancelled by user.", e);
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

            // Read the transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            int maxSupport = parseMaxSupport(params, database);

            if (maxSupport <= 1) {
                throw new ParameterException(
                        "For minimal itemset mining, maxSupport must result in a value greater than 1");
            }

            LOGGER.info("Starting minimal itemset mining with maxSupport: " + maxSupport);

            Model model = new Model("Minimal Itemset Mining");
            BoolVar[] x = model.boolVarArray("x", database.getNbItems());
            IntVar freq = model.intVar("freq", 1, database.getNbTransactions());

            // Post constraints
            model.arithm(freq, "<", maxSupport).post();
            ConstraintFactory.coverSize(database, freq, x).post();
            ConstraintFactory.frequentSubs(database, maxSupport, x).post();

            Solver solver = model.getSolver();
            List<MiningResult> results = new ArrayList<>();

            try {
                while (solver.solve()) {
                    checkCancellation(cancellationChecker);
                    MiningResult result = getMiningResult(database, x, freq);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Minimal itemset mining completed. Found " + results.size() + " results.");

            // Empty itemsets are not allowed
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Minimal itemset mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("Minimal itemset mining cancelled during operation.");
                throw new MiningException("Mining cancelled by user.", e);
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

            // Read the transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            int minSupport = parseMinSupport(params, database);

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

            if (maxSize > database.getNbItems()) {
                LOGGER.warning("maxSize is greater than the number of items in the database. " +
                        "Setting maxSize to the number of items: " + database.getNbItems());
                maxSize = database.getNbItems();
            }

            LOGGER.info("Starting size between itemset mining with minSize: " + minSize + ", maxSize: " + maxSize +
                    ", minSupport: " + minSupport);

            Model model = new Model("Size Between Itemset Mining");
            BoolVar[] x = model.boolVarArray("x", database.getNbItems());
            IntVar freq = model.intVar("freq", 1, database.getNbTransactions());

            // Limiting the size of the itemsets
            model.sum(x, ">=", minSize).post();
            model.sum(x, "<=", maxSize).post();
            model.arithm(freq, ">=", minSupport).post();

            // Adding the constraints for closed itemsets
            ConstraintFactory.coverSize(database, freq, x).post();
            ConstraintFactory.coverClosure(database, x).post();

            Solver solver = model.getSolver();
            List<MiningResult> results = new ArrayList<>();

            try {
                while (solver.solve()) {
                    checkCancellation(cancellationChecker);
                    MiningResult result = getMiningResult(database, x, freq);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Size between itemset mining completed. Found " + results.size() + " results.");

            // Empty itemsets are not allowed
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("SizeBetween itemset mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("SizeBetween itemset mining cancelled during operation.");
                throw new MiningException("Mining cancelled by user.", e);
            }
            throw new MiningException("Unexpected error in extractSizeBetween: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MiningResult> extractPresence(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            checkCancellation(cancellationChecker);
            validateParams(params, "items", "minSupport");

            // Read the transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            int minSupport = parseMinSupport(params, database);

            int[] presence = parseArrayParameter(params.get("items"), database.getNbItems());

            LOGGER.info("Starting closed itemset mining with presence constraints and minSupport: " + minSupport);

            Model model = new Model("Closed Itemset Mining with Presence");
            BoolVar[] x = model.boolVarArray("x", database.getNbItems());
            IntVar freq = model.intVar("freq", 1, database.getNbTransactions());

            // Constraining the presence of items
            for (int i = 0; i < presence.length; i++) {
                if (presence[i] == 1) {
                    x[i].eq(1).post();
                }
            }

            // Adding the constraints for closed itemsets
            model.arithm(freq, ">=", minSupport).post();
            ConstraintFactory.coverSize(database, freq, x).post();
            ConstraintFactory.coverClosure(database, x).post();

            Solver solver = model.getSolver();
            List<MiningResult> results = new ArrayList<>();

            try {
                while (solver.solve()) {
                    checkCancellation(cancellationChecker);
                    MiningResult result = getMiningResult(database, x, freq);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Presence constrained mining completed. Found " + results.size() + " results.");

            // Empty itemsets are not allowed
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Presence constrained mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("Presence constrained mining cancelled during operation.");
                throw new MiningException("Mining cancelled by user.", e);
            }
            throw new MiningException("Unexpected error in extractPresence: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MiningResult> extractAbsence(String datasetPath, Map<String, String> params,
            BooleanSupplier cancellationChecker) throws MiningException {
        try {
            checkCancellation(cancellationChecker);
            validateParams(params, "items", "minSupport");

            // Read the transactional database
            TransactionalDatabase database = readTransactionalDatabase(datasetPath);
            int minSupport = parseMinSupport(params, database);

            int[] absence = parseArrayParameter(params.get("items"), database.getNbItems());

            LOGGER.info("Starting closed itemset mining with absence constraints and minSupport: " + minSupport);

            Model model = new Model("Closed Itemset Mining with Absence");
            BoolVar[] x = model.boolVarArray("x", database.getNbItems());
            IntVar freq = model.intVar("freq", 1, database.getNbTransactions());

            // Constraining the absence of items
            for (int i = 0; i < absence.length; i++) {
                if (absence[i] == 1) {
                    x[i].eq(0).post();
                }
            }

            // Adding the constraints for closed itemsets
            model.arithm(freq, ">=", minSupport).post();
            ConstraintFactory.coverSize(database, freq, x).post();
            ConstraintFactory.coverClosure(database, x).post();

            Solver solver = model.getSolver();
            List<MiningResult> results = new ArrayList<>();

            try {
                while (solver.solve()) {
                    checkCancellation(cancellationChecker);
                    MiningResult result = getMiningResult(database, x, freq);
                    results.add(result);
                }
            } catch (Exception e) {
                throw new MiningException("Error during solving process: " + e.getMessage(), e);
            }

            LOGGER.info("Absence constrained mining completed. Found " + results.size() + " results.");

            // Empty itemsets are not allowed
            filterOutEmptyItemsets(results);

            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Absence constrained mining cancelled.");
            throw new MiningException("Mining cancelled by user.", e);
        } catch (ParameterException | DatabaseException e) {
            throw e;
        } catch (Exception e) {
            if (cancellationChecker.getAsBoolean()) {
                LOGGER.info("Absence constrained mining cancelled during operation.");
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