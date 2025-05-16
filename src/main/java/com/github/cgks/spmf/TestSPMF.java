package com.github.cgks.spmf;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import ca.pfv.spmf.algorithms.frequentpatterns.lcm.Dataset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;
import io.gitlab.chaver.mining.patterns.io.DatReader;
import io.gitlab.chaver.mining.patterns.io.TransactionalDatabase;
// Import algorithms
import ca.pfv.spmf.algorithms.frequentpatterns.lcm.AlgoLCMFreq; // Freq 
import ca.pfv.spmf.algorithms.frequentpatterns.lcm.AlgoLCM; // Freq closed 
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPMax; // Freq Max 
import ca.pfv.spmf.algorithms.frequentpatterns.apriori.AlgoApriori;
import ca.pfv.spmf.algorithms.frequentpatterns.apriori_rare.AlgoAprioriRare; // Rare Min
//import ca.pfv.spmf.algorithms.frequentpatterns.rpgrowth.AlgoRPGrowth; // Rare 
import ca.pfv.spmf.algorithms.frequentpatterns.zart.*; // Min Gen
//import ca.pfv.spmf.algorithms.frequentpatterns.defme.AlgoDefMe; // Freq Gen
import ca.pfv.spmf.algorithms.frequentpatterns.pascal.AlgoPASCAL; // Freq and Gen
import ca.pfv.spmf.input.transaction_database_list_integers.TransactionDatabase;

import com.github.cgks.choco.DynamicPatternMinerChoco;
import com.github.cgks.spmf.rpgrowth.*;


public class TestSPMF {

	
	public static void printRunStats(Itemsets itemsets, int sizeDB, long beginS){
		
		printRuntimeExecution(beginS);
		itemsets.printItemsets(sizeDB);
		System.out.println("Nombre de patterns trouvés : " + itemsets.getItemsetsCount() );
	}

	public static Itemsets runFreq(Dataset dataset, double minsup) throws IOException{
		
		// Applying the algorithm
		AlgoLCMFreq algo = new AlgoLCMFreq();
		
		long beginS = System.currentTimeMillis();
		Itemsets itemsets = algo.runAlgorithm(minsup, dataset, null);
		
		// Print stats and itemsets found
		TestSPMF.printRunStats(itemsets, dataset.getTransactions().size(), beginS);
		return itemsets;
	}

	
	public static Itemsets runFreqClosed(Dataset dataset, double minsup) throws IOException{
		
		// Applying the algorithm
		AlgoLCM algo = new AlgoLCM();
		long beginS = System.currentTimeMillis();
		Itemsets itemsets = algo.runAlgorithm(minsup, dataset, null);
		
		// Print stats and itemsets found
		TestSPMF.printRunStats(itemsets, dataset.getTransactions().size(), beginS);
		return itemsets;
	}

	public static Itemsets runFreqMax(String input, double minsup) throws IOException{
		Dataset dataset = new Dataset(input);
		// Applying the algorithm
		
		//AlgoLCMMax algo = new AlgoLCMMax(); // Algo avec la version locale Non dispo
		AlgoFPMax algo = new AlgoFPMax();

		long beginS = System.currentTimeMillis();
		Itemsets itemsets = algo.runAlgorithm(input, null, minsup);
		
		// Print stats and itemsets found
		TestSPMF.printRunStats(itemsets, dataset.getTransactions().size(), beginS);
		return itemsets;
	}

	public static Itemsets runRare(String input, double maxsup) throws IOException{
		Dataset dataset = new Dataset(input);
		// Applying the algorithm
		
		AlgoRPGrowth algo = new AlgoRPGrowth(); // Introuvable
		
		long beginS = System.currentTimeMillis();
		Itemsets itemsets = algo.runAlgorithm(input, null, maxsup, 0);
		
		// Print stats and itemsets found
		TestSPMF.printRunStats(itemsets, dataset.getTransactions().size(), beginS);

		return itemsets;
	}

	public static Itemsets runRareMinimal(String input, double minsup) throws IOException{
		Dataset dataset = new Dataset(input);
		// Applying the algorithm
		
		//AlgoLCMMax algo = new AlgoLCMMax(); // Algo avec la version locale Non dispo
		AlgoAprioriRare algo = new AlgoAprioriRare();

		long beginS = System.currentTimeMillis();
		Itemsets itemsets = algo.runAlgorithm(minsup, input, null);
		
		// Print stats and itemsets found
		TestSPMF.printRunStats(itemsets, dataset.getTransactions().size(), beginS);
		return itemsets;
	}

