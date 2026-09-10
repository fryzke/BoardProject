import React from 'react';
import { CheckCircle2, AlertCircle, AlertTriangle, Info, X } from 'lucide-react';
import { ToastType } from './ToastType';
import './Toast.css';

const ICONS = {
    [ToastType.SUCCESS]: CheckCircle2,
    [ToastType.ERROR]: AlertCircle,
    [ToastType.WARNING]: AlertTriangle,
    [ToastType.INFO]: Info,
};

export default function ToastItem({ toast, onRemove }) {
    const IconComponent = ICONS[toast.type] || Info;

    return (
        <div className={`ToastItem ToastItem--${toast.type}`} role="alert">
            <div className="ToastIconWrapper">
                <IconComponent size={20} className="ToastIcon" />
            </div>
            <div className="ToastMessage">
                {toast.message}
            </div>
            <button
                type="button"
                className="ToastCloseBtn"
                onClick={() => onRemove(toast.id)}
                aria-label="닫기"
            >
                <X size={16} />
            </button>
        </div>
    );
}
