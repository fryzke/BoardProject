import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import ToastContainer from './ToastContainer';
import { ToastType } from './ToastType';

export { ToastType };

const ToastContext = createContext(null);

// Non-React 환경(예: api.js 인터셉터 등)에서도 사용 가능한 전역 발송 헬퍼
export const showToast = {
    success: (message, duration = 3000) => {
        window.dispatchEvent(new CustomEvent('app-toast', { detail: { message, type: ToastType.SUCCESS, duration } }));
    },
    error: (message, duration = 3500) => {
        window.dispatchEvent(new CustomEvent('app-toast', { detail: { message, type: ToastType.ERROR, duration } }));
    },
    warning: (message, duration = 3000) => {
        window.dispatchEvent(new CustomEvent('app-toast', { detail: { message, type: ToastType.WARNING, duration } }));
    },
    info: (message, duration = 3000) => {
        window.dispatchEvent(new CustomEvent('app-toast', { detail: { message, type: ToastType.INFO, duration } }));
    },
};

export function ToastProvider({ children }) {
    const [toasts, setToasts] = useState([]);

    const removeToast = useCallback((id) => {
        setToasts((prev) => prev.filter((toast) => toast.id !== id));
    }, []);

    const addToast = useCallback((message, type = ToastType.INFO, duration = 3000) => {
        const id = Date.now() + Math.random().toString(36).substring(2, 9);
        const newToast = { id, message, type, duration };

        setToasts((prev) => [...prev, newToast]);

        if (duration > 0) {
            setTimeout(() => {
                removeToast(id);
            }, duration);
        }
        return id;
    }, [removeToast]);

    // 전역 CustomEvent 리스너 등록
    useEffect(() => {
        const handleGlobalToast = (event) => {
            if (event.detail && event.detail.message) {
                addToast(event.detail.message, event.detail.type || ToastType.INFO, event.detail.duration || 3000);
            }
        };

        window.addEventListener('app-toast', handleGlobalToast);
        return () => window.removeEventListener('app-toast', handleGlobalToast);
    }, [addToast]);

    const toast = {
        success: (msg, dur) => addToast(msg, ToastType.SUCCESS, dur),
        error: (msg, dur) => addToast(msg, ToastType.ERROR, dur || 3500),
        warning: (msg, dur) => addToast(msg, ToastType.WARNING, dur),
        info: (msg, dur) => addToast(msg, ToastType.INFO, dur),
    };

    return (
        <ToastContext.Provider value={toast}>
            {children}
            <ToastContainer toasts={toasts} onRemove={removeToast} />
        </ToastContext.Provider>
    );
}

export function useToast() {
    const context = useContext(ToastContext);
    if (!context) {
        // Provider 외부에서 호출 시 전역 showToast 헬퍼로 fallback
        return showToast;
    }
    return context;
}
