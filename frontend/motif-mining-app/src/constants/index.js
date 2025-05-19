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
    "Extraire tous les motifs dont le support est supérieur au seuil minimum.",
  closed: "Extraire les motifs fermés (sans sur-ensemble de même support).",
  maximal: "Extraire les motifs maximaux (sans sur-ensemble fréquent).",
  rare: "Extraire les motifs rares (support faible mais informatif).",
  generators: "Extraire les motifs générateurs (sous-ensembles minimaux).",
  minimal: "Extraire les motifs minimaux selon le critère spécifié.",
  size_between:
    "Extraire les motifs fermés dont la taille est comprise entre min et max, et respectant le support minimum.",
  presence:
    "Extraire les motifs fermés contenant les items spécifiés et respectant le support minimum.",
  absence:
    "Extraire les motifs fermés ne contenant pas les items spécifiés et respectant le support minimum.",
};

export const GLOBAL_REQUEST_TIMEOUT = 60000; // 1 minute in milliseconds

// Helper function to get default params based on query type
export const getDefaultParamsForQuery = (queryType) => {
  const baseParams = { minSupport: 0.5 };

  switch (queryType) {
    case "size_between":
      return { ...baseParams, minSize: "", maxSize: "" };
    case "presence":
    case "absence":
      return { ...baseParams, items: "" };
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
  minSize: "minSize-input",
  maxSize: "maxSize-input",
  items: "items-input",
};
