import { useState, useEffect } from "react";

export const useContentHeight = (headerRef) => {
  const [contentHeight, setContentHeight] = useState("auto");

  useEffect(() => {
    const calculateHeight = () => {
      if (headerRef.current) {
        const headerHeight = headerRef.current.offsetHeight;
        // Adjust for the container's top/bottom padding
        const availableHeight = window.innerHeight - headerHeight - 2 * 24;
        setContentHeight(`${Math.max(300, availableHeight)}px`);
      }
    };

    calculateHeight();
    window.addEventListener("resize", calculateHeight);

    return () => {
      window.removeEventListener("resize", calculateHeight);
    };
  }, [headerRef]);

  return contentHeight;
};
