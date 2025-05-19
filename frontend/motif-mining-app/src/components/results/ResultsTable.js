import React from "react";
import { FileTextIcon, LoaderIcon } from "../icons/Icons";
import ErrorBoundary from "../ui/ErrorBoundary";
import { TABLE_COLUMN_WIDTHS } from "../../constants";

const ResultsTable = ({ 
  results, 
  isLoading, 
  clearResultsAndAlert
}) => {
  return (
    <div className="card">
      <div className="results-header">
        <h2 className="card-title">
          <span className="icon-spacing">
            <FileTextIcon />
          </span>
          Résultats
          {!isLoading && results.length > 0 && (
            <span className="results-count">
              ({results.length} motif{results.length > 1 ? "s" : ""}{" "}
              trouvé
              {results.length > 1 ? "s" : ""})
            </span>
          )}
        </h2>
        {!isLoading && results.length > 0 && (
          <button
            onClick={clearResultsAndAlert}
            className="clear-button"
            aria-label="Effacer les résultats"
          >
            Effacer
          </button>
        )}
      </div>
      
      <ErrorBoundary>
        {isLoading ? (
          <div className="status-container">
            <LoaderIcon
              style={{ color: "#2563EB" }}
              className="status-icon"
            />
            <p className="status-text">Traitement en cours...</p>
          </div>
        ) : results.length > 0 ? (
          <div className="table-container">
            <table className="table">
              <thead className="table-head">
                <tr>
                  <th 
                    className="table-header-cell" 
                    style={{ width: TABLE_COLUMN_WIDTHS.index }}
                    scope="col"
                  >
                    #
                  </th>
                  <th 
                    className="table-header-cell" 
                    style={{ width: TABLE_COLUMN_WIDTHS.pattern }}
                    scope="col"
                  >
                    Motif
                  </th>
                  <th
                    className="table-header-cell"
                    style={{ 
                      width: TABLE_COLUMN_WIDTHS.support,
                      textAlign: "right"
                    }}
                    scope="col"
                  >
                    Support
                  </th>
                  <th
                    className="table-header-cell"
                    style={{ 
                      width: TABLE_COLUMN_WIDTHS.size,
                      textAlign: "right"
                    }}
                    scope="col"
                  >
                    Taille
                  </th>
                </tr>
              </thead>
              <tbody>
                {results.map((r, i) => (
                  <tr
                    key={i}
                    className={i % 2 !== 0 ? "alternate-row" : ""}
                  >
                    <td className="table-cell">{i + 1}</td>
                    <td
                      className="table-cell pattern-cell"
                    >
                      [{r.pattern.join(", ")}]
                    </td>
                    <td
                      className="table-cell"
                      style={{ textAlign: "right" }}
                    >
                      {r.freq.toFixed(3)}
                      <div className="progress-container">
                        <div
                          className="progress-bar"
                          style={{ width: `${r.freq * 100}%` }}
                          aria-valuemin="0"
                          aria-valuemax="1"
                          aria-valuenow={r.freq}
                          role="progressbar"
                        ></div>
                      </div>
                    </td>
                    <td
                      className="table-cell"
                      style={{ textAlign: "right" }}
                    >
                      {r.pattern.length}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="status-container">
            <FileTextIcon className="status-icon" />
            <p
              style={{
                fontSize: "16px",
                fontWeight: 500,
                marginBottom: "8px",
              }}
            >
              Aucun résultat
            </p>
            <p>Lancez une requête ou ajustez les paramètres.</p>
          </div>
        )}
      </ErrorBoundary>
    </div>
  );
};

export default ResultsTable;