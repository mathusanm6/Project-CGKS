package com.github.cgks;

import java.util.HashMap;
import java.util.Map;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.io.IOException;

import com.github.cgks.choco.ChocoMiner;
import com.github.cgks.spmf.SpmfMiner;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


/**
 * Responsible for selecting the appropriate mining algorithm based on request parameters.
 * Uses a prediction API to automatically determine the most suitable mining engine.
 */
public class MiningSelector {
    // API endpoint for prediction service
    private static final String PREDICTION_API_URL = "http://localhost:5000/predict";
    
    // Mining engine identifiers
    private static final String ENGINE_AUTO = "auto";
    private static final String ENGINE_SPMF = "spmf";
    private static final String ENGINE_CHOCO = "choco-mining";
    
    // Prediction result values
    private static final int PREDICTION_SPMF = 1;
    
    // Query type mapping from frontend query name to backend query name
    private static final Map<String, String> QUERY_TYPE_MAP = new HashMap<>();
    
    // Initialize the query type mapping
    static {
        QUERY_TYPE_MAP.put("frequent", "Q1");    // Motifs fréquents
        QUERY_TYPE_MAP.put("closed", "Q2");      // Motifs fermés
        QUERY_TYPE_MAP.put("maximal", "Q3");     // Motifs maximaux
        QUERY_TYPE_MAP.put("rare", "Q4");        // Motifs rares
        QUERY_TYPE_MAP.put("generators", "Q5");  // Motifs générateurs
        QUERY_TYPE_MAP.put("minimal", "Q6");     // Motifs minimaux
        QUERY_TYPE_MAP.put("size_between", "Q7"); // Fermés de taille X-Y
        QUERY_TYPE_MAP.put("presence", "Q8");    // Fermés avec items présents
        QUERY_TYPE_MAP.put("absence", "Q9");     // Fermés avec items absents
    }
    
    /**
     * Determines the appropriate mining algorithm based on request parameters.
     * If engine is set to "auto", uses the prediction API to make the decision.
     *
     * @param request The mining request containing all parameters
     * @return The appropriate miner implementation
     * @throws Exception If selection fails or an unknown engine is specified
     */
    public static Miner chooseMiner(MiningRequest request) throws Exception {
        String engineType = request.getEngine();
        
        if (engineType == null || ENGINE_AUTO.equalsIgnoreCase(engineType)) {
            return chooseMinerAutomatically(request);
        } else if (ENGINE_SPMF.equalsIgnoreCase(engineType)) {
            return new SpmfMiner();
        } else if (ENGINE_CHOCO.equalsIgnoreCase(engineType)) {
            return new ChocoMiner();
        } else {
            throw new IllegalArgumentException("Unknown engine type: " + engineType);
        }
    }
    
    /**
     * Uses the prediction API to automatically select the most appropriate
     * mining algorithm for the given request parameters.
     *
     * @param request The mining request containing all parameters
     * @return The selected miner implementation
     * @throws Exception If prediction or selection fails
     */
    private static Miner chooseMinerAutomatically(MiningRequest request) throws Exception {
        try {
            // Extract the filename from the path
            String datasetPath = request.getDataset();
            String filename = extractFilenameFromPath(datasetPath);
            
            // Prepare features for prediction
            Map<String, Object> predictionFeatures = createPredictionFeatures(
                request.getQueryType(), 
                filename, 
                request.getParams().get("minSupport")
            );
            
            // Get prediction from the API
            Integer prediction = requestPrediction(predictionFeatures);
            
            // Select miner based on prediction
            return (prediction == PREDICTION_SPMF) ? new SpmfMiner() : new ChocoMiner();
            
        } catch (Exception e) {
            throw new Exception("Failed to automatically select mining engine", e);
        }
    }

