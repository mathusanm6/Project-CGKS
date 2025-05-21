package com.github.cgks;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class MiningEngine {

    public List<MiningResult> runMining(MiningRequest request, BooleanSupplier cancellationChecker) throws Exception {
        String queryType = request.getQueryType();
        String datasetPath = request.getDataset();
        Map<String, String> params = request.getParams();
        Miner miner = MiningSelector.chooseMiner(request);

        switch (queryType) {
            case "frequent":
                if (cancellationChecker.getAsBoolean())
                    throw new InterruptedException("Mining cancelled");
                return miner.extractFrequent(datasetPath, params, cancellationChecker);
            case "closed":
                if (cancellationChecker.getAsBoolean())
                    throw new InterruptedException("Mining cancelled");
                return miner.extractClosed(datasetPath, params, cancellationChecker);
            case "maximal":
                if (cancellationChecker.getAsBoolean())
                    throw new InterruptedException("Mining cancelled");
                return miner.extractMaximal(datasetPath, params, cancellationChecker);
            case "rare":
                if (cancellationChecker.getAsBoolean())
                    throw new InterruptedException("Mining cancelled");
                return miner.extractRare(datasetPath, params, cancellationChecker);
            case "generators":
                if (cancellationChecker.getAsBoolean())
                    throw new InterruptedException("Mining cancelled");
                return miner.extractGenerators(datasetPath, params, cancellationChecker);
            case "minimal":
                if (cancellationChecker.getAsBoolean())
                    throw new InterruptedException("Mining cancelled");
                return miner.extractMinimal(datasetPath, params, cancellationChecker);
            case "size_between":
                if (cancellationChecker.getAsBoolean())
                    throw new InterruptedException("Mining cancelled");
                return miner.extractSizeBetween(datasetPath, params, cancellationChecker);
            case "presence":
                if (cancellationChecker.getAsBoolean())
                    throw new InterruptedException("Mining cancelled");
                return miner.extractPresence(datasetPath, params, cancellationChecker);
            case "absence":
                if (cancellationChecker.getAsBoolean())
                    throw new InterruptedException("Mining cancelled");
                return miner.extractAbsence(datasetPath, params, cancellationChecker);
            default:
                throw new IllegalArgumentException("Unknown query type: " + queryType);
        }
    }

    public List<MiningResult> runMining(MiningRequest request) throws Exception {
        return runMining(request, () -> false);
    }

}
