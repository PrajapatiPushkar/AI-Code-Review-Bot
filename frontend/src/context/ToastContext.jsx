import React, { createContext, useState, useRef, useEffect, useCallback } from 'react';
import ToastContainer from '../components/common/ToastContainer';

export const ToastContext = createContext(null);

const DEFAULT_DURATIONS = {
  success: 4000,
  info: 4000,
  warning: 5000,
  error: 6000
};

export const ToastProvider = ({ children }) => {
  const [toasts, setToasts] = useState([]);
  const timersRef = useRef(new Map());

  // Dismiss a specific toast and clear its active timer
  const dismissToast = useCallback((id) => {
    if (timersRef.current.has(id)) {
      clearTimeout(timersRef.current.get(id));
      timersRef.current.delete(id);
    }
    setToasts((prev) => prev.filter((item) => item.id !== id));
  }, []);

  // Add a toast with automatic dismissal timer
  const addToast = useCallback(
    (message, options = {}) => {
      const type = options.type || 'info';
      const duration = options.duration ?? DEFAULT_DURATIONS[type] ?? 4000;
      const id = `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

      const newToast = {
        id,
        message,
        type,
        title: options.title || null,
        duration
      };

      setToasts((prev) => [...prev, newToast]);

      // Set auto-dismiss timer if duration > 0
      if (duration > 0) {
        const timer = setTimeout(() => {
          dismissToast(id);
        }, duration);
        timersRef.current.set(id, timer);
      }

      return id;
    },
    [dismissToast]
  );

  // Clear all pending timeouts when provider unmounts
  useEffect(() => {
    const currentTimers = timersRef.current;
    return () => {
      currentTimers.forEach((timer) => clearTimeout(timer));
      currentTimers.clear();
    };
  }, []);

  // Primary toast interface with ergonomic helpers
  const toast = useCallback(
    (message, options) => addToast(message, options),
    [addToast]
  );

  toast.success = useCallback(
    (message, options = {}) => addToast(message, { ...options, type: 'success' }),
    [addToast]
  );

  toast.error = useCallback(
    (message, options = {}) => addToast(message, { ...options, type: 'error' }),
    [addToast]
  );

  toast.warning = useCallback(
    (message, options = {}) => addToast(message, { ...options, type: 'warning' }),
    [addToast]
  );

  toast.info = useCallback(
    (message, options = {}) => addToast(message, { ...options, type: 'info' }),
    [addToast]
  );

  toast.dismiss = dismissToast;

  const value = {
    toast,
    toasts,
    addToast,
    dismissToast
  };

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />
    </ToastContext.Provider>
  );
};

export default ToastContext;
