import React from "react";
import { engines, datasets, queryTypes, queryTooltips } from "../../constants";
import { FilterIcon, LoaderIcon, ChevronDownIcon, XIcon } from "../icons/Icons";
import Tooltip from "../ui/Tooltip";
import FormFields from "./FormFields";
import ErrorBoundary from "../ui/ErrorBoundary";

const DataForm = ({
  engine,
  setEngine,
  dataset,
  setDataset,
  query,
  setQuery,
  params,
  isLoading,
  validation,
  handleParamChange,
  handleSubmit,
  resetForm,
  isFormModified,
}) => {
  const handleQueryChange = (e) => {
    setQuery(e.target.value);
  };

  return (
    <div className="left-card">
      <div className="form-scrollable-content">
        <h2 className="card-title">
          <span className="icon-spacing">
            <FilterIcon />
          </span>
          Paramètres de Requête
        </h2>

        <div className="select-container">
          <label htmlFor="engine-select" className="select-label">
            <Tooltip content="Sélectionnez le moteur pour l'analyse de motifs.">
              Moteur
            </Tooltip>
          </label>
          <select
            id="engine-select"
            value={engine}
            onChange={(e) => setEngine(e.target.value)}
            className="select"
            aria-label="Sélectionner un moteur"
          >
            {engines.map((e_val) => (
              <option key={e_val} value={e_val}>
                {e_val}
              </option>
            ))}
          </select>
          <div className="select-arrow">
            <ChevronDownIcon />
          </div>
        </div>

        <div className="select-container">
          <label htmlFor="dataset-select" className="select-label">
            <Tooltip content="Sélectionnez le jeu de données à analyser.">
              Jeu de données
            </Tooltip>
          </label>
          <select
            id="dataset-select"
            value={dataset}
            onChange={(e) => setDataset(e.target.value)}
            className="select"
            aria-label="Sélectionner un jeu de données"
          >
            {datasets.map((d) => (
              <option key={d.label} value={d.path}>
                {d.label}
              </option>
            ))}
          </select>
          <div className="select-arrow">
            <ChevronDownIcon />
          </div>
        </div>

        <div className="select-container">
          <label htmlFor="query-type-select" className="select-label">
            <Tooltip content="Choisissez le type d'analyse de motifs à effectuer.">
              Type de requête
            </Tooltip>
          </label>
          <select
            id="query-type-select"
            value={query}
            onChange={handleQueryChange}
            className="select"
            aria-label="Sélectionner un type de requête"
          >
            {queryTypes.map((q) => (
              <option key={q.id} value={q.id}>
                {q.label}
              </option>
            ))}
          </select>
          <div className="select-arrow">
            <ChevronDownIcon />
          </div>
        </div>

        <p
          className="query-description"
          id={`description-${query}`}
          aria-live="polite"
        >
          {queryTooltips[query]}
        </p>

        <ErrorBoundary>
          <FormFields
            query={query}
            params={params}
            validation={validation}
            handleParamChange={handleParamChange}
          />
        </ErrorBoundary>
      </div>

      <div className="button-group">
        <button
          onClick={handleSubmit}
          disabled={isLoading}
          className={`primary-button ${isLoading ? "disabled" : ""}`}
          aria-busy={isLoading}
        >
          {isLoading ? (
            <>
              <span className="loading-icon">
                <LoaderIcon />
              </span>
              Traitement...
            </>
          ) : (
            "Lancer la requête"
          )}
        </button>

        <button
          onClick={resetForm}
          disabled={isLoading}
          className={`secondary-button ${isLoading ? "disabled" : ""}`}
          aria-disabled={isLoading}
        >
          Réinitialiser
        </button>
      </div>
    </div>
  );
};

export default DataForm;
