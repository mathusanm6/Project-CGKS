package com.github.cgks.spmf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.cgks.MiningResult;

import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;

public class SpmfMiner {
    // TODO(unknown): Implement this class to use SPMF for mining patterns.

    public static List<MiningResult> extractFrequent(String datasetPath, Map<String, String> params) {
        double minsup = Double.parseDouble(params.getOrDefault("minSupp", "60")) / 100.0;
        String outputPath = "output/spmf_frequent.txt";

        try {
            AlgoFPGrowth algo = new AlgoFPGrowth();
            algo.runAlgorithm(datasetPath, outputPath, minsup);
            algo.printStats();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }

        return parseSpmfOutput(outputPath);
    }

    public static List<MiningResult> extractClosed(String datasetPath, Map<String, String> params) {
        double minsup = Double.parseDouble(params.getOrDefault("minSupp", "60")) / 100.0;
        String outputPath = "output/spmf_closed.txt";

        try {
            AlgoClose algo = new AlgoClose();
            algo.runAlgorithm(minsup, datasetPath, outputPath);
            algo.printStats();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }

        return parseSpmfOutput(outputPath);
    }

    private static List<MiningResult> parseSpmfOutput(String path) {
        List<MiningResult> results = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.contains("#SUP:"))
                    continue;

                String[] parts = line.split("#SUP:");
                String[] items = parts[0].trim().split(" ");
                List<Integer> pattern = new ArrayList<>();
                for (String item : items) {
                    if (!item.isBlank()) {
                        pattern.add(Integer.parseInt(item));
                    }
                }
                int freq = Integer.parseInt(parts[1].trim());
                results.add(new MiningResult(pattern, freq));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return results;
    }
}
