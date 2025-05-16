package com.github.cgks.choco;

import io.gitlab.chaver.mining.patterns.io.DatReader;
import io.gitlab.chaver.mining.patterns.io.TransactionalDatabase;
import io.gitlab.chaver.mining.patterns.constraints.factory.ConstraintFactory;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.net.URL;
import java.time.Duration;

public class DynamicPatternMinerChoco {

    public interface MiningConstraint {
        void apply(Model model, TransactionalDatabase database, BoolVar[] x, IntVar freq);
    }

    private static class FrequentItemset implements MiningConstraint {
        private double minSupport;

        public FrequentItemset(double minSupport) {
            this.minSupport = minSupport;
        }

        public void apply(Model model, TransactionalDatabase database, BoolVar[] x, IntVar freq) {
            int minFreq = (int) Math.ceil(database.getNbTransactions() * this.minSupport);
            model.arithm(freq, ">=", minFreq).post(); 
            ConstraintFactory.coverSize(database, freq, x).post();
        }
    }

    private static class InfrequentItemset implements MiningConstraint {
        private double maxSupport;

        public InfrequentItemset(double maxSupport) {
            this.maxSupport = maxSupport;
        }

        public void apply(Model model, TransactionalDatabase database, BoolVar[] x, IntVar freq) {
            int minFreq = (int) Math.ceil(database.getNbTransactions() * this.maxSupport);
            model.arithm(freq, "<", minFreq).post(); 
            ConstraintFactory.coverSize(database, freq, x).post();
        }
    }

    private static class ClosedItemset implements MiningConstraint {
        private double minSupport;

        public ClosedItemset(double minSupport) {
            this.minSupport = minSupport;
        }

        @Override
        public void apply(Model model, TransactionalDatabase database, BoolVar[] x, IntVar freq) {
            int minFreq = (int) Math.ceil(database.getNbTransactions() * this.minSupport);
            model.arithm(freq, ">=", minFreq).post(); 
            ConstraintFactory.coverSize(database, freq, x).post();
            ConstraintFactory.coverClosure(database, x).post();
        }
    }

    private static class GeneratorItemset implements MiningConstraint {
        private double minSupport;

        public GeneratorItemset(double minSupport) {
            this.minSupport = minSupport;
        }

        @Override
        public void apply(Model model, TransactionalDatabase database, BoolVar[] x, IntVar freq) {
            int minFreq = (int) Math.ceil(database.getNbTransactions() * this.minSupport);
            model.arithm(freq, ">=", minFreq).post(); 
            ConstraintFactory.coverSize(database, freq, x).post();
            ConstraintFactory.generator(database, x).post();
        }
    }

    private static class MaximalItemset implements MiningConstraint {
        private  double minSupport;

        public MaximalItemset(double minSupport) {
            this.minSupport = minSupport;
        }

        @Override
        public void apply(Model model, TransactionalDatabase database, BoolVar[] x, IntVar freq) {
            int minFreq = (int) Math.ceil(database.getNbTransactions() * this.minSupport);
            model.arithm(freq, ">=", minFreq).post(); 
            ConstraintFactory.coverSize(database, freq, x).post();
            ConstraintFactory.infrequentSupers(database, minFreq, x).post(); 
        }
    }

    private static class MinimalItemset implements MiningConstraint {
        private double maxSupport;

        public MinimalItemset(double maxSupport) {
            this.maxSupport = maxSupport;
        }

        @Override
        public void apply(Model model, TransactionalDatabase database, BoolVar[] x, IntVar freq) {
            int maxFreq = (int) Math.ceil(database.getNbTransactions() * this.maxSupport);
            model.arithm(freq, "<", maxFreq).post(); 
            ConstraintFactory.coverSize(database, freq, x).post();
            ConstraintFactory.frequentSubs(database, maxFreq, x).post();
        }
    }

    private static class SizeConstraint implements MiningConstraint {
        private   int minSize;
        private   int maxSize;

        public SizeConstraint(int minSize, int maxSize) {
            this.minSize = minSize;
            this.maxSize = maxSize;
        }

        @Override
        public void apply(Model model, TransactionalDatabase database, BoolVar[] x, IntVar freq) {
            model.sum(x, ">=", minSize).post();
            model.sum(x, "<=", maxSize).post();
        }
    }

    private static class ItemInclusion implements MiningConstraint {
        private   Set<Integer> requiredItems;

        public ItemInclusion(Set<Integer> items) {
            this.requiredItems = items;
        }

        @Override
        public void apply(Model model, TransactionalDatabase database, BoolVar[] x, IntVar freq) {
            for (int item : requiredItems) {
                x[item-1].eq(1).post();
            }
        }
    }

    private static class ItemExclusion implements MiningConstraint {
        private   Set<Integer> forbiddenItems;

        public ItemExclusion(Set<Integer> items) {
            this.forbiddenItems = items;
        }

        @Override
        public void apply(Model model, TransactionalDatabase database, BoolVar[] x, IntVar freq) {
            for (int item : forbiddenItems) {
                x[item-1].eq(0).post();
            }
        }
    }

