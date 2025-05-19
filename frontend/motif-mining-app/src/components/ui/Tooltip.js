import React, { useState, useRef, useEffect } from "react";
import { InfoIcon } from "../icons/Icons";

const Tooltip = ({ children, content }) => {
  const [isVisible, setIsVisible] = useState(false);
  const tooltipRef = useRef(null);
  const tooltipId = `tooltip-${Math.random().toString(36).substring(2, 11)}`;
  
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (tooltipRef.current && !tooltipRef.current.contains(event.target)) {
        setIsVisible(false);
      }
    };
    
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      setIsVisible(!isVisible);
    } else if (e.key === 'Escape' && isVisible) {
      setIsVisible(false);
    }
  };

  return (
    <div className="tooltip-container" ref={tooltipRef}>
      <span
        className="tooltip-trigger"
        onMouseEnter={() => setIsVisible(true)}
        onMouseLeave={() => setIsVisible(false)}
        onClick={() => setIsVisible(!isVisible)}
        onKeyDown={handleKeyDown}
        role="button"
        tabIndex="0"
        aria-describedby={tooltipId}
        aria-expanded={isVisible}
      >
        {children}
        <span className="tooltip-icon">
          <InfoIcon />
        </span>
      </span>
      <div 
        id={tooltipId}
        className={`tooltip-content ${isVisible ? 'tooltip-visible' : ''}`}
        role="tooltip"
      >
        {content}
        <div className="tooltip-arrow"></div>
      </div>
    </div>
  );
};

export default Tooltip;