    /**
     * Converts a frontend query ID to the corresponding backend query code (Q1-Q9).
     * 
     * @param queryTypeId the frontend ID (e.g., "frequent", "closed", etc.)
     * @return the corresponding backend query code (Q1-Q9)
     * @throws IllegalArgumentException if an unknown query type ID is provided
     */
    public static String mapToBackendQuery(String queryTypeId) {
        if (QUERY_TYPE_MAP.containsKey(queryTypeId)) {
            return QUERY_TYPE_MAP.get(queryTypeId);
        } else {
            throw new IllegalArgumentException("Unknown query type: " + queryTypeId);
        }
    }
    
    /**
     * Extracts the filename from a path string.
     *
     * @param path The full file path
     * @return The filename portion of the path
     */
    private static String extractFilenameFromPath(String path) {
        String[] pathTokens = path.split("/");
        return pathTokens[pathTokens.length - 1];
    }
    
    /**
     * Creates a map of features needed for the prediction API.
     *
     * @param queryType The query type from the frontend
     * @param filename The dataset filename
     * @param minSupport The minimum support threshold
     * @return A map containing the prediction features
     */
    private static Map<String, Object> createPredictionFeatures(
            String queryType, String filename, Object minSupport) {
        
        Map<String, Object> features = new HashMap<>();
        features.put("Query", mapToBackendQuery(queryType));
        features.put("File", filename);
        features.put("Frequency", minSupport);
        return features;
    }
    
    /**
     * Makes a request to the prediction API and returns the prediction result.
     *
     * @param features The feature map to send to the API
     * @return The prediction value
     * @throws Exception If the API call or response parsing fails
     */
    private static Integer requestPrediction(Map<String, Object> features) throws Exception {
        JsonElement predictionResult = getPrediction(features);
        return predictionResult.getAsInt();
    }
    
    /**
     * Sends a request to the prediction API and parses the response.
     *
     * @param features Map of features to send to the prediction API
     * @return The prediction result as a JsonElement
     * @throws IOException If HTTP connection fails
     * @throws Exception For any other errors
     */
    public static JsonElement getPrediction(Map<String, Object> features) throws IOException {
        // Convert features to JSON
        Gson gson = new Gson();
        String jsonInput = gson.toJson(features);
        
        HttpURLConnection connection = null;
        try {
            // Create and configure connection
            connection = createHttpConnection(PREDICTION_API_URL);
            
            // Send request body
            sendRequestBody(connection, jsonInput);
            
            // Check response status
            checkResponseStatus(connection);
            
            // Read response
            String response = readResponse(connection);
            
            // Parse JSON response
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            return jsonResponse.get("prediction").getAsJsonArray().get(0);
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Creates and configures an HTTP connection for the prediction API.
     *
     * @param apiUrl The URL of the prediction API
     * @return A configured HttpURLConnection
     * @throws IOException If connection creation fails
     */
    private static HttpURLConnection createHttpConnection(String apiUrl) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        return connection;
    }
    
    /**
     * Sends the JSON request body to the HTTP connection.
     *
     * @param connection The HTTP connection
     * @param jsonInput The JSON string to send
     * @throws IOException If writing to the connection fails
     */
    private static void sendRequestBody(HttpURLConnection connection, String jsonInput) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            byte[] inputBytes = jsonInput.getBytes("utf-8");
            os.write(inputBytes, 0, inputBytes.length);
        }
    }
    
    /**
     * Checks the HTTP response status and throws an exception if not 200-299.
     *
     * @param connection The HTTP connection
     * @throws IOException If the response status is not a success
     */
    private static void checkResponseStatus(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode > 299) {
            throw new IOException("Prediction API request failed. Status code: " + responseCode);
        }
    }
    
    /**
     * Reads the response body from the HTTP connection.
     *
     * @param connection The HTTP connection
     * @return The response body as a string
     * @throws IOException If reading the response fails
     */
    private static String readResponse(HttpURLConnection connection) throws IOException {
        try (Scanner scanner = new Scanner(connection.getInputStream(), "utf-8")) {
            return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        }
    }
}

