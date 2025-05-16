package com.github.cgks.spmf;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import com.github.cgks.MiningResult;

import java.util.ArrayList;
import java.util.List;

public class ConvertToMiningResult {

    public static List<MiningResult> convertItemsetsToMiningResults(Itemsets itemsets) {
        List<MiningResult> resultList = new ArrayList<>();

        for (List<Itemset> level : itemsets.getLevels()) {
            for (Itemset itemset : level) {
                int[] itemsArray = itemset.getItems();
                List<Integer> pattern = new ArrayList<>();
                for (int item : itemsArray) {
                    pattern.add(item);
                }
                int freq = itemset.getAbsoluteSupport();
                resultList.add(new MiningResult(pattern, freq));
            }
        }

        return resultList;
    }
    
}
