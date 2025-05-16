
package com.github.cgks.choco;


import io.gitlab.chaver.mining.patterns.constraints.factory.ConstraintFactory;
import io.gitlab.chaver.mining.patterns.io.DatReader;
import io.gitlab.chaver.mining.patterns.io.TransactionalDatabase;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.net.URL;
import java.util.Arrays;
import java.util.stream.IntStream;


public class ChocoMiningQuestions{

    public static void printPattern(TransactionalDatabase database, BoolVar[] x, IntVar freq){
        int[] itemset = IntStream.range(0, x.length).filter(i -> x[i].getValue() == 1)
            .map(i -> database.getItems()[i])
            .toArray();

            System.out.println(Arrays.toString(itemset) +
                    ", freq=" + freq.getValue());
    }
    public static void main(String[] args) throws Exception {
        // Read the transactional database

        URL url = ChocoMiningQuestions.class.getResource("/data/contextPasquier99.dat");
		String path = java.net.URLDecoder.decode(url.getPath(),"UTF-8");
        TransactionalDatabase database = new DatReader(path).read();

        //Q1 : Énumérer les motifs ensemblistes fréquents + 60%
        Model model_1 = new Model("Maximal Itemset Mining");
        BoolVar[] x_1 = model_1.boolVarArray("x_1", database.getNbItems());
        int a_1 = (int) (database.getNbTransactions() * 0.6);
        IntVar freq_1 = model_1.intVar("freq", a_1, database.getNbTransactions());

        ConstraintFactory.coverSize(database, freq_1, x_1).post();

        Solver solver_1 = model_1.getSolver();
        System.out.println("Liste des motifs ensemblistes fréquents for the dataset contextPasquier99:");
        while (solver_1.solve()) {
            ChocoMiningQuestions.printPattern(database, x_1, freq_1);
        }
        //Q2 : Motifs fermés
        Model model_2 = new Model("Closed Itemset Mining");
        BoolVar[] x_2 = model_2.boolVarArray("x_2", database.getNbItems());
        IntVar freq_2 = model_2.intVar("freq", a_1, database.getNbTransactions());

        ConstraintFactory.coverSize(database, freq_2, x_2).post();
        ConstraintFactory.coverClosure(database, x_2).post();

        Solver solver_2 = model_2.getSolver();
        System.out.println("Liste des motifs fermés for the dataset contextPasquier99:");
        while (solver_2.solve()) {
            ChocoMiningQuestions.printPattern(database, x_2, freq_2);
        }

        //Q3 : Motifs maximaux
        Model model_3 = new Model("Maximal Itemset Mining");
        BoolVar[] x_3 = model_3.boolVarArray("x_3", database.getNbItems());
        IntVar freq_3 = model_3.intVar("freq", a_1, database.getNbTransactions());

        ConstraintFactory.coverSize(database, freq_3, x_3).post();
        ConstraintFactory.infrequentSupers(database,a_1, x_3).post(); 
        Solver solver_3 = model_3.getSolver();
        System.out.println("Liste des motifs maximaux for the dataset contextPasquier99:");
        while (solver_3.solve()) {
            ChocoMiningQuestions.printPattern(database, x_3, freq_3);
        }       

        //Q4 : Motifs rares
        Model model_4 = new Model("Rare Itemset Mining");
        BoolVar[] x_4 = model_4.boolVarArray("x_4", database.getNbItems());
        IntVar freq_4 = model_4.intVar("freq", 1, a_1-1); 

        ConstraintFactory.coverSize(database, freq_4, x_4).post();

        Solver solver_4 = model_4.getSolver();
        System.out.println("Liste des motifs rares for the dataset contextPasquier99:");
        while (solver_4.solve()) {
            ChocoMiningQuestions.printPattern(database, x_4, freq_4);
        }

        // Q5 : Motifs générateurs (sur-ensemble fréquences <= freq(motif generateur))
        Model model_5 = new Model("Minimal itemset Mining");
        BoolVar[] x_5 = model_5.boolVarArray("x_6", database.getNbItems());
        IntVar freq = model_5.intVar("freq", 1, database.getNbTransactions());
        //Contraintes
        ConstraintFactory.generator(database, x_5).post();
        ConstraintFactory.coverSize(database, freq, x_5).post();

        //résolution et affichage
        Solver solver_5 = model_5.getSolver();
        System.out.println("Liste des motifs générateurs for the dataset contextPasquier99:");
        while (solver_5.solve()) {
            ChocoMiningQuestions.printPattern(database, x_5, freq);
        }


        //Q6 : Motifs minimaux (rares et aucun sur-ensemble possible)
        Model model_6 = new Model("Minimal itemset Mining");
        BoolVar[] x_6 = model_6.boolVarArray("x_6", database.getNbItems());
        a_1 = (int) (database.getNbTransactions() * 0.4);
        //Contraintes
        IntVar freq_6 = model_6.intVar("freq", 1, a_1-1); // -1 pour exclure la bordure qui est inclus dans les motifs fréquents
        
        // Contrainte de fréquence faible (motifs rares)
        ConstraintFactory.coverSize(database, freq_6, x_6).post();

        // Contrainte de fermeture (motifs fermés)
        ConstraintFactory.coverClosure(database, x_6).post();


        //résolution et affichage
        Solver solver_6 = model_6.getSolver();
        System.out.println("Liste des motifs minimaux for the dataset contextPasquier99:");
        while (solver_6.solve()) {
            ChocoMiningQuestions.printPattern(database, x_6, freq_6);
        }


        //Q7 : Motifs fermés de taille entre X et Y 
        int x = 2;
        int y = 4;
        a_1 = (int) (database.getNbTransactions() * 1);
        Model model_7 = new Model("Closed Itemset Mining");
        BoolVar[] x_7 = model_7.boolVarArray("x_7", database.getNbItems());
        freq = model_7.intVar("freq", 1, database.getNbTransactions());

        // Limite le nombre d'items dans le motif
        model_7.sum(x_7, "<=", y).post();
        model_7.sum(x_7, ">=", x).post();
        // Contrainte de fermeture des motifs
        ConstraintFactory.coverClosure(database, x_7).post();
        ConstraintFactory.coverSize(database, freq, x_7).post();

        Solver solver_7 = model_7.getSolver();
        System.out.println("Liste des motifs fermés de taille entre x et y for the dataset contextPasquier99:");
        while (solver_7.solve()) {
            ChocoMiningQuestions.printPattern(database, x_7, freq);
        }

        int i = 2;
        //Q8 : Motifs fermés avec contrainte de présence des items {...}
        Model model_8 = new Model("Closed Itemset Mining");
        BoolVar[] x_8 = model_8.boolVarArray("x_8", database.getNbItems());
        freq = model_8.intVar("freq", 1, database.getNbTransactions());

        //Contrainte de présence
        x_8[i].eq(1).post();

        // Contrainte de fermeture 
        ConstraintFactory.coverClosure(database, x_8).post();
        ConstraintFactory.coverSize(database, freq, x_8).post();

        Solver solver_8 = model_8.getSolver();
        System.out.println("Liste des motifs fermés avec présence de "+ (i+1) +" for the dataset contextPasquier99:");
        while (solver_8.solve()) {
            ChocoMiningQuestions.printPattern(database, x_8, freq);
        }

        i = 2;
        //Q8 : Motifs fermés avec contrainte de présence des items {...}
        Model model_9 = new Model("Closed Itemset Mining");
        BoolVar[] x_9 = model_9.boolVarArray("x_9", database.getNbItems());
        freq = model_9.intVar("freq", 1, database.getNbTransactions());

        //Contrainte d'absence
        x_9[i].eq(0).post();
        // Contrainte de fermeture 
        ConstraintFactory.coverClosure(database, x_9).post();
        ConstraintFactory.coverSize(database, freq, x_9).post();

        Solver solver_9 = model_9.getSolver();
        System.out.println("Liste des motifs fermés avec absence de "+ (i+1) +" for the dataset contextPasquier99:");
        while (solver_9.solve()) {
            ChocoMiningQuestions.printPattern(database, x_9, freq);
        }
    }
}
