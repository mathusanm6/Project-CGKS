import axios from "axios";
import { GLOBAL_REQUEST_TIMEOUT } from "../constants";

// Use environment variable for API base URL
const API_BASE_URL =
  process.env.REACT_APP_API_BASE_URL || "http://localhost:8080";

export const submitQuery = async (dataset, queryType, params) => {
  try {
    // Process params for sending to the API
    const apiParams = { ...params };

    // Simplified items processing - only trim if it's a string
    if (apiParams.items) {
      apiParams.items = String(apiParams.items).trim();
    }

    const response = await axios.post(
      `${API_BASE_URL}/mine`,
      {
        dataset,
        queryType,
        params: apiParams,
      },
      {
        timeout: GLOBAL_REQUEST_TIMEOUT,
      }
    );

    return {
      success: true,
      data: response.data || [],
      message:
        response.data && response.data.length > 0
          ? `Requête exécutée avec succès. ${response.data.length} motif(s) trouvé(s).`
          : "Requête exécutée avec succès. Aucun motif trouvé pour les critères spécifiés.",
    };
  } catch (error) {
    console.error("API Error:", error);

    let errorMessage =
      "Une erreur inconnue s'est produite lors de l'exécution de la requête.";

    if (
      error.code === "ECONNABORTED" ||
      (error.message && error.message.includes("timeout"))
    ) {
      errorMessage = `La requête a échoué: Le délai d'attente de ${
        GLOBAL_REQUEST_TIMEOUT / 1000
      } secondes a été dépassé. Le serveur n'a pas répondu à temps.`;
    } else if (error.response) {
      errorMessage = `Erreur du serveur: ${error.response.status} - ${
        error.response.data?.message ||
        error.response.statusText ||
        "Réponse non valide du serveur"
      }`;
    } else if (error.request) {
      errorMessage =
        "La requête a été envoyée, mais aucune réponse n'a été reçue du serveur. Vérifiez la connexion réseau et l'état du serveur.";
    } else {
      errorMessage = `Erreur de configuration de la requête: ${error.message}`;
    }

    return {
      success: false,
      data: [],
      message: errorMessage,
    };
  }
};
