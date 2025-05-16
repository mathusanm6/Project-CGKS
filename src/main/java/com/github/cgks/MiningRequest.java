package com.github.cgks;

import java.util.Map;

public class MiningRequest {
    private String dataset; // ex: "contextPasquier99.dat"
    private String queryType; // ex: "frequent", "closed", etc.
    private Map<String, String> params; // ex: { "minSupp": "60" }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }
}