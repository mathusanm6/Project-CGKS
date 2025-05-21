import React, { useState, useRef, useCallback, useEffect } from "react";
import {
  engines,
  datasets,
  getDefaultParamsForQuery,
  queryTypes,
} from "./constants";
import Alert from "./components/ui/Alert";
import DataForm from "./components/form/DataForm";
import ResultsTable from "./components/results/ResultsTable";
import { DatabaseIcon } from "./components/icons/Icons";
import { useFormValidation } from "./hooks/useFormValidation";
import {
  submitTask,
  getTaskStatus,
  cancelTask,
  acknowledgeTask,
} from "./services/ApiService";
import ErrorBoundary from "./components/ui/ErrorBoundary";
import { useContentHeight } from "./hooks/useContentHeight";

// Import global CSS
import "./index.css";

const App = () => {
  // State
  const [engine, setEngine] = useState(engines[0]);
  const [dataset, setDataset] = useState(datasets[0].path);
  const [query, setQuery] = useState(queryTypes[0].id);
  const [params, setParams] = useState(
    getDefaultParamsForQuery(queryTypes[0].id)
  );
  const [results, setResults] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [alertMessage, setAlertMessage] = useState(null);
  // Track if form has been modified since last submission
  const [isFormModified, setIsFormModified] = useState(false);
  const [currentTask, setCurrentTask] = useState(null);
  const [isPolling, setIsPolling] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [hasRun, setHasRun] = useState(false);

  // Refs
  const appHeaderRef = useRef(null);
  const alertTimeoutRef = useRef(null);

  // Custom hooks
  const { validation, validateForm, clearValidationError, setValidation } =
    useFormValidation();
  const contentHeight = useContentHeight(appHeaderRef);

  // Cleanup function for alert timeouts
  const clearAlertTimeout = () => {
    if (alertTimeoutRef.current) {
      clearTimeout(alertTimeoutRef.current);
      alertTimeoutRef.current = null;
    }
  };

  // Auto-dismiss success/info alerts after 5 seconds
  useEffect(() => {
    clearAlertTimeout();

    if (alertMessage && alertMessage.type === "success") {
      alertTimeoutRef.current = setTimeout(() => {
        setAlertMessage(null);
      }, 5000);
    }

    return clearAlertTimeout;
  }, [alertMessage]);

  // Clean up on unmount
  useEffect(() => {
    return () => clearAlertTimeout();
  }, []);

  // Fetch task status periodically
  useEffect(() => {
    let isMounted = true; // To prevent state updates on unmounted component
    let intervalId;

    const fetchStatus = async () => {
      // Skip polling if we're explicitly not polling
      if (!isPolling) return;

      const statusResponse = await getTaskStatus();
      if (!isMounted) return; // Check if component is still mounted

      if (statusResponse.success && statusResponse.data) {
        const newTaskStatus = statusResponse.data;

        // Batch state updates to reduce re-renders
        const updates = {};
        updates.currentTask = newTaskStatus;

        if (
          newTaskStatus.status === "PROCESSING" ||
          newTaskStatus.status === "PENDING"
        ) {
          updates.isLoading = true;
        } else {
          // COMPLETED, FAILED, CANCELLED, or other terminal states
          updates.isLoading = false;
          updates.isPolling = false;

          // Acknowledge terminal state to backend
          await acknowledgeTask();

          if (newTaskStatus.status === "COMPLETED") {
            updates.results = newTaskStatus.result || [];
            // Don't change the alertMessage if it was already set by submitTask
          }
          if (newTaskStatus.status === "FAILED") {
            updates.alertMessage = {
              type: "error",
              message: newTaskStatus.error || "Task failed.",
            };
          }
          // Handle CANCELLED if needed
          if (newTaskStatus.status === "CANCELLED") {
            updates.alertMessage = {
              type: "info",
              message: "Task was cancelled.",
            };
          }
        }

        // When backend reports terminal state, reset isCancelling
        if (
          newTaskStatus.status === "COMPLETED" ||
          newTaskStatus.status === "FAILED" ||
          newTaskStatus.status === "CANCELLED"
        ) {
          updates.isCancelling = false;
        }

        // Apply all updates at once
        setCurrentTask(updates.currentTask);
        if (updates.isLoading !== undefined) setIsLoading(updates.isLoading);
        if (updates.isPolling !== undefined) setIsPolling(updates.isPolling);
        if (updates.results) setResults(updates.results);
        if (updates.alertMessage) setAlertMessage(updates.alertMessage);
        if (updates.isCancelling !== undefined)
          setIsCancelling(updates.isCancelling);
      } else if (statusResponse.success && !statusResponse.data) {
        // No active task
        setCurrentTask(null);
        setIsLoading(false);
        setIsPolling(false);
      }
    };

    if (isPolling) {
      fetchStatus(); // Initial fetch
      intervalId = setInterval(fetchStatus, 2000); // Poll every 2 seconds
    }

    return () => {
      isMounted = false; // Set flag on unmount
      if (intervalId) clearInterval(intervalId);
    };
  }, [isPolling]);

  // Handle form field changes
  const handleParamChange = useCallback(
    (e) => {
      const { name, value, type } = e.target;
      let processedValue = value;
      if (type === "number") {
        processedValue = value === "" ? "" : parseFloat(value);
      }
      setParams((prev) => ({ ...prev, [name]: processedValue }));
      clearValidationError(name);
      setIsFormModified(true);
    },
    [clearValidationError]
  );

  // Handle form submission
  const handleSubmit = async () => {
    if (!validateForm(query, params)) {
      setAlertMessage({
        type: "error",
        message:
          "Veuillez corriger les erreurs dans le formulaire avant de soumettre.",
      });
      return;
    }

    setIsLoading(true);
    setAlertMessage(null);
    setResults([]);
    setHasRun(true);

    const response = await submitTask(engine, dataset, query, params);

    if (response.success && response.data) {
      setCurrentTask(response.data);
      // Only set alert if the task is immediately completed or failed
      if (response.data.status === "COMPLETED") {
        setAlertMessage({
          type: "success",
          message: response.message, // "Task submitted successfully."
        });
        setResults(response.data.result || []);
        setIsLoading(false); // Task completed immediately
        setIsPolling(false); // Stop polling
      } else if (response.data.status === "FAILED") {
        setAlertMessage({
          type: "error",
          message: response.data.error || "Task failed immediately.",
        });
        setIsLoading(false); // Task failed immediately
        setIsPolling(false); // Stop polling
      }
      setIsFormModified(false); // Mark form as "submitted"
      setIsPolling(true); // Start polling for updates
    } else {
      // Submission itself failed (e.g., network error, API conflict, validation error from backend)
      setAlertMessage({
        type: "error",
        message: response.message || "Failed to submit task. Please try again.",
      });
      setIsLoading(false); // Critical: Reset isLoading if the submission action itself failed
    }
  };

  // Reset form to initial state
  const resetForm = useCallback(() => {
    setEngine(engines[0]);
    setDataset(datasets[0].path);
    setQuery(queryTypes[0].id);
    setParams(getDefaultParamsForQuery(queryTypes[0].id));
    setValidation({});
    setAlertMessage(null);
    setIsFormModified(false);
  }, [setValidation]);

  // Clear results and success/info alerts
  const clearResultsAndAlert = useCallback(() => {
    setResults([]);
    setHasRun(false);
    // Only clear alert if it is a success (not info)
    if (alertMessage && alertMessage.type === "success") {
      setAlertMessage(null);
    }
  }, [alertMessage]);

  // Update params when query type changes
  const handleQueryChange = useCallback(
    (newQuery) => {
      setQuery(newQuery);
      setParams(getDefaultParamsForQuery(newQuery));
      setValidation({});
      setIsFormModified(true);
    },
    [setValidation]
  );

  const handleCancelTask = async () => {
    if (
      !currentTask ||
      (currentTask.status !== "PENDING" && currentTask.status !== "PROCESSING")
    ) {
      setAlertMessage({
        type: "info",
        message:
          "No active task to cancel or task is not in a cancellable state.",
      });
      return;
    }
    setIsCancelling(true);
    await cancelTask();
    // Wait for polling to update UI and alert
  };

  return (
    <div className="app-container">
      <div className="app">
        <header className="app-header" ref={appHeaderRef}>
          <h1 className="app-title">
            <span className="icon-spacing">
              <DatabaseIcon />
            </span>
            Fouille de Motifs
          </h1>
          <p className="app-subtitle">
            Exploration et analyse de motifs dans les jeux de données
          </p>
        </header>

        <div className="alert-container">
          {alertMessage && (
            <Alert
              type={alertMessage.type}
              message={alertMessage.message}
              onClose={() => {
                setAlertMessage(null);
                clearAlertTimeout();
              }}
            />
          )}
        </div>

        <div className="two-column-layout" style={{ height: contentHeight }}>
          <div className="left-column">
            <ErrorBoundary>
              <DataForm
                engine={engine}
                setEngine={setEngine}
                dataset={dataset}
                setDataset={setDataset}
                query={query}
                setQuery={handleQueryChange}
                params={params}
                isLoading={isLoading}
                validation={validation}
                handleParamChange={handleParamChange}
                handleSubmit={handleSubmit}
                resetForm={resetForm}
                isFormModified={isFormModified}
                currentTask={currentTask}
                handleCancelTask={handleCancelTask}
                isCancelling={isCancelling}
              />
            </ErrorBoundary>
          </div>

          <div className="right-column">
            <ErrorBoundary>
              <ResultsTable
                results={results}
                isLoading={isLoading}
                clearResultsAndAlert={clearResultsAndAlert}
                hasRun={hasRun}
              />
            </ErrorBoundary>
          </div>
        </div>
      </div>
    </div>
  );
};

export default App;
