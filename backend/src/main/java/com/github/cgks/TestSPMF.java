package com.github.cgks;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;
import ca.pfv.spmf.algorithms.frequentpatterns.lcm.AlgoLCM;
import ca.pfv.spmf.algorithms.frequentpatterns.lcm.Dataset;



public class TestSPMF {
    public static void main(String [] arg) throws IOException{
		String input = fileToPath("/data/contextPasquier99.dat");
		
		double minsup = 0.4; // means a minsup of 2 transaction (we used a relative support)
		Dataset dataset = new Dataset(input);
		
		// Applying the algorithm
		AlgoLCM algo = new AlgoLCM();
		// if true in next line it will find only closed itemsets, otherwise, all frequent itemsets
		Itemsets itemsets = algo.runAlgorithm(minsup, dataset, null);
		algo.printStats();
		
		itemsets.printItemsets(dataset.getTransactions().size());
	}
	
	public static String fileToPath(String filename) throws UnsupportedEncodingException{
		URL url = TestSPMF.class.getResource(filename);
		return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
	}
}
