package com.github.cgks;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/mine") // L'URL de base de l'API : http://localhost:8080/mine
@CrossOrigin // Autorise les appels depuis ton frontend React
public class MiningController {

    @PostMapping
    public List<MiningResult> mine(@RequestBody MiningRequest request) {
        MiningEngine engine = new MiningEngine();
        try {
            return engine.runMining(request);
        } catch (Exception e) {
            System.err.println("Error during mining: " + e.getMessage());
            return List.of();
        }
    }
}
