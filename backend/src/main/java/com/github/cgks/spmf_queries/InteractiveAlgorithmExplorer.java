package com.github.cgks.spmf;

import java.util.Arrays;
import java.util.Scanner;


import ca.pfv.spmf.algorithmmanager.AlgorithmManager;
import ca.pfv.spmf.algorithmmanager.DescriptionOfAlgorithm;

public class InteractiveAlgorithmExplorer {

    public static void main(String[] args) throws Exception {
        AlgorithmManager algoManager = AlgorithmManager.getInstance();
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║  EXPLORATEUR D'ALGORITHMES SPMF  ║");
        System.out.println("╚══════════════════════════════════╝");
        
        // Afficher la liste complète des algorithmes
        System.out.println("\nLISTE DES ALGORITHMES DISPONIBLES :");
        for (String algoName : algoManager.getListOfAlgorithmsAsString()) {
            System.out.println("  • " + algoName);
        }
        
        // Boucle interactive principale
        while (true) {
            System.out.println("\n════════════════════════════════════");
            System.out.print("Entrez un nom d'algorithme (ou 'exit' pour quitter)\n> ");
            
            String input = scanner.nextLine().trim();
            
            // Condition de sortie
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Au revoir !");
                break;
            }
            
            try {
                // Récupération de la description
                DescriptionOfAlgorithm descriptionOfAlgorithm = algoManager.getDescriptionOfAlgorithm(input);
                
                // Affichage formaté des informations
                System.out.println("\n Description de l'algorithme : " + descriptionOfAlgorithm.getName());
                System.out.println("════════════════════════════════════");
                System.out.println("Category : " + descriptionOfAlgorithm.getAlgorithmCategory());
                System.out.println("Types of input file : " + Arrays.toString(descriptionOfAlgorithm.getInputFileTypes()));
                System.out.println("Types of output file : " + Arrays.toString(descriptionOfAlgorithm.getOutputFileTypes()));
                System.out.println("Types of parameters : " + Arrays.toString(descriptionOfAlgorithm.getParametersDescription()));
                System.out.println("Implementation author : " + descriptionOfAlgorithm.getImplementationAuthorNames());
                System.out.println("URL:  : " + descriptionOfAlgorithm.getURLOfDocumentation());
                
            } catch (Exception e) {
                System.err.println("\n Erreur : Algorithme '" + input + "' introuvable.");
                System.out.println("Conseils :");
                System.out.println("- Vérifiez l'orthographe (attention à la casse)");
                System.out.println("- Essayez une recherche partielle (ex: 'FPG' pour 'FPGrowth')");
            }
        }
        
        scanner.close();
    }
}
