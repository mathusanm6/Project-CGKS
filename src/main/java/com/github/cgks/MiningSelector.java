package com.github.cgks;

import com.github.cgks.choco.ChocoMiner;
import com.github.cgks.spmf.SpmfMiner;

public class MiningSelector {
    public static Miner chooseMiner(MiningRequest request) {
        if (request.getEngine() == null || request.getEngine().toLowerCase().equals("auto")) {
            // TODO: Implement logic to choose the best engine
            return new SpmfMiner();
        } else if (request.getEngine().toLowerCase().equals("spmf")) {
            return new SpmfMiner();
        } else if (request.getEngine().toLowerCase().equals("choco-mining")) {
            return new ChocoMiner();
        } else {
            throw new IllegalArgumentException("Unknown engine: " + request.getEngine().toLowerCase());
        }
    }
}
