package com.github.cgks;

import java.util.List;
import java.util.Map;

public class MiningEngine {

    public List<MiningResult> runMining(MiningRequest request) throws Exception {
        // TODO(unknown): Implement the logic to run the mining algorithm using SPMF.

        String queryType = request.getQueryType();
        String datasetName = request.getDataset();
        Map<String, String> params = request.getParams();
        Miner miner = MiningSelector.chooseMiner(request);

        switch (queryType) {
            case "frequent":
                return miner.extractFrequent(datasetName, params);
            case "closed":
                return miner.extractClosed(datasetName, params);
            default:
                throw new IllegalArgumentException("Unknown query type: " + queryType);
        }
    }

}
