import os
import pandas as pd

# === Étape 1 : Calcul des statistiques pour tous les fichiers .dat ===
data_folder = "data"
stats = {}

for filename in os.listdir(data_folder):
    if filename.endswith(".dat"):
        file_path = os.path.join(data_folder, filename)
        try:
            items = set()
            line_count = 0
            nb_items_in_lines = 0

            with open(file_path, 'r', encoding='utf-8', errors='ignore') as file:
                for line in file:
                    tokens = line.strip().split()
                    nb_items_in_lines += len(tokens)
                    items.update(tokens)
                    line_count += 1

            if line_count > 0:
                density = (nb_items_in_lines / line_count) / len(items)
            else:
                density = 0

            # Enregistrement des stats : Nb unique items, Nb transactions, Densité
            stats[filename] = [len(items), line_count, density]

        except Exception as e:
            print(f"Erreur de lecture du fichier {filename} : {e}")

# === Étape 2 : Création du DataFrame de résultats ===
colonnes = ['Query', 'File', 'Frequency', 'Nbitems', 'Nbtransactions', 'Density', 'Class']
res_df = pd.DataFrame(columns=colonnes)

# === Étape 3 : Lecture des fichiers choco.csv et spmf.csv ===
with open('choco.csv', 'r') as f1, open('spmf.csv', 'r') as f2:
    lignes1 = f1.readlines()
    lignes2 = f2.readlines()

# === Étape 4 : Construction des lignes du DataFrame final ===
for i, (l1, l2) in enumerate(zip(lignes1, lignes2), start=1):
    champs1 = l1.strip().split(',')
    champs2 = l2.strip().split(',')

    try:
        query = champs1[0]
        filename = champs1[1]
        frequency = float(champs1[3])  # Assurez-vous que c'est bien un entier
        duration1 = float(champs1[4])
        duration2 = float(champs2[4])
        itemset1 = int(champs1[2])
        itemset2 = int(champs2[2])

        nb_items, nb_trans, density = stats[filename]

        # Classe : 0 si choco est meilleur, 1 sinon
        if duration1 < 15000 and duration2 < 15000:
            result_class = 0 if duration1 < duration2 else 1
        else:
            result_class = 0 if itemset1 > itemset2 else 1

        # Ajout de la ligne
        res_df.loc[len(res_df)] = [query, filename, frequency, nb_items, nb_trans, density, result_class]

    except Exception as e:
        print(f"Ligne {i} ignorée à cause d'une erreur : {e}")

# === Étape 5 : Sauvegarde du DataFrame final ===
res_df.to_csv("classes.csv", index=False)
