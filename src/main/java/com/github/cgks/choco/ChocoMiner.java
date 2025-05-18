package com.github.cgks.choco;

import java.net.URL;
import java.net.URLDecoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import com.github.cgks.Miner;
import com.github.cgks.MiningResult;

import io.gitlab.chaver.mining.patterns.constraints.factory.ConstraintFactory;
import io.gitlab.chaver.mining.patterns.io.DatReader;
import io.gitlab.chaver.mining.patterns.io.TransactionalDatabase;

public class ChocoMiner implements Miner {

    @Override
    public List<MiningResult> extractFrequent(String datasetPath, Map<String, String> params) throws Exception {
        // Read the transactional database
        TransactionalDatabase database = readTransactionalDatabase(datasetPath);

        Model model = new Model("Frequent Itemset Mining");
        BoolVar[] x = model.boolVarArray("x", database.getNbItems());
        int minSupport = (int) (database.getNbTransactions() * Double.parseDouble(params.get("minSupport")));
        IntVar freq = model.intVar("freq", minSupport, database.getNbTransactions());

        ConstraintFactory.coverSize(database, freq, x).post();

        Solver solver = model.getSolver();

        List<MiningResult> results = new ArrayList<>();

        while (solver.solve()) {
            MiningResult result = getMiningResult(database, x, freq);
            results.add(result);
        }

        return results;
    }

    @Override
    public List<MiningResult> extractClosed(String datasetPath, Map<String, String> params) throws Exception {
        // Read the transactional database
        TransactionalDatabase database = readTransactionalDatabase(datasetPath);

        Model model = new Model("Closed Itemset Mining");
        BoolVar[] x = model.boolVarArray("x", database.getNbItems());
        int minSupport = (int) (database.getNbTransactions() * Double.parseDouble(params.get("minSupport")));
        IntVar freq = model.intVar("freq", minSupport, database.getNbTransactions());

        ConstraintFactory.coverSize(database, freq, x).post();
        ConstraintFactory.coverClosure(database, x).post();

        Solver solver = model.getSolver();

        List<MiningResult> results = new ArrayList<>();

        while (solver.solve()) {
            MiningResult result = getMiningResult(database, x, freq);
            results.add(result);
        }

        return results;
    }

    @Override
    public List<MiningResult> extractMaximal(String datasetPath, Map<String, String> params) throws Exception {
        // Read the transactional database
        TransactionalDatabase database = readTransactionalDatabase(datasetPath);

        Model model = new Model("Maximal Itemset Mining");
        BoolVar[] x = model.boolVarArray("x", database.getNbItems());
        int minSupport = (int) (database.getNbTransactions() * Double.parseDouble(params.get("minSupport")));
        IntVar freq = model.intVar("freq", minSupport, database.getNbTransactions());

        ConstraintFactory.coverSize(database, freq, x).post();
        ConstraintFactory.infrequentSupers(database, minSupport, x).post();

        Solver solver = model.getSolver();

        List<MiningResult> results = new ArrayList<>();

        while (solver.solve()) {
            MiningResult result = getMiningResult(database, x, freq);
            results.add(result);
        }

        return results;
    }

    @Override
    public List<MiningResult> extractRare(String datasetPath, Map<String, String> params) throws Exception {
        // Read the transactional database
        TransactionalDatabase database = readTransactionalDatabase(datasetPath);

        Model model = new Model("Rare Itemset Mining");
        BoolVar[] x = model.boolVarArray("x", database.getNbItems());
        int minSupport = (int) (database.getNbTransactions() * Double.parseDouble(params.get("minSupport")));
        IntVar freq = model.intVar("freq", 1, minSupport - 1);

        ConstraintFactory.coverSize(database, freq, x).post();

        Solver solver = model.getSolver();

        List<MiningResult> results = new ArrayList<>();

        while (solver.solve()) {
            MiningResult result = getMiningResult(database, x, freq);
            results.add(result);
        }

        return results;
    }

    @Override
    public List<MiningResult> extractGenerators(String datasetPath, Map<String, String> params) throws Exception {
        // Read the transactional database
        TransactionalDatabase database = readTransactionalDatabase(datasetPath);

        Model model = new Model("Generators Mining");
        BoolVar[] x = model.boolVarArray("x", database.getNbItems());
        int minSupport = (int) (database.getNbTransactions() * Double.parseDouble(params.get("minSupport")));
        IntVar freq = model.intVar("freq", minSupport, database.getNbTransactions());

        ConstraintFactory.generator(database, x).post();
        ConstraintFactory.coverSize(database, freq, x).post();

        Solver solver = model.getSolver();

        List<MiningResult> results = new ArrayList<>();

        while (solver.solve()) {
            MiningResult result = getMiningResult(database, x, freq);
            results.add(result);
        }

        return results;
    }