	public static Itemsets runFreqGenerator(String input, double minsup) throws IOException{
		// Load a binary context
		TransactionDatabase context = new TransactionDatabase();
		context.loadFile(input);
		AlgoZart algo = new AlgoZart();
		
		long beginS = System.currentTimeMillis();
		
		TZTableClosed algoItems = algo.runAlgorithm(context, minsup);
		Itemsets itemsets = new Itemsets("Generator itemset");
		
		// System.out.println(algoItems.mapGenerators.size());
		// int level = 0;
		// List<Itemset> closedList = algoItems.getLevelForZart(level);
		// while(closedList.size()>0){
		// 	for(Itemset item: closedList){
		// 		List<Itemset> gen = getGeneratorFromMap(algoItems.mapGenerators, item);
		// 		addItemsFromLItems(gen, itemsets);
		// 	}
		// 	closedList = algoItems.getLevelForZart(level);
		// 	level++;
			
		// };

		TZTableClosed results = algoItems;
		System.out.println("======= List of closed itemsets and their generators ============");
		for(int i=0; i< results.levels.size(); i++){
			for(Itemset closed : results.levels.get(i)){
				List<Itemset> generators = results.mapGenerators.get(closed);
				// if there are some generators
				if(generators.size()!=0) { 
					for(Itemset generator : generators){
						itemsets.addItemset(generator, i);
					}
				}else {
					// otherwise the closed itemset is a generator
					itemsets.addItemset(closed, i);
				}
			}
		}

		// Print stats and itemsets found
		TestSPMF.printRunStats(itemsets, context.getTransactions().size(), beginS);
		return itemsets;
	}

	public static List<Itemset> getGeneratorFromMap(Map<Itemset, List<Itemset>> mapGenerators, Itemset i){
		return mapGenerators.get(i);
	}
	
	public static void addItemsFromLItems(List<Itemset> l, Itemsets itemsets) throws IOException{
		for(Itemset i: l){
			itemsets.addItemset(i, i.size());
		}
	}
		/**
	 * Filtre les itemsets par taille (bornes incluses)
	 * @param itemsets Itemsets à filtrer
	 * @param sizeDB Taille de la base de données 
	 * @param minSize Taille minimale (inclusive)
	 * @param maxSize Taille maximale (inclusive)
	 * @return Nouvel Itemsets respectant la contrainte de taille
	 */
	public static Itemsets filterBySize(Dataset dataset, double minsup, int sizeDB, int minSize, int maxSize) throws IOException {
		// Création du résultat avec un nom descriptif
		AlgoLCM algo = new AlgoLCM();
		long startTime = System.currentTimeMillis();
		Itemsets itemsets = algo.runAlgorithm(minsup, dataset, null);
		
		Itemsets result = new Itemsets( 
			String.format("Itemsets de taille %d à %d", minSize, maxSize)
		);
		
		// Validation des paramètres
		if (minSize < 0 || maxSize < minSize) {
			throw new IllegalArgumentException("Tailles invalides");
		}


		for (List<Itemset> level : itemsets.getLevels()) {
			for (Itemset itemset : level) {
				int size = itemset.size();
				if (size >= minSize && size <= maxSize) {
					result.addItemset(itemset, size);
				}
			}
		}

		TestSPMF.printRunStats(result, sizeDB, startTime);
		return result;
	}


	/**
 * Filtre les itemsets pour ne conserver que ceux contenant TOUS les items requis
 * @param itemsets Les itemsets à filtrer
 * @param sizeDB La taille de la base de données (pour l'affichage)
 * @param requiredItems Les items qui doivent tous être présents (peut être null ou vide)
 * @return Un nouvel objet Itemsets filtré
 */
public static Itemsets filterByInclusion(Dataset dataset, double minsup, int sizeDB, List<Integer> requiredItems) throws IOException {
		// Création du résultat avec un nom descriptif
		AlgoLCM algo = new AlgoLCM();
		long startTime = System.currentTimeMillis();
		Itemsets itemsets = algo.runAlgorithm(minsup, dataset, null);

		
		Itemsets result = new Itemsets("Itemsets contenant tous: " + requiredItems);

		// Cas spécial: si aucun item requis, on retourne tout
		if (requiredItems == null || requiredItems.isEmpty()) {
			return itemsets;
		}

		// Tri des items requis pour la recherche binaire
		List<Integer> sortedRequired = new ArrayList<>(requiredItems);
		Collections.sort(sortedRequired);
		

		// Parcours optimisé avec stream
		itemsets.getLevels().forEach(level -> 
			level.stream()
				.filter(itemset -> containsAllRequired(itemset, sortedRequired))
				.forEach(itemset -> result.addItemset(itemset, itemset.size()))
		);

		TestSPMF.printRunStats(result, sizeDB, startTime);
		return result;
	}

