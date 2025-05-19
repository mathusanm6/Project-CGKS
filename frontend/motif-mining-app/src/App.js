import React, { useState, useRef, useCallback } from "react";
import { datasets, getDefaultParamsForQuery, queryTypes } from "./constants";
import Alert from "./components/ui/Alert";
import DataForm from "./components/form/DataForm";
import ResultsTable from "./components/results/ResultsTable";
import { DatabaseIcon } from "./components/icons/Icons";
import { useFormValidation } from "./hooks/useFormValidation";
import { submitQuery } from "./services/ApiService";
import ErrorBoundary from "./components/ui/ErrorBoundary";
import { useContentHeight } from "./hooks/useContentHeight";

// Import global CSS
import "./index.css";

const App = () => {
  // State
  const [dataset, setDataset] = useState(datasets[0].path);
  const [query, setQuery] = useState(queryTypes[0].id);
  const [params, setParams] = useState(
    getDefaultParamsForQuery(queryTypes[0].id)
  );
  const [results, setResults] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [alertMessage, setAlertMessage] = useState(null);

  // Refs
  const appHeaderRef = useRef(null);

  // Custom hooks
  const { validation, validateForm, clearValidationError, setValidation } =
    useFormValidation();
  const contentHeight = useContentHeight(appHeaderRef);

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

    const response = await submitQuery(dataset, query, params);

    if (response.success) {
      setResults(response.data);
      setAlertMessage({
        type: response.data.length > 0 ? "success" : "info",
        message: response.message,
      });
    } else {
      setAlertMessage({
        type: "error",
        message: response.message,
      });
    }

    setIsLoading(false);
  };

  // Reset form to initial state
  const resetForm = useCallback(() => {
    setDataset(datasets[0].path);
    setQuery(queryTypes[0].id);
    setParams(getDefaultParamsForQuery(queryTypes[0].id));
    setValidation({});
    setAlertMessage(null);
  }, [setValidation]);

  // Clear results and success/info alerts
  const clearResultsAndAlert = useCallback(() => {
    setResults([]);
    if (
      alertMessage &&
      (alertMessage.type === "success" || alertMessage.type === "info")
    ) {
      setAlertMessage(null);
    }
  }, [alertMessage]);

  // Update params when query type changes
  const handleQueryChange = useCallback(
    (newQuery) => {
      setQuery(newQuery);
      setParams(getDefaultParamsForQuery(newQuery));
      setValidation({});
      setAlertMessage(null);
    },
    [setValidation]
  );

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
              onClose={() => setAlertMessage(null)}
            />
          )}
        </div>

        <div className="two-column-layout" style={{ height: contentHeight }}>
          <div className="left-column">
            <ErrorBoundary>
              <DataForm
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
              />
            </ErrorBoundary>
          </div>

          <div className="right-column">
            <ErrorBoundary>
              <ResultsTable
                results={results}
                isLoading={isLoading}
                clearResultsAndAlert={clearResultsAndAlert}
              />
            </ErrorBoundary>
          </div>
        </div>
      </div>
    </div>
  );
};

export default App;
