package com.github.cgks;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class MiningResult {
    @JsonProperty("pattern")
    private List<Integer> pattern; // une liste de motifs, ex: [1, 4]
    @JsonProperty("freq")
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
