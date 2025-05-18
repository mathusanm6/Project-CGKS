package com.github.cgks.spmf;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.cgks.Miner;
import com.github.cgks.MiningResult;
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

// import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;

public class SpmfMiner implements Miner {
    // TODO(unknown): Implement this class to use SPMF for mining patterns.

    public static String fileToPath(String file) throws UnsupportedEncodingException{
		URL url = TestSPMF.class.getResource(file);
		return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
	}

    public static Dataset pathToDataset(String path) throws UnsupportedEncodingException, IOException{
        String input = fileToPath(path); 
        Dataset dataset = new Dataset(input);
        return dataset;
    }

    @Override
    public List<MiningResult> extractFrequent(String datasetPath, Map<String, String> params) throws IOException{
		Dataset dataset = pathToDataset(datasetPath);
		Double minSupport = Double.parseDouble(params.get("minSupport"));
		AlgoLCMFreq algo = new AlgoLCMFreq();
		Itemsets itemsets = algo.runAlgorithm(minSupport, dataset, null);
		return ConvertToMiningResult.convertItemsetsToMiningResults(itemsets);
	}

    @Override
    public List<MiningResult> extractClosed(String datasetPath, Map<String, String> params) throws Exception {
        Dataset dataset = pathToDataset(datasetPath);
		Double minSupport = Double.parseDouble(params.get("minSupport"));
		AlgoLCM algo = new AlgoLCM();
		Itemsets itemsets = algo.runAlgorithm(minSupport, dataset, null);
		return ConvertToMiningResult.convertItemsetsToMiningResults(itemsets);
    }

    @Override
    public List<MiningResult> extractMaximal(String datasetPath, Map<String, String> params) throws Exception {
        Double minSupport = Double.parseDouble(params.get("minSupport"));
		// AlgoLCMMax algo = new AlgoLCMMax(); // Algo avec la version locale Non dispo
		AlgoFPMax algo = new AlgoFPMax();
		Itemsets itemsets = algo.runAlgorithm(fileToPath(datasetPath), null, minSupport);
		return ConvertToMiningResult.convertItemsetsToMiningResults(itemsets);
	}

    @Override
    public List<MiningResult> extractRare(String datasetPath, Map<String, String> params) throws Exception {
        Double minSupport = Double.parseDouble(params.get("minSupport"));
		AlgoRPGrowth algo = new AlgoRPGrowth(); // Introuvable
		Itemsets itemsets = algo.runAlgorithm(fileToPath(datasetPath), null, minSupport, 0);
		return ConvertToMiningResult.convertItemsetsToMiningResults(itemsets);
    }

    @Override
    public List<MiningResult> extractGenerators(String datasetPath, Map<String, String> params) throws Exception {
		TransactionDatabase context = new TransactionDatabase();
		context.loadFile(fileToPath(datasetPath));
        Double minSupport = Double.parseDouble(params.get("minSupport"));
		AlgoZart algo = new AlgoZart();		
		TZTableClosed results = algo.runAlgorithm(context, minSupport);
		Itemsets itemsets = new Itemsets("Generator itemset");
		for(int i=0; i< results.levels.size(); i++){
			for(Itemset closed : results.levels.get(i)){
				List<Itemset> generators = results.mapGenerators.get(closed);
				// if there are some generators
				if(generators.size()!=0) { 
					for(Itemset generator : generators){
						itemsets.addItemset(generator, i);
					}
				}else {
					// otherwise the closed itemset is a generator
					itemsets.addItemset(closed, i);
				}
			}
		}
		return ConvertToMiningResult.convertItemsetsToMiningResults(itemsets);
    }

    @Override
    public List<MiningResult> extractMinimal(String datasetPath, Map<String, String> params) throws Exception {
		Double minSupport = Double.parseDouble(params.get("minSupport"));
		//AlgoLCMMax algo = new AlgoLCMMax(); // Algo avec la version locale Non dispo
		AlgoAprioriRare algo = new AlgoAprioriRare();
		Itemsets itemsets = algo.runAlgorithm(minSupport, fileToPath(datasetPath), null);
        return ConvertToMiningResult.convertItemsetsToMiningResults(itemsets);
    }

    @Override
    public List<MiningResult> extractSizeBetween(String datasetPath, Map<String, String> params) throws Exception {
        Dataset dataset = pathToDataset(datasetPath);
        int minSize = Integer.parseInt(params.get("minSize"));
        int maxSize = Integer.parseInt(params.get("maxSize"));
        Double minSupport = Double.parseDouble(params.get("minSupport"));
        AlgoLCM algo = new AlgoLCM();
		Itemsets itemsets = algo.runAlgorithm(minSupport, dataset, null);
		
		Itemsets results = new Itemsets( 
			String.format("Itemsets de taille %d à %d", minSize, maxSize)
		);
		
		// Validation des paramètres
		if (minSize < 0 || maxSize < minSize) {
			throw new IllegalArgumentException("Tailles invalides");
		}


		for (List<Itemset> level : itemsets.getLevels()) {
			for (Itemset itemset : level) {
				int size = itemset.size();
				if (size >= minSize && size <= maxSize) {
					results.addItemset(itemset, size);
				}
            }
		}

		return ConvertToMiningResult.convertItemsetsToMiningResults(results);
    }

    @Override
    public List<MiningResult> extractPresence(String datasetPath, Map<String, String> params) throws Exception {
        return new ArrayList<>();
    }

    @Override
    public List<MiningResult> extractAbsence(String datasetPath, Map<String, String> params) throws Exception {
        return new ArrayList<>();
    }
}
