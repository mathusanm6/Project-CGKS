package com.github.cgks;

import java.util.List;

public interface MiningEngine {
    List<MiningResult> runMining(MiningRequest request);
}
