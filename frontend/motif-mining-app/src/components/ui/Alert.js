import React from "react";
import { AlertCircleIcon, CheckIcon, InfoIcon, XIcon } from "../icons/Icons";

const Alert = ({ type, message, onClose }) => {
  const icons = {
    error: <AlertCircleIcon />,
    warning: <AlertCircleIcon />,
    success: <CheckIcon />,
    info: <InfoIcon />,
  };

  const alertId = `alert-${type}-${message
    .substring(0, 10)
    .replace(/\s/g, "-")}`;

  return (
    <div
      className={`alert alert-${type}`}
      role="alert"
      aria-live="assertive"
      id={alertId}
    >
      <div className="alert-icon" aria-hidden="true">
        {icons[type]}
      </div>
      <div className="alert-message">{message}</div>
      {onClose && (
        <button
          onClick={onClose}
          className="alert-close"
          aria-label="Close alert"
        >
          <XIcon />
        </button>
      )}
    </div>
  );
};

export default Alert;
