import { useState } from "react";

export const useFormValidation = () => {
  const [validation, setValidation] = useState({});

  const validateForm = (query, params) => {
    const errors = {};
    const currentMinSupport = params.minSupport;

    if (
      currentMinSupport === "" ||
      currentMinSupport === null ||
      typeof currentMinSupport === "undefined"
    ) {
      errors.minSupport = "Le support minimum est requis.";
    } else if (
      isNaN(currentMinSupport) ||
      currentMinSupport < 0 ||
      currentMinSupport > 1
    ) {
      errors.minSupport = "Le support doit être un nombre entre 0 et 1.";
    }

    if (query === "size_between") {
      const minSize = params.minSize;
      const maxSize = params.maxSize;
      if (
        minSize === "" ||
        minSize === null ||
        typeof minSize === "undefined"
      ) {
        errors.minSize = "La taille minimale est requise.";
      } else if (isNaN(minSize) || minSize < 1) {
        errors.minSize = "La taille minimale doit être au moins 1.";
      }
      if (
        maxSize === "" ||
        maxSize === null ||
        typeof maxSize === "undefined"
      ) {
        errors.maxSize = "La taille maximale est requise.";
      } else if (isNaN(maxSize) || maxSize < 1) {
        errors.maxSize = "La taille maximale doit être au moins 1.";
      } else if (
        !isNaN(minSize) &&
        minSize >= 1 &&
        !isNaN(maxSize) &&
        maxSize < minSize
      ) {
        errors.maxSize =
          "La taille maximale doit être supérieure ou égale à la taille minimale.";
      }
    }

    if (query === "presence" || query === "absence") {
      const items = params.items;
      if (!items || items.trim() === "") {
        errors.items = "Au moins un item est requis.";
      } else {
        // Check each item is a valid number
        const itemList = items.split(",").map((item) => item.trim());
        const hasInvalidItems = itemList.some((item) => !/^\d+$/.test(item));

        if (hasInvalidItems) {
          errors.items =
            "Format incorrect. Utilisez des nombres séparés par des virgules (ex: 1,3,5).";
        }
      }
    }

    setValidation(errors);
    return Object.keys(errors).length === 0;
  };

  const clearValidationError = (fieldName) => {
    if (validation[fieldName]) {
      setValidation((prev) => {
        const newValidation = { ...prev };
        delete newValidation[fieldName];
        return newValidation;
      });
    }
  };

  return {
    validation,
    validateForm,
    clearValidationError,
    setValidation,
  };
};
