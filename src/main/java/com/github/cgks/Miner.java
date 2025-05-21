package com.github.cgks;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public interface Miner {

    List<MiningResult> extractFrequent(String datasetPath, Map<String, String> params, BooleanSupplier cancellationChecker) throws Exception;

    List<MiningResult> extractClosed(String datasetPath, Map<String, String> params, BooleanSupplier cancellationChecker) throws Exception;

    List<MiningResult> extractMaximal(String datasetPath, Map<String, String> params, BooleanSupplier cancellationChecker) throws Exception;

    List<MiningResult> extractRare(String datasetPath, Map<String, String> params, BooleanSupplier cancellationChecker) throws Exception;

    List<MiningResult> extractGenerators(String datasetPath, Map<String, String> params, BooleanSupplier cancellationChecker) throws Exception;

    List<MiningResult> extractMinimal(String datasetPath, Map<String, String> params, BooleanSupplier cancellationChecker) throws Exception;

    List<MiningResult> extractSizeBetween(String datasetPath, Map<String, String> params, BooleanSupplier cancellationChecker) throws Exception;

    List<MiningResult> extractPresence(String datasetPath, Map<String, String> params, BooleanSupplier cancellationChecker) throws Exception;

    List<MiningResult> extractAbsence(String datasetPath, Map<String, String> params, BooleanSupplier cancellationChecker) throws Exception;
}
