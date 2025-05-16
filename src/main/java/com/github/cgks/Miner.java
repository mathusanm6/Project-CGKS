package com.github.cgks;

import java.util.List;
import java.util.Map;

public interface Miner {

    List<MiningResult> extractFrequent(String datasetPath, Map<String, String> params);

    List<MiningResult> extractClosed(String datasetPath, Map<String, String> params);

    // TODO: add other methods for other mining tasks
}
