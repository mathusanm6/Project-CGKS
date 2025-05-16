package com.github.cgks;

import java.util.List;

public class MiningResult {
    private List<List<Integer>> pattern; // une liste de motifs, ex: [[1, 4], [7, 8]]
    private int freq; // fréquence du motif

    public MiningResult(List<List<Integer>> pattern, int freq) {
        this.pattern = pattern;
        this.freq = freq;
    }

    public List<List<Integer>> getPattern() {
        return pattern;
    }

    public void setPattern(List<List<Integer>> pattern) {
        this.pattern = pattern;
    }

    public int getFreq() {
        return freq;
    }

    public void setFreq(int freq) {
        this.freq = freq;
    }
}
