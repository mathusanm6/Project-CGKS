import axios from "axios";

// Use environment variable for API base URL
const API_BASE_URL =
  process.env.REACT_APP_API_BASE_URL || "http://localhost:8080";

export const submitTask = async (engine, dataset, queryType, params) => {
  try {
    // Process params for sending to the API
    const apiParams = { ...params };

    // Only trim if it's a string
    if (apiParams.items) {
      apiParams.items = String(apiParams.items).trim();
    }
    const response = await axios.post(`${API_BASE_URL}/api/tasks`, {
      engine,
      dataset,
      queryType,
      params: apiParams,
    });
    return {
      success: true,
      data: response.data,
      message: "Task submitted successfully.",
    };
  } catch (error) {
    console.error("API Error (submitTask):", error);
    let errorMessage = "Failed to submit task.";
    if (error.response) {
      errorMessage = `Error: ${error.response.status} - ${
        error.response.data?.message ||
        error.response.data ||
        error.response.statusText
      }`;
    } else if (error.request) {
      errorMessage = "No response from server. Check network or server status.";
    } else {
      errorMessage = `Request setup error: ${error.message}`;
    }
    return { success: false, data: null, message: errorMessage };
  }
};

export const getTaskStatus = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/api/tasks/status`);
    return { success: true, data: response.data };
  } catch (error) {
    console.error("API Error (getTaskStatus):", error);
    // Avoid flooding with errors if server is temporarily down during polling
    return {
      success: false,
      data: null,
      message: "Could not fetch task status.",
    };
  }
};

export const cancelTask = async () => {
  try {
    const response = await axios.post(`${API_BASE_URL}/api/tasks/cancel`);
    return { success: true, message: response.data };
  } catch (error) {
    console.error("API Error (cancelTask):", error);
    let errorMessage = "Failed to cancel task.";
    if (error.response) {
      errorMessage = `Error: ${error.response.status} - ${
        error.response.data || error.response.statusText
      }`;
    } else if (error.request) {
      errorMessage = "No response from server.";
    }
    return { success: false, message: errorMessage };
  }
};
