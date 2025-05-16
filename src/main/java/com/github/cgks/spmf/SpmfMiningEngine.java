package com.github.cgks.spmf;

import java.util.List;
import java.util.Map;

import com.github.cgks.MiningEngine;
import com.github.cgks.MiningRequest;
import com.github.cgks.MiningResult;

public class SpmfMiningEngine implements MiningEngine {
    @Override
    public List<MiningResult> runMining(MiningRequest request) {
        // TODO(unknown): Implement the logic to run the mining algorithm using SPMF.

        String queryType = request.getQueryType();
        String datasetPath = "chemin/vers/spmf/" + request.getDataset();
        Map<String, String> params = request.getParams();

        switch (queryType) {
            case "frequent":
                return SpmfMiner.extractFrequent(datasetPath, params);
            case "closed":
                return SpmfMiner.extractClosed(datasetPath, params);
            default:
                throw new IllegalArgumentException("Unknown query type: " + queryType);
        }
    }

}
