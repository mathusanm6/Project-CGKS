export const engines = ["Auto", "Choco-mining", "SPMF"];

export const datasets = [
  { label: "anneal", path: "/data/anneal.dat" },
  { label: "chess", path: "/data/chess.dat" },
  { label: "connect", path: "/data/connect.dat" },
  { label: "contextPasquier99", path: "/data/contextPasquier99.dat" },
  { label: "eisen", path: "/data/eisen.dat" },
  { label: "heart-cleveland", path: "/data/heart-cleveland.dat" },
  { label: "iris", path: "/data/iris.dat" },
  { label: "mushroom", path: "/data/mushroom.dat" },
  { label: "pumsb", path: "/data/pumsb.dat" },
];

export const queryTypes = [
  { id: "frequent", label: "Motifs fréquents" },
  { id: "closed", label: "Motifs fermés" },
  { id: "maximal", label: "Motifs maximaux" },
  { id: "rare", label: "Motifs rares" },
  { id: "generators", label: "Motifs générateurs" },
  { id: "minimal", label: "Motifs minimaux" },
  { id: "size_between", label: "Fermés de taille X-Y" },
  { id: "presence", label: "Fermés avec items présents" },
  { id: "absence", label: "Fermés avec items absents" },
];

export const queryTooltips = {
  frequent:
    "Motifs Fréquents : Extraire tous les motifs dont le support est supérieur ou égal au seuil minimum (freq(P) ≥ α).",
  closed:
    "Motifs Fermés Fréquents : Extraire les motifs où freq(P) ≥ α et aucun sur-ensemble strict n'a exactement la même fréquence.",
  maximal:
    "Motifs Maximaux Fréquents : Extraire les motifs où freq(P) ≥ α et aucun de leurs sur-ensembles n'est fréquent.",
  rare: 
    "Motifs Rares : Extraire les motifs dont le support est strictement inférieur au seuil minimum (freq(P) < α).",
  generators:
    "Motifs Générateurs Fréquents : Extraire les motifs où freq(P) ≥ α et aucun de leurs sous-ensembles stricts n'a exactement la même fréquence.",
  minimal:
    "Motifs Minimaux Rares : Extraire les motifs où freq(P) < α et tous leurs sous-ensembles sont fréquents.",
  size_between:
    "Motifs Fermés de Taille Contrainte : Extraire les motifs fermés dont la taille est comprise entre min et max, avec freq(P) ≥ α.",
  presence:
    "Motifs Fermés avec Items Présents : Extraire les motifs fermés qui contiennent obligatoirement les items spécifiés, avec freq(P) ≥ α.",
  absence:
    "Motifs Fermés avec Items Absents : Extraire les motifs fermés qui ne contiennent aucun des items spécifiés, avec freq(P) ≥ α.",
};

// Helper function to get default params based on query type
export const getDefaultParamsForQuery = (queryType) => {
  const baseParams = {};

  switch (queryType) {
    case "size_between":
      return { ...baseParams, minSupport: 0.5, minSize: "", maxSize: "" };
    case "presence":
    case "absence":
      return { ...baseParams, minSupport: 0.5, items: "" };
    case "minimal":
    case "rare":
      return { ...baseParams, maxSupport: 0.5 };
    case "frequent":
    case "closed":
    case "maximal":
    case "generators":
      return { ...baseParams, minSupport: 0.5 };
    default:
      return baseParams;
  }
};

// Table column widths for consistency
export const TABLE_COLUMN_WIDTHS = {
  index: "5%",
  pattern: "55%",
  support: "25%",
  size: "15%",
};

// Common form elements
export const FORM_FIELD_IDS = {
  minSupport: "minSupport-input",
  maxSupport: "maxSupport-input",
  minSize: "minSize-input",
  maxSize: "maxSize-input",
  items: "items-input",
};
