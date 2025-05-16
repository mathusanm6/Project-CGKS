package com.github.cgks;

import java.util.List;

public class MiningResult {
    private List<Integer> pattern; // une liste de motifs, ex: [1, 4]
    private int freq; // fréquence du motif

    public MiningResult(List<Integer> pattern, int freq) {
        this.pattern = pattern;
        this.freq = freq;
    }

    public List<Integer> getPattern() {
        return pattern;
    }

    public void setPattern(List<Integer> pattern) {
        this.pattern = pattern;
    }

    public int getFreq() {
        return freq;
    }

    public void setFreq(int freq) {
        this.freq = freq;
    }
}