    @Override
    public List<MiningResult> extractMinimal(String datasetPath, Map<String, String> params) throws Exception {
        // Read the transactional database
        TransactionalDatabase database = readTransactionalDatabase(datasetPath);

        Model model = new Model("Minimal Itemset Mining");
        BoolVar[] x = model.boolVarArray("x", database.getNbItems());
        int minSupport = (int) (database.getNbTransactions() * Double.parseDouble(params.get("minSupport")));
        IntVar freq = model.intVar("freq", 1, minSupport - 1);

        ConstraintFactory.coverSize(database, freq, x).post();
        ConstraintFactory.coverClosure(database, x).post();

        Solver solver = model.getSolver();

        List<MiningResult> results = new ArrayList<>();

        while (solver.solve()) {
            MiningResult result = getMiningResult(database, x, freq);
            results.add(result);
        }

        return results;
    }

    @Override
    public List<MiningResult> extractSizeBetween(String datasetPath, Map<String, String> params)
            throws Exception {
        // Read the transactional database
        TransactionalDatabase database = readTransactionalDatabase(datasetPath);

        int minSize = Integer.parseInt(params.get("minSize"));
        int maxSize = Integer.parseInt(params.get("maxSize"));


        Model model = new Model("Size Between Itemset Mining");
        BoolVar[] x = model.boolVarArray("x", database.getNbItems());
        int minSupport = (int) (database.getNbTransactions() * Double.parseDouble(params.get("minSupport")));
        IntVar freq = model.intVar("freq", minSupport, database.getNbTransactions());

        // Limiting the size of the itemsets
        model.sum(x, ">=", minSize).post();
        model.sum(x, "<=", maxSize).post();

        // Adding the constraints for closed itemsets
        ConstraintFactory.coverSize(database, freq, x).post();
        ConstraintFactory.coverClosure(database, x).post();

        Solver solver = model.getSolver();

        List<MiningResult> results = new ArrayList<>();

        while (solver.solve()) {
            MiningResult result = getMiningResult(database, x, freq);
            results.add(result);
        }

        return results;
    }

    @Override
    public List<MiningResult> extractPresence(String datasetPath, Map<String, String> params) throws Exception {
        // Read the transactional database
        TransactionalDatabase database = readTransactionalDatabase(datasetPath);

        int[] presence = parseArrayParameter(params.get("presence"), database.getNbItems());

        Model model = new Model("Closed Itemset Mining with Presence");
        BoolVar[] x = model.boolVarArray("x", database.getNbItems());
        int minSupport = (int) (database.getNbTransactions() * Double.parseDouble(params.get("minSupport")));
        IntVar freq = model.intVar("freq", minSupport, database.getNbTransactions());

        // Constraining the presence of items
        for (int i = 0; i < presence.length; i++) {
            if (presence[i] == 1) {
                x[i].eq(1).post();
            }
        }

        // Adding the constraints for closed itemsets
        ConstraintFactory.coverSize(database, freq, x).post();
        ConstraintFactory.coverClosure(database, x).post();

        Solver solver = model.getSolver();

        List<MiningResult> results = new ArrayList<>();

        while (solver.solve()) {
            MiningResult result = getMiningResult(database, x, freq);
            results.add(result);
        }

        return results;
    }

    @Override
    public List<MiningResult> extractAbsence(String datasetPath, Map<String, String> params) throws Exception {
        // Read the transactional database
        TransactionalDatabase database = readTransactionalDatabase(datasetPath);

        int[] absence = parseArrayParameter(params.get("absence"), database.getNbItems());

        Model model = new Model("Closed Itemset Mining with Absence");
        BoolVar[] x = model.boolVarArray("x", database.getNbItems());
        int minSupport = (int) (database.getNbTransactions() * Double.parseDouble(params.get("minSupport")));
        IntVar freq = model.intVar("freq", minSupport, database.getNbTransactions());

        // Constraining the absence of items
        for (int i = 0; i < absence.length; i++) {
            if (absence[i] == 1) {
                x[i].eq(0).post();
            }
        }

        // Adding the constraints for closed itemsets
        ConstraintFactory.coverSize(database, freq, x).post();
        ConstraintFactory.coverClosure(database, x).post();

        Solver solver = model.getSolver();

        List<MiningResult> results = new ArrayList<>();

        while (solver.solve()) {
            MiningResult result = getMiningResult(database, x, freq);
            results.add(result);
        }

        return results;
    }

    private TransactionalDatabase readTransactionalDatabase(String datasetPath) throws Exception {
        URL url = ChocoMiner.class.getResource(datasetPath);
        String path = URLDecoder.decode(url.getPath(), "UTF-8");
        return new DatReader(path).read();
    }

    private int[] parseArrayParameter(String param, int nbItems) {
        int[] array = new int[nbItems];
        if (param != null && !param.isEmpty()) {
            String[] indices = param.split(",");
            for (String idxStr : indices) {
                int idx = Integer.parseInt(idxStr.trim()) - 1; // Convert to 0-based index
                if (idx >= 0 && idx < array.length) {
                    array[idx] = 1;
                }
            }
        }
        return array;
    }

    private MiningResult getMiningResult(TransactionalDatabase database, BoolVar[] x, IntVar freq) {
        List<Integer> itemset = new ArrayList<>();
        for (int i = 0; i < x.length; i++) {
            if (x[i].getValue() == 1) {
                itemset.add(database.getItems()[i]);
            }
        }
        return new MiningResult(itemset, freq.getValue());
    }
}
