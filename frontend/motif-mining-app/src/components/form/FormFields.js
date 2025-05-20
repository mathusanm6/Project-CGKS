import React from "react";
import Tooltip from "../ui/Tooltip";
import { FORM_FIELD_IDS } from "../../constants";
import TagInput from "./TagInput";

const FormFields = ({ query, params, validation, handleParamChange }) => {
  const commonMinSupportField = (
    <div className="field-group">
      <label
        htmlFor={FORM_FIELD_IDS.minSupport}
        className="label-style"
        id={`${FORM_FIELD_IDS.minSupport}-label`}
      >
        <Tooltip content="Proportion minimale de transactions contenant le motif (nombre entre 0 et 1, ex: 0.5)">
          Support minimum
        </Tooltip>
      </label>
      <input
        id={FORM_FIELD_IDS.minSupport}
        type="number"
        min="0"
        max="1"
        step="0.01"
        name="minSupport"
        placeholder="Ex: 0.5"
        value={params.minSupport ?? ""}
        onChange={handleParamChange}
        className={
          validation.minSupport ? "input-base input-error" : "input-base"
        }
        aria-invalid={validation.minSupport ? "true" : "false"}
        aria-describedby={
          validation.minSupport ? "minSupport-error" : undefined
        }
      />
      <p
        id="minSupport-error"
        className="error-text"
        role={validation.minSupport ? "alert" : undefined}
      >
        {validation.minSupport || ""}
      </p>
    </div>
  );

  const commonMaxSupportField = (
    <div className="field-group">
      <label
        htmlFor={FORM_FIELD_IDS.maxSupport}
        className="label-style"
        id={`${FORM_FIELD_IDS.maxSupport}-label`}
      >
        <Tooltip content="Proportion maximale de transactions contenant le motif (nombre entre 0 et 1, ex: 0.5)">
          Support maximum
        </Tooltip>
      </label>
      <input
        id={FORM_FIELD_IDS.maxSupport}
        type="number"
        min="0"
        max="1"
        step="0.01"
        name="maxSupport"
        placeholder="Ex: 0.5"
        value={params.maxSupport ?? ""}
        onChange={handleParamChange}
        className={
          validation.maxSupport ? "input-base input-error" : "input-base"
        }
        aria-invalid={validation.maxSupport ? "true" : "false"}
        aria-describedby={
          validation.maxSupport ? "maxSupport-error" : undefined
        }
      />
      <p
        id="maxSupport-error"
        className="error-text"
        role={validation.maxSupport ? "alert" : undefined}
      >
        {validation.maxSupport || ""}
      </p>
    </div>
  );

  switch (query) {
    case "frequent":
    case "closed":
    case "maximal":
    case "generators":
      return commonMinSupportField;

    case "rare":
    case "minimal":
      return commonMaxSupportField;

    case "size_between":
      return (
        <>
          {commonMinSupportField}
          <div style={{ display: "flex", gap: "16px", flexWrap: "wrap" }}>
            <div
              style={{
                flex: "1 1 calc(50% - 8px)",
                minWidth: "120px",
              }}
              className="field-group"
            >
              <label
                htmlFor={FORM_FIELD_IDS.minSize}
                className="label-style"
                id={`${FORM_FIELD_IDS.minSize}-label`}
              >
                <Tooltip content="Nombre minimal d'items dans le motif (entier >= 1)">
                  Taille minimale
                </Tooltip>
              </label>
              <input
                id={FORM_FIELD_IDS.minSize}
                type="number"
                min="1"
                step="1"
                name="minSize"
                placeholder="Ex: 2"
                value={params.minSize ?? ""}
                onChange={handleParamChange}
                className={
                  validation.minSize ? "input-base input-error" : "input-base"
                }
                aria-invalid={validation.minSize ? "true" : "false"}
                aria-describedby={
                  validation.minSize ? "minSize-error" : undefined
                }
              />
              <p
                id="minSize-error"
                className="error-text"
                role={validation.minSize ? "alert" : undefined}
              >
                {validation.minSize || ""}
              </p>
            </div>
            <div
              style={{
                flex: "1 1 calc(50% - 8px)",
                minWidth: "120px",
              }}
              className="field-group"
            >
              <label
                htmlFor={FORM_FIELD_IDS.maxSize}
                className="label-style"
                id={`${FORM_FIELD_IDS.maxSize}-label`}
              >
                <Tooltip content="Nombre maximal d'items dans le motif (entier >= taille min)">
                  Taille maximale
                </Tooltip>
              </label>
              <input
                id={FORM_FIELD_IDS.maxSize}
                type="number"
                min="1"
                step="1"
                name="maxSize"
                placeholder="Ex: 5"
                value={params.maxSize ?? ""}
                onChange={handleParamChange}
                className={
                  validation.maxSize ? "input-base input-error" : "input-base"
                }
                aria-invalid={validation.maxSize ? "true" : "false"}
                aria-describedby={
                  validation.maxSize ? "maxSize-error" : undefined
                }
              />
              <p
                id="maxSize-error"
                className="error-text"
                role={validation.maxSize ? "alert" : undefined}
              >
                {validation.maxSize || ""}
              </p>
            </div>
          </div>
        </>
      );

    case "presence":
    case "absence":
      return (
        <>
          {commonMinSupportField}
          <div className="field-group">
            <label
              htmlFor={FORM_FIELD_IDS.items}
              className="label-style"
              id={`${FORM_FIELD_IDS.items}-label`}
            >
              <Tooltip
                content={`Items (nombres séparés par des virgules) qui doivent être ${
                  query === "presence" ? "présents dans" : "absents de"
                } chaque motif.`}
              >
                Items {query === "presence" ? "Présents" : "Absents"}
              </Tooltip>
            </label>
            <TagInput
              id={FORM_FIELD_IDS.items}
              name="items"
              value={params.items || ""}
              onChange={handleParamChange}
              placeholder="Ex: Saisissez un nombre et appuyez sur Entrée"
              isInvalid={!!validation.items}
            />
            <p
              id="items-error"
              className="error-text"
              role={validation.items ? "alert" : undefined}
            >
              {validation.items || ""}
            </p>
          </div>
        </>
      );

    default:
      return <p>Configuration de requête non reconnue.</p>;
  }
};

export default FormFields;
