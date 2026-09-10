import React, { useEffect } from 'react';
import { AlertTriangle, Info, X } from 'lucide-react';
import './Modal.css';

export default function ConfirmModal({ config, onConfirm, onCancel }) {
    const {
        type = 'confirm',
        title,
        message,
        confirmText = '확인',
        cancelText = '취소',
        isDestructive = false,
    } = config;

    useEffect(() => {
        const handleKeyDown = (e) => {
            if (e.key === 'Escape') {
                onCancel();
            } else if (e.key === 'Enter') {
                onConfirm();
            }
        };

        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [onConfirm, onCancel]);

    return (
        <div className="ModalOverlay" onClick={onCancel} role="dialog" aria-modal="true">
            <div className="ModalContainer" onClick={(e) => e.stopPropagation()}>
                <div className="ModalHeader">
                    <div className="ModalHeaderTitle">
                        {isDestructive ? (
                            <div className="ModalIconWrapper ModalIconWrapper--danger">
                                <AlertTriangle size={20} />
                            </div>
                        ) : (
                            <div className="ModalIconWrapper ModalIconWrapper--info">
                                <Info size={20} />
                            </div>
                        )}
                        <h3 className="ModalTitle">{title}</h3>
                    </div>
                    <button type="button" className="ModalCloseBtn" onClick={onCancel} aria-label="닫기">
                        <X size={18} />
                    </button>
                </div>

                <div className="ModalBody">
                    <p className="ModalMessage">{message}</p>
                </div>

                <div className="ModalFooter">
                    {type === 'confirm' && (
                        <button type="button" className="ModalBtn ModalBtn--cancel" onClick={onCancel}>
                            {cancelText}
                        </button>
                    )}
                    <button
                        type="button"
                        className={`ModalBtn ModalBtn--confirm ${isDestructive ? 'ModalBtn--danger' : ''}`}
                        onClick={onConfirm}
                        autoFocus
                    >
                        {confirmText}
                    </button>
                </div>
            </div>
        </div>
    );
}
