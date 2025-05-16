package com.github.cgks.choco;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.github.cgks.MiningResult;

import io.gitlab.chaver.mining.patterns.io.TransactionalDatabase;

public class ChocoMiner {
    // TODO(unknown): Implement this class to use Choco solver for mining patterns.
    // This class should contain all the methods needed.

    public static List<MiningResult> extractFrequent(TransactionalDatabase db, Map<String, String> params) {
        List<MiningResult> results = new ArrayList<>();
        return results;
    }

    public static List<MiningResult> extractClosed(TransactionalDatabase db, Map<String, String> params) {
        List<MiningResult> results = new ArrayList<>();
        return results;
    }
}
