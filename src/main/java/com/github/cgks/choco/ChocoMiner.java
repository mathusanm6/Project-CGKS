package com.github.cgks.choco;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.cgks.Miner;
import com.github.cgks.MiningResult;

// import io.gitlab.chaver.mining.patterns.io.TransactionalDatabase;

public class ChocoMiner implements Miner {
    // TODO(unknown): Implement this class to use Choco solver for mining patterns.
    // This class should contain all the methods needed.

    public List<MiningResult> extractFrequent(String datasetPath, Map<String, String> params) {
        List<MiningResult> results = new ArrayList<>();
        return results;
    }

    public List<MiningResult> extractClosed(String datasetPath, Map<String, String> params) {
        List<MiningResult> results = new ArrayList<>();
        return results;
    }
}
