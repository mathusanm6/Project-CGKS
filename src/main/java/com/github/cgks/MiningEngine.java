package com.github.cgks;

import java.util.List;
import java.util.Map;

public class MiningEngine {

    public List<MiningResult> runMining(MiningRequest request) throws Exception {
        String queryType = request.getQueryType();
        String datasetPath = request.getDataset();
        Map<String, String> params = request.getParams();
        Miner miner = MiningSelector.chooseMiner(request);

        switch (queryType) {
            case "frequent":
                return miner.extractFrequent(datasetPath, params);
            case "closed":
                return miner.extractClosed(datasetPath, params);
            case "maximal":
                return miner.extractMaximal(datasetPath, params);
            case "rare":
                return miner.extractRare(datasetPath, params);
            case "generators":
                return miner.extractGenerators(datasetPath, params);
            case "minimal":
                return miner.extractMinimal(datasetPath, params);
            case "sizeBetween":
                return miner.extractSizeBetween(datasetPath, params);
            case "presence":
                return miner.extractPresence(datasetPath, params);
            case "absence":
                return miner.extractAbsence(datasetPath, params);
            default:
                throw new IllegalArgumentException("Unknown query type: " + queryType);
        }
    }

}
