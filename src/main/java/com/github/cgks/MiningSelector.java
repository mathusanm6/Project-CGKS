package com.github.cgks;

import java.util.HashMap;
import java.util.Map;

import com.github.cgks.choco.ChocoMiner;
import com.github.cgks.spmf.SpmfMiner;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class MiningSelector {
    private static final String predictionURL = "http://localhost:5000/predict";

    // Static map to hold the mapping from frontend ID to backend query code
    private static final Map<String, String> queryTypeMap = new HashMap<>();
    
    // Initialize the mapping
    static {
        queryTypeMap.put("frequent", "Q1");    // Motifs fréquents
        queryTypeMap.put("closed", "Q2");      // Motifs fermés
        queryTypeMap.put("maximal", "Q3");     // Motifs maximaux
        queryTypeMap.put("rare", "Q4");        // Motifs rares
        queryTypeMap.put("generators", "Q5");  // Motifs générateurs
        queryTypeMap.put("minimal", "Q6");     // Motifs minimaux
        queryTypeMap.put("size_between", "Q7"); // Fermés de taille X-Y
        queryTypeMap.put("presence", "Q8");    // Fermés avec items présents
        queryTypeMap.put("absence", "Q9");     // Fermés avec items absents
    }
    
    /**
     * Converts a frontend query ID to the corresponding backend query code (Q1-Q9)
     * 
     * @param queryTypeId the frontend ID (e.g., "frequent", "closed", etc.)
     * @return the corresponding backend query code (Q1-Q9)
     * @throws IllegalArgumentException if an unknown query type ID is provided
     */
    public static String mapToBackendQuery(String queryTypeId) {
        if (queryTypeMap.containsKey(queryTypeId)) {
            return queryTypeMap.get(queryTypeId);
        } else {
            throw new IllegalArgumentException("Unknown query type: " + queryTypeId);
        }
    }


    public static Miner chooseMiner(MiningRequest request) throws Exception{
        if (request.getEngine() == null || request.getEngine().toLowerCase().equals("auto")) {
            
            // Prep input
            HashMap<String, Object> features = new HashMap<>();
            features.put("Query", mapToBackendQuery(request.getQueryType()));
            String[] pathTokens = request.getDataset().split("/");
            features.put("File", pathTokens[pathTokens.length-1]);
            features.put("Frequency", request.getParams().get("minSupport"));
            
            
            try {
                // Get prediction
                Integer prediction = getPrediction(features).getAsInt();

                // Make choice
                if(prediction==1){
                    return new SpmfMiner();
                }else{
                    return new ChocoMiner();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("Failed to predict the mining engine.", e);
            }
        } else if (request.getEngine().toLowerCase().equals("spmf")) {
            return new SpmfMiner();
        } else if (request.getEngine().toLowerCase().equals("choco-mining")) {
            return new ChocoMiner();
        } else {
            throw new IllegalArgumentException("Unknown engine: " + request.getEngine().toLowerCase());
        }
    }
    
    
    public static JsonElement getPrediction(HashMap<String, Object> features) throws Exception {
        // Convert HashMap to JSON
        Gson gson = new Gson();
        String jsonInput = gson.toJson(features);
        
        // Create connection
        URL url = new URL(predictionURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInput.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        // Read response
        try (Scanner scanner = new Scanner(conn.getInputStream(), "utf-8")) {
            String response = scanner.useDelimiter("\\A").next();
        
            // Parse JSON response
            JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);

            return jsonResponse.get("prediction").getAsJsonArray().get(0);
        }
    }
}

