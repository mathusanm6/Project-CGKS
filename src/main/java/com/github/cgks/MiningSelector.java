package com.github.cgks;

import java.util.HashMap;

import com.github.cgks.choco.ChocoMiner;
import com.github.cgks.spmf.SpmfMiner;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Scanner;
import com.google.gson.Gson;


public class MiningSelector {
    public static Miner chooseMiner(MiningRequest request) {
        if (request.getEngine() == null || request.getEngine().toLowerCase().equals("auto")) {
            // Load classifier and metadata

            // Prep input
            HashMap<String, String> input = new HashMap<>();
            input.put("Query", request.getQueryType());
            input.put("File", request.getDataset());

            
            // Predict

            // Make choice
            

            return new SpmfMiner();
        } else if (request.getEngine().toLowerCase().equals("spmf")) {
            return new SpmfMiner();
        } else if (request.getEngine().toLowerCase().equals("choco-mining")) {
            return new ChocoMiner();
        } else {
            throw new IllegalArgumentException("Unknown engine: " + request.getEngine().toLowerCase());
        }
    }
    
    
    public static String getPrediction(HashMap<String, Object> features) throws Exception {
        // Convert HashMap to JSON
        Gson gson = new Gson();
        String jsonInput = gson.toJson(features);
        
        // Create connection
        URL url = new URL("http://localhost:5000/predict");
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
            return response;
        }
    }
    
    public static void main(String[] args) {
        try {
            // Example usage
            HashMap<String, Object> features = new HashMap<>();
            features.put("Query", "Q1");
            features.put("File", "contextPasquier99.dat");
            features.put("Frequency", 0.6);
            
            String prediction = getPrediction(features);
            System.out.println("Prediction: " + prediction);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

