package com.github.cgks.spmf;

//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.cgks.Miner;
import com.github.cgks.MiningResult;

// import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;

public class SpmfMiner implements Miner{
    // TODO(unknown): Implement this class to use SPMF for mining patterns.

    @Override
    public  List<MiningResult> extractFrequent(String datasetPath, Map<String, String> params) {
        return new ArrayList<>();
    }

    @Override
    public List<MiningResult> extractClosed(String datasetPath, Map<String, String> params) {
        return new ArrayList<>();
    }

    @Override
    public  List<MiningResult> extractMaximal(String datasetPath, Map<String, String> params) {
        return new ArrayList<>();
    }

    @Override
    public List<MiningResult> extractRare(String datasetPath, Map<String, String> params) {
        return new ArrayList<>();
    }

    @Override
    public  List<MiningResult> extractGenerators(String datasetPath, Map<String, String> params) {
        return new ArrayList<>();
    }

    @Override
    public List<MiningResult> extractMinimal(String datasetPath, Map<String, String> params) {
        return new ArrayList<>();
    }

    @Override
    public  List<MiningResult> extractSizeBetween(String datasetPath, Map<String, String> params) {
        return new ArrayList<>();
    }

    @Override
    public List<MiningResult> extractPresence(String datasetPath, Map<String, String> params) {
        return new ArrayList<>();
    }

    @Override
    public  List<MiningResult> extractAbsence(String datasetPath, Map<String, String> params) {
        return new ArrayList<>();
    }

}
