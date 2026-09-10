import React, { createContext, useContext, useState, useCallback, useRef } from 'react';
import ConfirmModal from './ConfirmModal';

const ModalContext = createContext(null);

export function ModalProvider({ children }) {
    const [modalConfig, setModalConfig] = useState(null);
    const resolverRef = useRef(null);

    const closeModal = useCallback((result = false) => {
        setModalConfig(null);
        if (resolverRef.current) {
            resolverRef.current(result);
            resolverRef.current = null;
        }
    }, []);

    const confirm = useCallback(({
        title = '확인',
        message = '',
        confirmText = '확인',
        cancelText = '취소',
        isDestructive = false,
    }) => {
        return new Promise((resolve) => {
            resolverRef.current = resolve;
            setModalConfig({
                type: 'confirm',
                title,
                message,
                confirmText,
                cancelText,
                isDestructive,
            });
        });
    }, []);

    const alertModal = useCallback(({
        title = '알림',
        message = '',
        confirmText = '확인',
    }) => {
        return new Promise((resolve) => {
            resolverRef.current = resolve;
            setModalConfig({
                type: 'alert',
                title,
                message,
                confirmText,
                isDestructive: false,
            });
        });
    }, []);

    return (
        <ModalContext.Provider value={{ confirm, alertModal }}>
            {children}
            {modalConfig && (
                <ConfirmModal
                    config={modalConfig}
                    onConfirm={() => closeModal(true)}
                    onCancel={() => closeModal(false)}
                />
            )}
        </ModalContext.Provider>
    );
}

export function useModal() {
    const context = useContext(ModalContext);
    if (!context) {
        throw new Error('useModal must be used within a ModalProvider');
    }
    return context;
}
