package com.github.cgks;

import java.util.List;
import java.util.Map;

public class MiningEngine {

    public List<MiningResult> runMining(MiningRequest request) throws Exception {
        String queryType = request.getQueryType();
        String datasetName = request.getDataset();
        Map<String, String> params = request.getParams();
        Miner miner = MiningSelector.chooseMiner(request);

        switch (queryType) {
            case "frequent":
                return miner.extractFrequent(datasetName, params);
            case "closed":
                return miner.extractClosed(datasetName, params);
            case "maximal":
                return miner.extractMaximal(datasetName, params);
            case "rare":
                return miner.extractRare(datasetName, params);
            case "generators":
                return miner.extractGenerators(datasetName, params);
            case "minimal":
                return miner.extractMinimal(datasetName, params);
            case "sizeBetween":
                return miner.extractSizeBetween(datasetName, params);
            case "presence":
                return miner.extractPresence(datasetName, params);
            case "absence":
                return miner.extractAbsence(datasetName, params);
            default:
                throw new IllegalArgumentException("Unknown query type: " + queryType);
        }
    }

}
