import { useState, useEffect } from 'react';
import { fetchTasks } from '../api';

export function useTasks(query, status, page, pageSize) {
  const [tasks, setTasks] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    // AbortController cancels the in-flight request when deps change or on unmount,
    // so a slow older response can never overwrite a newer one.
    const controller = new AbortController();

    setLoading(true);
    setError(null);

    fetchTasks({ query, status, page, pageSize, signal: controller.signal })
      .then((data) => {
        setTasks(data.items);
        setTotal(data.total);
      })
      .catch((err) => {
        // An aborted request is not a failure - a newer request has taken over.
        if (controller.signal.aborted) return;
        setError(err.message);
      })
      .finally(() => {
        // Reset loading only if this request is still the current one; otherwise
        // it would clear the spinner while the newer request is still running.
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [query, status, page, pageSize]);

  return { tasks, total, loading, error };
}
