import React from 'react';
import ToastItem from './ToastItem';
import './Toast.css';

export default function ToastContainer({ toasts, onRemove }) {
    if (!toasts || toasts.length === 0) return null;

    return (
        <div className="ToastContainer" aria-live="polite">
            {toasts.map((toast) => (
                <ToastItem key={toast.id} toast={toast} onRemove={onRemove} />
            ))}
        </div>
    );
}
