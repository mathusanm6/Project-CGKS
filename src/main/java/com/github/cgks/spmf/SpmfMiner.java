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
        return new ArrayList<>();
    }

    public static List<MiningResult> extractClosed(String datasetPath, Map<String, String> params) {
        return new ArrayList<>();
    }
}
