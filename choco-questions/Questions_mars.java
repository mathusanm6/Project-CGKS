
package io.gitlab.chaver.mining.examples;


import io.gitlab.chaver.mining.patterns.constraints.factory.ConstraintFactory;
import io.gitlab.chaver.mining.patterns.io.DatReader;
import io.gitlab.chaver.mining.patterns.io.TransactionalDatabase;
import io.gitlab.chaver.mining.patterns.io.Pattern;
import io.gitlab.chaver.mining.patterns.search.strategy.selectors.variables.MinCov;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;
import java.util.stream.IntStream;


public class Questions_mars{

    public static void printPattern(TransactionalDatabase database, BoolVar[] x, IntVar freq){
        int[] itemset = IntStream.range(0, x.length).filter(i -> x[i].getValue() == 1)
            .map(i -> database.getItems()[i])
            .toArray();

            System.out.println(Arrays.toString(itemset) +
                    ", freq=" + freq.getValue());
    }
    public static void main(String[] args) throws Exception {
        // Read the transactional database
        TransactionalDatabase database = new DatReader("data/contextPasquier99.dat").read();

        //Q1 : Énumérer les motifs ensemblistes fréquents + 60%
        Model model_1 = new Model("Maximal Itemset Mining");
        BoolVar[] x_1 = model_1.boolVarArray("x_1", database.getNbItems());
        int a_1 = (int) (database.getNbTransactions() * 0.6);
        IntVar freq_1 = model_1.intVar("freq", a_1, database.getNbTransactions());

        ConstraintFactory.coverSize(database, freq_1, x_1).post();

        Solver solver_1 = model_1.getSolver();
        System.out.println("Liste des motifs ensemblistes fréquents for the dataset contextPasquier99:");
        while (solver_1.solve()) {
            Questions_mars.printPattern(database, x_1, freq_1);
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
            Questions_mars.printPattern(database, x_2, freq_2);
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
            Questions_mars.printPattern(database, x_3, freq_3);
        }       

        //Q4 : Motifs rares
        Model model_4 = new Model("Rare Itemset Mining");
        BoolVar[] x_4 = model_4.boolVarArray("x_4", database.getNbItems());
        IntVar freq_4 = model_4.intVar("freq", 1, a_1-1); 

        ConstraintFactory.coverSize(database, freq_4, x_4).post();

        Solver solver_4 = model_4.getSolver();
        System.out.println("Liste des motifs rares for the dataset contextPasquier99:");
        while (solver_4.solve()) {
            Questions_mars.printPattern(database, x_4, freq_4);
        }

    }


}