	/**
	 * Vérifie si un itemset contient tous les items requis (version optimisée List<Integer>)
	 */
	private static boolean containsAllRequired(Itemset itemset, List<Integer> requiredItems) {
		List<Integer> itemsetItems = Arrays.stream(itemset.getItems())
										.boxed()
										.collect(Collectors.toList());
		return itemsetItems.containsAll(requiredItems);
	}

    /**
	 * Filtre les itemsets pour exclure ceux contenant AU MOINS UN des items interdits
	 * @param itemsets Les itemsets à filtrer
	 * @param sizeDB Taille de la base de données
	 * @param excludedItems Liste d'items à exclure (peut être null ou vide)
	 * @return Nouvel Itemsets filtré
	 */
	public static Itemsets filterByExclusion(Dataset dataset, double minsup, int sizeDB, List<Integer> excludedItems) throws IOException {
		AlgoLCM algo = new AlgoLCM();
		long startTime = System.currentTimeMillis();
		Itemsets itemsets = algo.runAlgorithm(minsup, dataset, null);
		
		Itemsets result = new Itemsets("Itemsets sans: " + excludedItems);
		
		if (excludedItems == null || excludedItems.isEmpty()) {
			return itemsets;
		}

		List<Integer> sortedExcluded = new ArrayList<>(excludedItems);
		Collections.sort(sortedExcluded);
		

		itemsets.getLevels().forEach(level ->
			level.stream()
				.filter(itemset -> !containsAny(itemset.getItems(), sortedExcluded))
				.forEach(itemset -> result.addItemset(itemset, itemset.size()))
		);

		TestSPMF.printRunStats(result, sizeDB, startTime);
		return result;
	}

	/**
	 * Vérifie si un tableau contient au moins un item interdit (version optimisée List<Integer>)
	 */
	private static boolean containsAny(int[] items, List<Integer> excludedItems) {
		return Arrays.stream(items)
					.anyMatch(item -> Collections.binarySearch(excludedItems, item) >= 0);
	}


	public static void printRuntimeExecution(long beginS){
		long exec_time = System.currentTimeMillis()-beginS;
        Duration d = Duration.ofMillis(exec_time);
        int minutes = d.toMinutesPart();
        int seconds = d.toSecondsPart();

		if ((minutes>0) | (seconds >0)){
            System.out.println("Temps de résolution : " + minutes + " mins et " + seconds + " secondes.");
        }else{
            System.out.println("Temps de résolution : " + exec_time + " ms");
        }
	}


		/**
	 * Lit des entiers saisis par l'utilisateur et met à jour le tableau fourni
	 * @param scanner Scanner initialisé dans le main
	 * @param existingItems Tableau existant à mettre à jour (peut être vide)
	 * @return Nouveau tableau int[] avec les items saisis
	 */
	public static List<Integer> readUserItems(Scanner scanner, List<Integer> existingItems) {
		// Initialisation safe
		List<Integer> itemsList = existingItems != null ? 
								new ArrayList<>(existingItems) : 
								new ArrayList<>();

		// Affichage état actuel
		System.out.println("\nItems actuels à filtrer: " + itemsList);

		// Saisie utilisateur
		System.out.println("\n--------------------------------------------------");
		System.out.println("Saisissez les nouveaux items (espaces ou virgules comme séparateurs)");
		System.out.println("Tapez 'suppr X' pour retirer un item existant");
		System.out.println("--------------------------------------------------");
		System.out.print("> ");

		scanner.nextLine();
		String input = scanner.nextLine().trim();
		
		
		// Gestion commande spéciale
		if (input.startsWith("suppr ")) {
			try {
				int itemToRemove = Integer.parseInt(input.substring(6));
				itemsList.remove((Integer) itemToRemove); // Cast nécessaire pour éviter confusion avec index
				System.out.println("Item " + itemToRemove + " supprimé");
			} catch (Exception e) {
				System.err.println("Format invalide. Usage: 'suppr 5'");
			}
			return itemsList;
		}

		// Traitement normal
		if (!input.isEmpty()) {
			Arrays.stream(input.split("[\\s,]+")) // Accepte espaces ET virgules
				.filter(token -> !token.isEmpty())
				.forEach(token -> {
					try {
						itemsList.add(Integer.parseInt(token));
					} catch (NumberFormatException e) {
						System.err.println("[Avertissement] Entrée ignorée: '" + token + "'");
					}
				});
		}

		System.out.println("Nouvelle liste: " + itemsList);
		return itemsList;
	}

	public static Itemsets buildUsingAprioriIfEmpty(Itemsets inputItemsets, String database_name) throws IOException{
		if (inputItemsets != null && inputItemsets.getItemsetsCount() > 0) {
			return inputItemsets;
		}
		
		// Utilisation de l'algorithme Apriori intégré de SPMF
		AlgoApriori apriori = new AlgoApriori();
		return apriori.runAlgorithm(0, database_name, null);
	}


