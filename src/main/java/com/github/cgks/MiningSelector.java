package com.github.cgks;

import com.github.cgks.choco.ChocoMiner;
import com.github.cgks.spmf.SpmfMiner;

public class MiningSelector {

    public static Miner chooseMiner(MiningRequest request) {
        // TODO(jewin): Implement the logic to choose the mining engine based on the
        // request.
        // For now, we will just return randomly either ChocoMining or SPMF.
        if (true) {
            return new ChocoMiner();
        } else {
            return new SpmfMiner();
        }
    }
}
