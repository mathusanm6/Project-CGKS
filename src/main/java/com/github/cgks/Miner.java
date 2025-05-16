package com.github.cgks;

import java.util.List;
import java.util.Map;

public interface Miner {

    List<MiningResult> extractFrequent(String datasetPath, Map<String, String> params);

    List<MiningResult> extractClosed(String datasetPath, Map<String, String> params);

    List<MiningResult> extractMaximal(String datasetPath, Map<String, String> params);

    List<MiningResult> extractRare(String datasetPath, Map<String, String> params);

    List<MiningResult> extractGenerators(String datasetPath, Map<String, String> params);

    List<MiningResult> extractMinimal(String datasetPath, Map<String, String> params);

    List<MiningResult> extractSizeBetween(String datasetPath, Map<String, String> params);

    List<MiningResult> extractPresence(String datasetPath, Map<String, String> params);

    List<MiningResult> extractAbsence(String datasetPath, Map<String, String> params);
}