	public static void main(String [] arg) throws IOException, Exception{
		String input = fileToPath("/data/contextPasquier99.dat"); // filteToPath do the same thing as choco for path retrieval
		Dataset dataset = new Dataset(input);
		Scanner scanner = new Scanner(System.in).useLocale(Locale.US);
		double minSup = 0.0, maxSup = 0.0;
        int minSize = 0, maxSize = 0, sizeDB = dataset.getTransactions().size();
		
		Itemsets itemsets = new Itemsets("");
		List<Integer> requiredItems = new ArrayList<>(), excludeItems = new ArrayList<>();
		while(true){
			System.out.println("\n=== MENU PRINCIPAL ===");
            System.out.println("1. Exemples pré-définis");
            System.out.println("0. Quitter");
            
            int mainChoice = scanner.nextInt();
            
            if (mainChoice == 0) break;

			System.out.println("╔═════════════════════════════════╗");
			System.out.println("║ SELECTION D'EXEMPLE DE REQUETES ║");
			System.out.println("╠═════════════════════════════════╣");
			System.out.println("║ 1. Itemsets fréquents           ║");
			System.out.println("║ 2. Itemsets fermés              ║");
			System.out.println("║ 3. Itemsets maximaux            ║");
			System.out.println("║ 4. Itemsets rares               ║");
			System.out.println("║ 5. Générateurs 		            ║");
			System.out.println("║ 6. Par taille                   ║");
			System.out.println("║ 7. Inclusion d'items            ║");
			System.out.println("║ 8. Exclusion d'items            ║");
			System.out.println("║ 9. Itemsets minimaux            ║");
			System.out.println("╚═════════════════════════════════╝");
			System.out.print("Choix (1-9) → ");
			int choice = scanner.nextInt();
			
			// Applying the algorithm
			// Sélection de l'algorithme de base
			switch (choice) {
			    case 1:
			        System.out.print("Support minimum (0-1) → ");
			        minSup = scanner.nextDouble();
					itemsets = TestSPMF.runFreq(dataset, minSup);
			        break;
				case 2:
			        System.out.print("Support minimum (0-1) → ");
			        minSup = scanner.nextDouble();
					itemsets = TestSPMF.runFreqClosed(dataset, minSup);
			        break;
				case 3:
			        System.out.print("Support minimum (0-1) → ");
			        minSup = scanner.nextDouble();
					itemsets = TestSPMF.runFreqMax(input, minSup);
			        break;
				case 4:
			        System.out.print("Support max (0-1) → ");
			        maxSup = scanner.nextDouble();
					itemsets = TestSPMF.runRare(input, maxSup);
			        break;
				case 5:
			        System.out.print("Support minimum (0-1) → ");
			        minSup = scanner.nextDouble();
					itemsets = TestSPMF.runFreqGenerator(input, minSup);
			        break;

				case 6:
					System.out.print("Taille minimum du motif :");
					minSize = scanner.nextInt();
					System.out.print("Taille maximum du motif :");
					maxSize = scanner.nextInt();
					System.out.print("Unique itemsets :" + dataset.getUniqueItems());
					System.out.print("Max itemsets :" + dataset.getMaxItem());
					System.out.print("Tx itemsets :" + dataset.getTransactions());
					System.out.print("Min support for freq generator :"  );
					minSup = scanner.nextDouble();
			        itemsets = TestSPMF.filterBySize(dataset, minSup, sizeDB, minSize, maxSize);
			        break;
				case 7:
			        requiredItems = readUserItems(scanner, requiredItems);
					System.out.print("Min support for freq closed :"  );
					minSup = scanner.nextDouble();
			        itemsets = TestSPMF.filterByInclusion(dataset, minSup, sizeDB, requiredItems);
			        break;
				case 8:
					excludeItems = readUserItems(scanner, excludeItems);
					System.out.print("Min support for freq closed :"  );
					minSup = scanner.nextDouble();
			        itemsets = TestSPMF.filterByExclusion(dataset, minSup, sizeDB, excludeItems);
			        break;
				case 9:
					System.out.print("Support max (0-1) → ");
					minSup = scanner.nextDouble();
					itemsets = TestSPMF.runRareMinimal(input, minSup);
					break;
				case 10:
					itemsets.printItemsets(itemsets.getItemsetsCount());
					break;
			    default:
				System.out.print("Exemple non implemnter");
			}
			
			
		};
		scanner.close();

    }
	
	public static String fileToPath(String filename) throws UnsupportedEncodingException{
		URL url = TestSPMF.class.getResource(filename);
		return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
	}
}
