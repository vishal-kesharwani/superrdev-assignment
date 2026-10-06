import { useState, useEffect } from 'react';

// Returns `value` after it has been stable for `delayMs`.
// Debounce the value handed to useTasks, not the input state,
// so typing stays instant while network requests are batched.
export function useDebouncedValue(value, delayMs = 300) {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);

  return debounced;
}
