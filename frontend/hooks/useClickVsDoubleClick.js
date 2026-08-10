import { useEffect, useRef } from "react";

// Disambiguates a single click from a double click on the same element. Without
// this, a double click still fires the single-click callback twice (once per
// click) before the double-click's own callback runs — visible as a flash of
// whatever the single click does (e.g. selecting/highlighting a row) right
// before whatever the double click does (e.g. navigating away). Delaying the
// single-click callback and cancelling it if a second click or an explicit
// dblclick arrives first removes that flash entirely.
const DELAY = 200;

export function useClickVsDoubleClick(onSingleClick, onDoubleClick) {
  const timerRef = useRef(null);

  useEffect(() => () => clearTimeout(timerRef.current), []);

  const handleClick = (e) => {
    clearTimeout(timerRef.current);
    const { metaKey, ctrlKey, shiftKey } = e;
    timerRef.current = setTimeout(() => {
      timerRef.current = null;
      onSingleClick({ metaKey, ctrlKey, shiftKey });
    }, DELAY);
  };

  const handleDoubleClick = (...args) => {
    clearTimeout(timerRef.current);
    timerRef.current = null;
    onDoubleClick(...args);
  };

  return { onClick: handleClick, onDoubleClick: handleDoubleClick };
}
