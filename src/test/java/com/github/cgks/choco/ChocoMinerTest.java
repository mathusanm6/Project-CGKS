package com.github.cgks.choco;

import com.github.cgks.Miner;
import com.github.cgks.MiningResult;
import com.github.cgks.exceptions.MiningException;
import com.github.cgks.exceptions.ParameterException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ChocoMiner implementation with contextPasquier99.dat
 * dataset.
 * Dataset content:
 * 1 3 4
 * 2 3 5
 * 1 2 3 5
 * 2 5
 * 1 2 3 5
 * 
 * Item frequencies:
 * - Item 1: 3 transactions (support = 0.6)
 * - Item 2: 4 transactions (support = 0.8)
 * - Item 3: 4 transactions (support = 0.8)
 * - Item 4: 1 transaction (support = 0.2)
 * - Item 5: 4 transactions (support = 0.8)
 */
class ChocoMinerTest {
    private Miner miner;
    private String datasetPath;
    private static final String DEFAULT_DATASET_PATH = "src/test/resources/contextPasquier99.dat";
    private static final String MIN_SUPPORT_PARAM = "minSupport";
    private static final String MAX_SUPPORT_PARAM = "maxSupport";
    private static final String MIN_SIZE_PARAM = "minSize";
    private static final String MAX_SIZE_PARAM = "maxSize";
    private static final String ITEMS_PARAM = "items";

    // Timeout for potentially long operations
    private static final Duration OPERATION_TIMEOUT = Duration.ofSeconds(30);

    // --- IMMUTABLE PARAMETERS BUILDER ---
    private static final class Parameters {

        private final Map<String, String> parameters;

        private Parameters(Map<String, String> initialParams) {
            this.parameters = Collections.unmodifiableMap(new HashMap<>(initialParams));
        }

        public static Parameters empty() {
            return new Parameters(Collections.emptyMap());
        }

        public Parameters withParam(String key, double value) {
            Map<String, String> newParameters = new HashMap<>(this.parameters);
            newParameters.put(key, String.valueOf(value));
            return new Parameters(newParameters);
        }

        public Parameters withParam(String key, int value) {
            Map<String, String> newParameters = new HashMap<>(this.parameters);
            newParameters.put(key, String.valueOf(value));
            return new Parameters(newParameters);
        }

        public Parameters withParam(String key, String value) {
            Map<String, String> newParameters = new HashMap<>(this.parameters);
            newParameters.put(key, value);
            return new Parameters(newParameters);
        }

        public Map<String, String> getHashMap() {
            return this.parameters;
        }
    }
    // --- END OF PARAMETERS BUILDER ---

    @BeforeEach
    void setUp() {
        miner = new ChocoMiner();
        datasetPath = System.getProperty("dataset.path", DEFAULT_DATASET_PATH);
    }

    /**
     * Helper method to convert a list of MiningResult to a set of sets for
     * order-independent comparison
     */
    private Set<Set<Integer>> convertToCanonicalForm(List<MiningResult> results) {
        return results.stream()
                .map(r -> new TreeSet<>(r.getPattern()))
                .collect(Collectors.toSet());
    }

    /**
     * Helper method for validating that all result patterns meet the minimum
     * support threshold
     */
    private void validateMinimumSupport(List<MiningResult> results, double minSupport) {
        int minFreq = (int) Math.ceil(5 * minSupport); // 5 transactions total
        for (MiningResult result : results) {
            assertTrue(result.getFreq() >= minFreq,
                    "Pattern " + result.getPattern() + " has frequency " + result.getFreq()
                            + " which is below minimum " + minFreq);
        }
    }

    /**
     * Helper method for validating that all result patterns meet the maximum
     * support threshold
     */
    private void validateMaximumSupport(List<MiningResult> results, double maxSupport) {
        int maxFreq = (int) Math.floor(5 * maxSupport); // 5 transactions total
        for (MiningResult result : results) {
            assertTrue(result.getFreq() <= maxFreq,
                    "Pattern " + result.getPattern() + " has frequency " + result.getFreq()
                            + " which is above maximum " + maxFreq);
        }
    }

