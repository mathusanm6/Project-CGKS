package com.github.cgks.choco;

import java.util.List;
import java.util.Map;

import com.github.cgks.MiningEngine;
import com.github.cgks.MiningRequest;
import com.github.cgks.MiningResult;

import io.gitlab.chaver.mining.patterns.io.DatReader;
import io.gitlab.chaver.mining.patterns.io.TransactionalDatabase;

public class ChocoMiningEngine implements MiningEngine {

    @Override
    public List<MiningResult> runMining(MiningRequest request) {
        // TODO(unknown): Implement the logic to run the mining algorithm using
        // ChocoMiner.

        String queryType = request.getQueryType();
        TransactionalDatabase db;
        try {
            db = new DatReader("chemin/vers/" + request.getDataset()).read();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read dataset file", e);
        }
        Map<String, String> params = request.getParams();

        switch (queryType) {
            case "frequent":
                return ChocoMiner.extractFrequent(db, params);
            case "closed":
                return ChocoMiner.extractClosed(db, params);
            // autres cas possibles
            default:
                throw new IllegalArgumentException("Unknown query type: " + queryType);
        }
    }
}
