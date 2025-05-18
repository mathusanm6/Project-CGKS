package com.github.cgks;


import jep.Jep;
import jep.JepException;

import com.github.cgks.choco.ChocoMiner;
import com.github.cgks.spmf.SpmfMiner;

public class MiningSelector {
    private Jep jep;

    public static Miner chooseMiner(MiningRequest request) throws JepException{
        // TODO(jewin): Implement the logic to choose the mining engine based on the
        // request.
        // For now, we will just return randomly either ChocoMining or SPMF.
        
        if (true) {
            return new SpmfMiner();
        } else {
            return new ChocoMiner();
        }
    }

    public static void main(String[] args) {
        jep = new Jep();
        // jep.eval("import pickle");
        // jep.eval("import numpy as np");
        // jep.eval("with open('f'{SAVED_PIPELINE_FOLDER}/{V_PIPELINE}/pipeline.pkl'', 'rb') as f:");
        // jep.eval("    model = pickle.load(f)");
        jep.eval("print(4)");

    }
}