    /**
     * Helper method to check if expected itemsets are present in the results
     */
    private void assertContainsItemsets(List<MiningResult> results, Set<Set<Integer>> expectedItemsets) {
        Set<Set<Integer>> resultSets = convertToCanonicalForm(results);
        for (Set<Integer> expected : expectedItemsets) {
            assertTrue(resultSets.contains(expected),
                    "Expected itemset " + expected + " not found in results. Actual results: " + resultSets);
        }
    }

    /**
     * Helper method to check if results exactly match expected itemsets
     */
    private void assertExactItemsets(List<MiningResult> results, Set<Set<Integer>> expectedItemsets) {
        Set<Set<Integer>> resultSets = convertToCanonicalForm(results);
        assertEquals(expectedItemsets, resultSets);
    }

    @Nested
    @DisplayName("Frequent Itemset Mining Tests")
    class FrequentItemsetMiningTests {

        @Test
        @DisplayName("Support 0.6 (absolute 3) - Should include items 1, 2, 3, 5 and valid combinations")
        void testExtractFrequent_Support60Percent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 0.6).getHashMap();
                List<MiningResult> results = miner.extractFrequent(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.6);

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(3)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(5)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1, 3)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2, 3)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2, 5)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(3, 5)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2, 3, 5)));

                assertContainsItemsets(results, expectedItemsets);
            });
        }

        @Test
        @DisplayName("Support 0.8 (absolute 4) - Should include only items 2, 3, 5 and their valid combinations")
        void testExtractFrequent_Support80Percent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 0.8).getHashMap();
                List<MiningResult> results = miner.extractFrequent(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.8);

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(3)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(5)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2, 5)));

                assertExactItemsets(results, expectedItemsets);
            });
        }

        @Test
        @DisplayName("Support 0.2 (absolute 1) - Should include all single items")
        void testExtractFrequent_Support20Percent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 0.2).getHashMap();
                List<MiningResult> results = miner.extractFrequent(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.2);

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(3)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(4)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(5)));

                assertContainsItemsets(results, expectedItemsets);
            });
        }

        @Test
        @DisplayName("Support 1.0 (absolute 5) - Should return empty result")
        void testExtractFrequent_Support100Percent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 1.0).getHashMap();
                List<MiningResult> results = miner.extractFrequent(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertTrue(results.isEmpty(), "Results should be empty as no itemset has 100% support");
            });
        }

        @Test
        @DisplayName("Invalid Parameters - Negative Support")
        void testExtractFrequent_NegativeSupport() {
            Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, -0.1).getHashMap();
            assertThrows(ParameterException.class, () -> miner.extractFrequent(datasetPath, params));
        }

        @Test
        @DisplayName("Invalid Parameters - Support > 1")
        void testExtractFrequent_SupportGreaterThanOne() {
            Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 1.1).getHashMap();
            assertThrows(ParameterException.class, () -> miner.extractFrequent(datasetPath, params));
        }

        @Test
        @DisplayName("Invalid Parameters - Missing Support")
        void testExtractFrequent_MissingSupport() {
            Map<String, String> params = Parameters.empty().getHashMap();
            assertThrows(ParameterException.class, () -> miner.extractFrequent(datasetPath, params));
        }
    }

    @Nested
    @DisplayName("Closed Itemset Mining Tests")
    class ClosedItemsetMiningTests {

        @Test
        @DisplayName("Support 0.6 (absolute 3) - Should return closed patterns")
        void testExtractClosed_Support60Percent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 0.6).getHashMap();
                List<MiningResult> results = miner.extractClosed(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.6);

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(3)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1, 3)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2, 5)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2, 3, 5)));

                assertExactItemsets(results, expectedItemsets);
            });
        }

        @Test
        @DisplayName("Support 0.8 (absolute 4) - Should include {2,5} and {3} as they are closed")
        void testExtractClosed_Support80Percent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 0.8).getHashMap();
                List<MiningResult> results = miner.extractClosed(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.8);

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2, 5)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(3)));

                assertExactItemsets(results, expectedItemsets);
            });
        }

        @Test
        @DisplayName("Support 0.2 (absolute 1) - All closed patterns in the dataset")
        void testExtractClosed_Support20Percent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 0.2).getHashMap();
                List<MiningResult> results = miner.extractClosed(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.2);

                Set<Set<Integer>> itemsetsWithFour = new HashSet<>();
                itemsetsWithFour.add(new TreeSet<>(Arrays.asList(1, 3, 4)));

                assertContainsItemsets(results, itemsetsWithFour);
            });
        }

        @Test
        @DisplayName("Invalid Parameters - Negative Support")
        void testExtractClosed_NegativeSupport() {
            Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, -0.1).getHashMap();
            assertThrows(ParameterException.class, () -> miner.extractClosed(datasetPath, params));
        }
    }

    @Nested
    @DisplayName("Maximal Itemset Mining Tests")
    class MaximalItemsetMiningTests {

        @Test
        @DisplayName("Support 0.6 (absolute 3) - Should return maximal patterns")
        void testExtractMaximal_Support60Percent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 0.6).getHashMap();
                List<MiningResult> results = miner.extractMaximal(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.6);

                Set<Integer> singleItems = new HashSet<>(Arrays.asList(1, 2, 3, 5));
                for (MiningResult result : results) {
                    assertFalse(result.getPattern().size() == 1 && singleItems.contains(result.getPattern().get(0)),
                            "Single item " + result.getPattern()
                                    + " should not be in maximal results if it has frequent supersets.");
                }

                Set<Set<Integer>> expectedMaximalItemsets = new HashSet<>();
                expectedMaximalItemsets.add(new TreeSet<>(Arrays.asList(1, 3)));
                expectedMaximalItemsets.add(new TreeSet<>(Arrays.asList(2, 3, 5)));
                assertContainsItemsets(results, expectedMaximalItemsets);
            });
        }

        @Test
        @DisplayName("Support 0.8 (absolute 4) - Should include only {2,5} and {3}")
        void testExtractMaximal_Support80Percent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 0.8).getHashMap();
                List<MiningResult> results = miner.extractMaximal(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.8);

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2, 5)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(3)));

                assertExactItemsets(results, expectedItemsets);
            });
        }

        @Test
        @DisplayName("Support 0.2 (absolute 1) - Should return only the largest patterns")
        void testExtractMaximal_Support20Percent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 0.2).getHashMap();
                List<MiningResult> results = miner.extractMaximal(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.2);

                Set<Set<Integer>> expectedMaximalItemsets = new HashSet<>();
                expectedMaximalItemsets.add(new TreeSet<>(Arrays.asList(1, 3, 4)));
                expectedMaximalItemsets.add(new TreeSet<>(Arrays.asList(1, 2, 3, 5)));

                assertContainsItemsets(results, expectedMaximalItemsets);

                for (MiningResult result : results) {
                    assertFalse(result.getPattern().size() == 1 && result.getFreq() > 0, // only check if freq > 0
                            "No single item should be in maximal results at support 0.2 if they have frequent supersets.");
                }
            });
        }

        @Test
        @DisplayName("Invalid Parameters - Negative Support")
        void testExtractMaximal_NegativeSupport() {
            Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, -0.1).getHashMap();
            assertThrows(ParameterException.class, () -> miner.extractMaximal(datasetPath, params));
        }
    }

    @Nested
    @DisplayName("Rare Itemset Mining Tests")
    class RareItemsetMiningTests {
        @Test
        @DisplayName("MaxSupport derived from 0.4 (absolute 2) - Should include only item 4 and its combinations")
        void testExtractRare_MaxSupport40Percent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MAX_SUPPORT_PARAM, 0.4).getHashMap();
                List<MiningResult> results = miner.extractRare(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMaximumSupport(results, 0.4);

                for (MiningResult result : results) {
                    assertTrue(result.getPattern().contains(4),
                            "All rare itemsets should contain item 4, but found: " + result.getPattern());
                    assertEquals(1, result.getFreq(), "Frequency of rare itemset should be 1 for this test.");
                }

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(4)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1, 4)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(3, 4)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1, 3, 4)));

                assertExactItemsets(results, expectedItemsets);
            });
        }

        @Test
        @DisplayName("MaxSupport derived from 0.3 (absolute support < 2) - Should include only item 4 and combinations")
        void testExtractRare_MaxSupport30Percent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MAX_SUPPORT_PARAM, 0.3).getHashMap();
                List<MiningResult> results = miner.extractRare(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMaximumSupport(results, 0.3);

                for (MiningResult result : results) {
                    assertTrue(result.getPattern().contains(4),
                            "All rare itemsets should contain item 4, but found: " + result.getPattern());
                    assertEquals(1, result.getFreq(), "Frequency of rare itemset should be 1 for this test.");
                }

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(4)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1, 4)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(3, 4)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1, 3, 4)));
                assertExactItemsets(results, expectedItemsets);

            });
        }

        @Test
        @DisplayName("Invalid parameters - Too low support threshold")
        void testExtractRare_TooLowSupport() {
            Map<String, String> params = Parameters.empty().withParam(MAX_SUPPORT_PARAM, 0.1).getHashMap();
            assertThrows(ParameterException.class, () -> miner.extractRare(datasetPath, params));
        }
    }

    @Nested
    @DisplayName("Generator Itemset Mining Tests")
    class GeneratorItemsetMiningTests {
        @Test
        @DisplayName("Support 0.6 (absolute 3) - Should return generator patterns")
        void testExtractGenerators_Support60Percent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 0.6).getHashMap();
                List<MiningResult> results = miner.extractGenerators(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.6); // freq >= 3

                Set<Set<Integer>> expectedGenerators = new HashSet<>();
                expectedGenerators.add(new TreeSet<>(Arrays.asList(1)));
                expectedGenerators.add(new TreeSet<>(Arrays.asList(2)));
                expectedGenerators.add(new TreeSet<>(Arrays.asList(3)));
                expectedGenerators.add(new TreeSet<>(Arrays.asList(5)));
                expectedGenerators.add(new TreeSet<>(Arrays.asList(2, 3)));
                expectedGenerators.add(new TreeSet<>(Arrays.asList(3, 5)));
                assertExactItemsets(results, expectedGenerators);
            });
        }

        @Test
        @DisplayName("Support 0.8 (absolute 4) - Should return generators among patterns with items 2, 3, 5")
        void testExtractGenerators_Support80Percent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 0.8).getHashMap();
                List<MiningResult> results = miner.extractGenerators(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.8); // freq >=4

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(3)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(5)));

                assertExactItemsets(results, expectedItemsets);
            });
        }

        @Test
        @DisplayName("Invalid Parameters - Negative Support")
        void testExtractGenerators_NegativeSupport() {
            Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, -0.1).getHashMap();
            assertThrows(ParameterException.class, () -> miner.extractGenerators(datasetPath, params));
        }
    }

    @Nested
    @DisplayName("Minimal Itemset Mining Tests")
    class MinimalItemsetMiningTests {
        @Test
        @DisplayName("MaxSupport derived from 0.4 (maxSupport < 0.4, i.e. freq < 2) - Should find minimal rare itemsets")
        void testExtractMinimal_MaxSupport40Percent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MAX_SUPPORT_PARAM, 0.4).getHashMap();
                List<MiningResult> results = miner.extractMinimal(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMaximumSupport(results, 0.4); // Actually means freq < 2, so freq=1.

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(4)));

                assertExactItemsets(results, expectedItemsets);
            });
        }

        @Test
        @DisplayName("MaxSupport derived from 0.7 (maxSupport < 0.7, i.e. freq < ceil(5*0.7)=4) - Should find minimal rare itemsets with support < 0.7")
        void testExtractMinimal_MaxSupport70Percent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MAX_SUPPORT_PARAM, 0.7).getHashMap();
                List<MiningResult> results = miner.extractMinimal(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMaximumSupport(results, 0.7); // freq <= floor(5*0.7) = 3

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(4)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2, 3)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(3, 5)));

                assertContainsItemsets(results, expectedItemsets);
            });
        }

        @Test
        @DisplayName("Invalid parameters - Too low support threshold")
        void testExtractMinimal_TooLowSupport() {
            Map<String, String> params = Parameters.empty().withParam(MAX_SUPPORT_PARAM, 0.1).getHashMap();
            assertThrows(ParameterException.class, () -> miner.extractMinimal(datasetPath, params));
        }
    }

    @Nested
    @DisplayName("SizeBetween Itemset Mining Tests")
    class SizeBetweenItemsetMiningTests {

        @Test
        @DisplayName("Size 1-1, Support 0.6 - Should return single items with support >= 0.6")
        void testExtractSizeBetween_SingleItems() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty()
                        .withParam(MIN_SUPPORT_PARAM, 0.6)
                        .withParam(MIN_SIZE_PARAM, 1)
                        .withParam(MAX_SIZE_PARAM, 1)
                        .getHashMap();
                List<MiningResult> results = miner.extractSizeBetween(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.6);

                for (MiningResult result : results) {
                    assertEquals(1, result.getPattern().size(),
                            "Pattern size should be exactly 1 but found: " + result.getPattern());
                }

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(3))); // sup 0.8

                assertExactItemsets(results, expectedItemsets);
            });
        }

        @Test
        @DisplayName("Size 2-3, Support 0.6 - Should return patterns of size 2-3 with support >= 0.6")
        void testExtractSizeBetween_SizeTwoToThree() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty()
                        .withParam(MIN_SUPPORT_PARAM, 0.6)
                        .withParam(MIN_SIZE_PARAM, 2)
                        .withParam(MAX_SIZE_PARAM, 3)
                        .getHashMap();
                List<MiningResult> results = miner.extractSizeBetween(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.6);

                for (MiningResult result : results) {
                    assertTrue(result.getPattern().size() >= 2 && result.getPattern().size() <= 3,
                            "Pattern size should be between 2 and 3 but found: " + result.getPattern());
                }

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1, 3)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2, 5)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2, 3, 5)));

                assertExactItemsets(results, expectedItemsets);
            });
        }

        @Test
        @DisplayName("Size 4-5, Support 0.4 - May include {1,2,3,5} if support sufficient")
        void testExtractSizeBetween_SizeFourToFive() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty()
                        .withParam(MIN_SUPPORT_PARAM, 0.4)
                        .withParam(MIN_SIZE_PARAM, 4)
                        .withParam(MAX_SIZE_PARAM, 5)
                        .getHashMap();
                List<MiningResult> results = miner.extractSizeBetween(datasetPath, params);

                assertNotNull(results, "Results should not be null");

                assertFalse(results.isEmpty(), "Results should not be empty if {1,2,3,5} is found");

                validateMinimumSupport(results, 0.4);
                for (MiningResult result : results) {
                    assertTrue(result.getPattern().size() >= 4 && result.getPattern().size() <= 5,
                            "Pattern size should be between 4 and 5 but found: " + result.getPattern());
                }

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1, 2, 3, 5)));
                assertExactItemsets(results, expectedItemsets);

            });
        }

        @Test
        @DisplayName("Invalid parameters - minSize > maxSize")
        void testExtractSizeBetween_InvalidSizeRange() {
            Map<String, String> params = Parameters.empty()
                    .withParam(MIN_SUPPORT_PARAM, 0.6)
                    .withParam(MIN_SIZE_PARAM, 3)
                    .withParam(MAX_SIZE_PARAM, 2)
                    .getHashMap();
            assertThrows(ParameterException.class, () -> miner.extractSizeBetween(datasetPath, params));
        }

        @Test
        @DisplayName("Invalid parameters - minSize < 1")
        void testExtractSizeBetween_InvalidMinSize() {
            Map<String, String> params = Parameters.empty()
                    .withParam(MIN_SUPPORT_PARAM, 0.6)
                    .withParam(MIN_SIZE_PARAM, 0)
                    .withParam(MAX_SIZE_PARAM, 2)
                    .getHashMap();
            assertThrows(ParameterException.class, () -> miner.extractSizeBetween(datasetPath, params));
        }

        @Test
        @DisplayName("Invalid parameters - Missing minSize or maxSize")
        void testExtractSizeBetween_MissingParam() {
            Map<String, String> params = Parameters.empty()
                    .withParam(MIN_SUPPORT_PARAM, 0.6)
                    .withParam(MAX_SIZE_PARAM, 3) // Missing MIN_SIZE_PARAM
                    .getHashMap();
            assertThrows(ParameterException.class, () -> miner.extractSizeBetween(datasetPath, params));

            Map<String, String> params2 = Parameters.empty()
                    .withParam(MIN_SUPPORT_PARAM, 0.6)
                    .withParam(MIN_SIZE_PARAM, 1) // Missing MAX_SIZE_PARAM
                    .getHashMap();
            assertThrows(ParameterException.class, () -> miner.extractSizeBetween(datasetPath, params2));
        }
    }

    @Nested
    @DisplayName("Presence Constraint Mining Tests")
    class PresenceConstraintMiningTests {

        @Test
        @DisplayName("Item 1 must be present, Support 0.6")
        void testExtractPresence_Item1MustBePresent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty()
                        .withParam(MIN_SUPPORT_PARAM, 0.6)
                        .withParam(ITEMS_PARAM, "1")
                        .getHashMap();
                List<MiningResult> results = miner.extractPresence(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.6);

                for (MiningResult result : results) {
                    assertTrue(result.getPattern().contains(1),
                            "All patterns should contain item 1 but found: " + result.getPattern());
                }

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1, 3)));

                assertExactItemsets(results, expectedItemsets);
            });
        }

        @Test
        @DisplayName("Items 1 and 3 must be present, Support 0.6")
        void testExtractPresence_Items1And3MustBePresent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty()
                        .withParam(MIN_SUPPORT_PARAM, 0.6)
                        .withParam(ITEMS_PARAM, "1,3")
                        .getHashMap();
                List<MiningResult> results = miner.extractPresence(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.6);

                for (MiningResult result : results) {
                    assertTrue(result.getPattern().contains(1),
                            "All patterns should contain item 1 but found: " + result.getPattern());
                    assertTrue(result.getPattern().contains(3),
                            "All patterns should contain item 3 but found: " + result.getPattern());
                }

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1, 3)));

                assertExactItemsets(results, expectedItemsets);
            });
        }

        @Test
        @DisplayName("Item 4 must be present, Support 0.6 - Empty result expected")
        void testExtractPresence_Item4MustBePresent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty()
                        .withParam(MIN_SUPPORT_PARAM, 0.6)
                        .withParam(ITEMS_PARAM, "4")
                        .getHashMap();
                List<MiningResult> results = miner.extractPresence(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertTrue(results.isEmpty(),
                        "Results should be empty as no pattern with item 4 has support >= 0.6");
            });
        }

        @Test
        @DisplayName("Invalid item index (out of range), Support 0.6")
        void testExtractPresence_InvalidItemIndex() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty()
                        .withParam(MIN_SUPPORT_PARAM, 0.6)
                        .withParam(ITEMS_PARAM, "99") // Item 99 doesn't exist
                        .getHashMap();
                List<MiningResult> results = miner.extractPresence(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty as it defaults to closed frequent mining");

                Set<Set<Integer>> expectedClosedFrequentItemsets = new HashSet<>();
                expectedClosedFrequentItemsets.add(new TreeSet<>(Arrays.asList(3)));
                expectedClosedFrequentItemsets.add(new TreeSet<>(Arrays.asList(1, 3)));
                expectedClosedFrequentItemsets.add(new TreeSet<>(Arrays.asList(2, 5)));
                expectedClosedFrequentItemsets.add(new TreeSet<>(Arrays.asList(2, 3, 5)));

                assertExactItemsets(results, expectedClosedFrequentItemsets);
            });
        }

        @Test
        @DisplayName("Invalid item format (non-numeric), Support 0.6")
        void testExtractPresence_InvalidItemFormat() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty()
                        .withParam(MIN_SUPPORT_PARAM, 0.6)
                        .withParam(ITEMS_PARAM, "a,b,c")
                        .getHashMap();

                List<MiningResult> results = miner.extractPresence(datasetPath, params);
                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty as it defaults to closed frequent mining");

                Set<Set<Integer>> expectedClosedFrequentItemsets = new HashSet<>();
                expectedClosedFrequentItemsets.add(new TreeSet<>(Arrays.asList(3)));
                expectedClosedFrequentItemsets.add(new TreeSet<>(Arrays.asList(1, 3)));
                expectedClosedFrequentItemsets.add(new TreeSet<>(Arrays.asList(2, 5)));
                expectedClosedFrequentItemsets.add(new TreeSet<>(Arrays.asList(2, 3, 5)));

                assertExactItemsets(results, expectedClosedFrequentItemsets);

            });
        }

        @Test
        @DisplayName("Missing items parameter")
        void testExtractPresence_MissingItemsParam() {
            Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 0.6).getHashMap();
            assertThrows(ParameterException.class, () -> miner.extractPresence(datasetPath, params));
        }
    }

    @Nested
    @DisplayName("Absence Constraint Mining Tests")
    class AbsenceConstraintMiningTests {

        @Test
        @DisplayName("Item 4 must be absent, Support 0.6")
        void testExtractAbsence_Item4MustBeAbsent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty()
                        .withParam(MIN_SUPPORT_PARAM, 0.6)
                        .withParam(ITEMS_PARAM, "4")
                        .getHashMap();
                List<MiningResult> results = miner.extractAbsence(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.6);

                for (MiningResult result : results) {
                    assertFalse(result.getPattern().contains(4),
                            "No pattern should contain item 4 but found: " + result.getPattern());
                }

                Set<Set<Integer>> expectedFrequentItemsets = new HashSet<>();
                expectedFrequentItemsets.add(new TreeSet<>(Arrays.asList(3)));
                expectedFrequentItemsets.add(new TreeSet<>(Arrays.asList(1, 3)));
                expectedFrequentItemsets.add(new TreeSet<>(Arrays.asList(2, 5)));
                expectedFrequentItemsets.add(new TreeSet<>(Arrays.asList(2, 3, 5)));

                assertExactItemsets(results, expectedFrequentItemsets);
            });
        }

        @Test
        @DisplayName("Item 3 must be absent, Support 0.6")
        void testExtractAbsence_Item3MustBeAbsent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty()
                        .withParam(MIN_SUPPORT_PARAM, 0.6)
                        .withParam(ITEMS_PARAM, "3")
                        .getHashMap();
                List<MiningResult> results = miner.extractAbsence(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.6);

                for (MiningResult result : results) {
                    assertFalse(result.getPattern().contains(3),
                            "No pattern should contain item 3 but found: " + result.getPattern());
                }

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2, 5)));

                assertExactItemsets(results, expectedItemsets);
            });
        }

        @Test
        @DisplayName("Items 2 and 5 must be absent, Support 0.2")
        void testExtractAbsence_Items2And5MustBeAbsent() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty()
                        .withParam(MIN_SUPPORT_PARAM, 0.2)
                        .withParam(ITEMS_PARAM, "2,5") // items 2 AND 5 must be absent
                        .getHashMap();
                List<MiningResult> results = miner.extractAbsence(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");
                validateMinimumSupport(results, 0.2);

                for (MiningResult result : results) {
                    assertFalse(result.getPattern().contains(2),
                            "No pattern should contain item 2 but found: " + result.getPattern());
                    assertFalse(result.getPattern().contains(5),
                            "No pattern should contain item 5 but found: " + result.getPattern());
                }

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(3)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1, 3)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1, 3, 4)));

                assertExactItemsets(results, expectedItemsets);
            });
        }

        @Test
        @DisplayName("Missing items parameter")
        void testExtractAbsence_MissingItemsParam() {
            Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 0.6).getHashMap();
            assertThrows(ParameterException.class, () -> miner.extractAbsence(datasetPath, params));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Non-existent Dataset Path")
        void testNonExistentDatasetPath() {
            Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 0.6).getHashMap();
            assertThrows(MiningException.class, () -> miner.extractFrequent("non_existent_path.dat", params));
        }

        @Test
        @DisplayName("Support = 1.0 - Tests behavior at maximum possible support")
        void testMaximumPossibleSupport() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 1.0).getHashMap();
                List<MiningResult> results = miner.extractFrequent(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertTrue(results.isEmpty(), "Results should be empty as no itemset has 100% support");
            });
        }

        @Test
        @DisplayName("Support = 0.0 - Tests behavior at minimum possible support")
        void testMinimumPossibleSupport() {
            Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 0.0).getHashMap();
            assertThrows(ParameterException.class, () -> miner.extractFrequent(datasetPath, params));
        }

        @Test
        @DisplayName("Support just above 0.0 - Tests behavior with very low support")
        void testJustAboveZeroSupport() {
            assertTimeoutPreemptively(OPERATION_TIMEOUT, () -> {
                Map<String, String> params = Parameters.empty().withParam(MIN_SUPPORT_PARAM, 0.01).getHashMap();
                List<MiningResult> results = miner.extractFrequent(datasetPath, params);

                assertNotNull(results, "Results should not be null");
                assertFalse(results.isEmpty(), "Results should not be empty");

                Set<Set<Integer>> expectedItemsets = new HashSet<>();
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(2)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(3)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(4)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(5)));
                expectedItemsets.add(new TreeSet<>(Arrays.asList(1, 3, 4)));

                assertContainsItemsets(results, expectedItemsets);
            });
        }
    }
}