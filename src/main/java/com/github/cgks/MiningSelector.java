package com.github.cgks;

import java.util.HashMap;

import com.github.cgks.choco.ChocoMiner;
import com.github.cgks.spmf.SpmfMiner;

import org.python.core.*;
import org.python.util.PythonInterpreter;


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
    public class PyModelPredictor {
        private PythonInterpreter interpreter;
        
        public PyModelPredictor() {
            // Initialize Python interpreter
            interpreter = new PythonInterpreter();
            interpreter.exec("import pickle\n" +
                             "import pandas as pd\n" +
                             "with open('your_model.pkl', 'rb') as f:\n" +
                             "    model = pickle.load(f)\n");
        }
        
        public Object predict(HashMap<String, Object> features) {
            // Convert HashMap to Python dict
            PyDictionary pyDict = new PyDictionary();
            for (Map.Entry<String, Object> entry : features.entrySet()) {
                pyDict.__setitem__(new PyString(entry.getKey()), 
                                  Py.java2py(entry.getValue()));
            }
            
            // Create prediction code and execute
            interpreter.set("input_dict", pyDict);
            interpreter.exec("df = pd.DataFrame([input_dict])\n" +
                             "prediction = model.predict(df)");
            
            // Get result
            PyObject result = interpreter.get("prediction");
            return result.__tojava__(Object.class);
        }
    }
}