    public static void minePatterns(TransactionalDatabase database, 
                                   List<MiningConstraint> constraints,
                                   String description) {
        Model model = new Model("Pattern Mining: " + description);
        BoolVar[] x = model.boolVarArray("x", database.getNbItems());
        IntVar freq = model.intVar("freq", 1, database.getNbTransactions());

        for (MiningConstraint constraint : constraints) {
            constraint.apply(model, database, x, freq);
        }

        Solver solver = model.getSolver();
        
        // Stratégie de recherche : essayer d'abord d'activer les items (LBS)
        solver.setSearch(Search.inputOrderLBSearch(x));
        
        
        System.out.println("\n Exécution de la requête : " + description);
        System.out.println("----------------------------------------");
        
        // Exécution de l'algo
        long beginS = System.currentTimeMillis();
        int solutionCount = 0;
        while (solver.solve()) {
            solutionCount++;
            printPattern(database, x, freq);
        }
        long exec_time = System.currentTimeMillis()-beginS;

        // Print stats
        solver.showStatistics();
        solver.showSolutions();
        Duration d = Duration.ofMillis(exec_time);
        int minutes = d.toMinutesPart();
        int seconds = d.toSecondsPart();

        if ((minutes>0) | (seconds >0)){
            System.out.println("Temps de résolution : " + minutes + " mins et " + seconds + " secondes.");
        }else{
            System.out.println("Temps de résolution : " + exec_time + " ms");
        }
        System.out.println("----------------------------------------");
        System.out.println("Total motifs trouvés : " + (solutionCount - 1));        
        System.out.println("Statistiques : " + solver.getNodeCount() + " nœuds explorés\n");
    }

    
    private static void printPattern(TransactionalDatabase database, BoolVar[] x, IntVar freq) {
        int[] itemset = IntStream.range(0, x.length)
            .filter(i -> x[i].getValue() == 1)
            .map(i -> database.getItems()[i])
            .toArray();

            System.out.printf("Motif : %-30s Fréquence : %d/%d (%.2f%%)%n",
                Arrays.toString(itemset),
                freq.getValue(),
                database.getNbTransactions(),
                (freq.getValue() * 100.0 / database.getNbTransactions())
            );
    }

    public static void main(String[] args) throws Exception {
        URL url = DynamicPatternMinerChoco.class.getResource("/data/contextPasquier99.dat");
		String path = java.net.URLDecoder.decode(url.getPath(),"UTF-8");
        TransactionalDatabase database = new DatReader(path).read();

        Scanner scanner = new Scanner(System.in).useLocale(Locale.US);
        while (true) {
            System.out.println("\n=== MENU PRINCIPAL ===");
            System.out.println("1. Appliquer des contraintes dynamiques");
            System.out.println("2. Exemples pré-définis");
            System.out.println("0. Quitter");
            
            int mainChoice = scanner.nextInt();
            
            if (mainChoice == 0) break;
            
            if (mainChoice == 2) {
                // [Vos exemples pré-définus existants...]
                continue;
            }

            System.out.println("\n=== TYPES DE CONTRAINTES ===");
            System.out.println("1.  Itemsets fréquents");
            System.out.println("2.  Itemsets rares");
            System.out.println("3.  Itemsets fermés");
            System.out.println("4.  Itemsets maximaux"); 
            System.out.println("5.  Itemsets générateurs");
            System.out.println("6.  Contrainte de taille");
            System.out.println("7.  Inclusion d'items");
            System.out.println("8.  Exclusion d'items");
            System.out.println("9.  Itemsets minimaux rares");
            System.out.println("0.  Exécuter la requête");
            
            List<MiningConstraint> dynamicConstraints = new ArrayList<>();
            
            int choice, minSize, maxSize;
            double minSup, maxSup, closedSup;
            Set<Integer> excludes, includes;
            while ((choice = scanner.nextInt()) != 0) {
                switch (choice) {
                    case 1:
                        System.out.print("Support minimum (0-1): ");
                        minSup = scanner.nextDouble();
                        dynamicConstraints.add(new FrequentItemset(minSup));
                        break;
                        
                    case 2:
                        System.out.print("Support maximum (0-1): ");
                        maxSup = scanner.nextDouble();
                        dynamicConstraints.add(new InfrequentItemset(maxSup));
                        break;
                        
                    case 3:
                        System.out.print("Support minimum pour fermeture (0-1): ");
                        closedSup = scanner.nextDouble();
                        dynamicConstraints.add(new ClosedItemset(closedSup));
                        break;
                        
                    case 4:
                        System.out.print("Support pour maximalité (0-1): ");
                        maxSup = scanner.nextDouble();
                        dynamicConstraints.add(new MaximalItemset(maxSup));
                        break;
                        
                    case 5:
                        System.out.println("Support pour minimalité (0-1):");
                        minSup = scanner.nextDouble();
                        dynamicConstraints.add(new GeneratorItemset(minSup));
                        break;
                        
                    case 6:
                        System.out.print("Taille minimale: ");
                        minSize = scanner.nextInt();
                        System.out.print("Taille maximale: ");
                        maxSize = scanner.nextInt();
                        dynamicConstraints.add(new SizeConstraint(minSize, maxSize));
                        break;
                        
                    case 7:
                        System.out.println("Items à inclure (séparés par espace):");
                        scanner.nextLine();
                        includes = Arrays.stream(scanner.nextLine().split(" "))
                            .map(Integer::parseInt)
                            .collect(Collectors.toSet());
                        dynamicConstraints.add(new ItemInclusion(includes));
                        break;
                        
                    case 8:
                        System.out.println("Items à exclure (séparés par espace):");
                        scanner.nextLine(); 
                        excludes = Arrays.stream(scanner.nextLine().split(" "))
                            .map(Integer::parseInt)
                            .collect(Collectors.toSet());
                        dynamicConstraints.add(new ItemExclusion(excludes));
                        break;
                        
                    case 9:
                        System.out.print("Support maximum pour rareté (0-1): ");
                        maxSup = scanner.nextDouble();
                        dynamicConstraints.add(new MinimalItemset(maxSup));
                        break;
                }
            }
            
            if (!dynamicConstraints.isEmpty()) {
                System.out.println("Description de la requête:");
                String desc = "";
                minePatterns(database, dynamicConstraints, desc);
            }
        }
        scanner.close();
    }
}
