package com.github.cgks;

import com.github.cgks.choco.ChocoMiningEngine;
import com.github.cgks.spmf.SpmfMiningEngine;

public class MiningSelector {

    public static MiningEngine chooseEngine(MiningRequest request) {
        // TODO(jewin): Implement the logic to choose the mining engine based on the
        // request.
        // For now, we will just return randomly either ChocoMining or SPMF.
        if (Math.random() < 0.5) {
            return new ChocoMiningEngine();
        } else {
            return new SpmfMiningEngine();
        }
    }
}
