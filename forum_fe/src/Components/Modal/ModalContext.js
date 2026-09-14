import React, { createContext, useContext, useState, useCallback, useRef } from 'react';
import Modal from './Modal';

const ModalContext = createContext(null);

export function ModalProvider({ children }) {
    const [modals, setModals] = useState([]);
    const resolversRef = useRef(new Map());

    const closeModal = useCallback((id, result = false) => {
        setModals((prev) => prev.filter((modal) => modal.id !== id));
        const resolve = resolversRef.current.get(id);
        if (resolve) {
            resolve(result);
            resolversRef.current.delete(id);
        }
    }, []);

    /**
     * 범용 모달 열기 (Promise 기반)
     * @param {Object} options
     * @param {string} [options.title]
     * @param {React.ReactNode | ((props: { close: (res?: any) => void }) => React.ReactNode)} options.content
     * @param {React.ReactNode | ((props: { close: (res?: any) => void }) => React.ReactNode)} [options.footer]
     * @param {'sm' | 'md' | 'lg' | 'xl' | 'full'} [options.size='md']
     * @param {boolean} [options.isDestructive=false]
     * @param {boolean} [options.showClose=true]
     * @param {boolean} [options.closeOnOverlayClick=true]
     * @param {boolean} [options.closeOnEsc=true]
     * @returns {Promise<any>}
     */
    const openModal = useCallback((options) => {
        const id = `${Date.now()}-${Math.random().toString(36).substring(2, 11)}`;
        return new Promise((resolve) => {
            resolversRef.current.set(id, resolve);
            setModals((prev) => [
                ...prev,
                {
                    id,
                    size: 'md',
                    showClose: true,
                    closeOnOverlayClick: true,
                    closeOnEsc: true,
                    ...options,
                },
            ]);
        });
    }, []);

    /**
     * 확인(Confirm) 모달
     */
    const confirm = useCallback(({
        title = '확인',
        message = '',
        confirmText = '확인',
        cancelText = '취소',
        isDestructive = false,
        size = 'md',
    }) => {
        return openModal({
            type: 'confirm',
            title,
            message,
            confirmText,
            cancelText,
            isDestructive,
            size,
        });
    }, [openModal]);

    /**
     * 알림(Alert) 모달
     */
    const alertModal = useCallback(({
        title = '알림',
        message = '',
        confirmText = '확인',
        size = 'md',
    }) => {
        return openModal({
            type: 'alert',
            title,
            message,
            confirmText,
            isDestructive: false,
            size,
        });
    }, [openModal]);

    return (
        <ModalContext.Provider value={{ openModal, closeModal, confirm, alertModal }}>
            {children}
            {modals.map((modalItem, index) => {
                const {
                    id,
                    type,
                    title,
                    message,
                    content,
                    footer,
                    confirmText = '확인',
                    cancelText = '취소',
                    isDestructive = false,
                    showClose = true,
                    closeOnOverlayClick = true,
                    closeOnEsc = true,
                    size = 'md',
                    icon,
                    className = '',
                } = modalItem;

                const handleClose = (res = false) => closeModal(id, res);
                const handleConfirm = () => closeModal(id, true);

                const currentZIndex = 10001 + index * 10;

                return (
                    <Modal
                        key={id}
                        isOpen={true}
                        onClose={() => handleClose(false)}
                        size={size}
                        closeOnOverlayClick={closeOnOverlayClick}
                        closeOnEsc={closeOnEsc}
                        zIndex={currentZIndex}
                        className={className}
                    >
                        {(title || showClose) && (
                            <Modal.Header
                                title={title}
                                icon={icon}
                                isDestructive={isDestructive}
                                showClose={showClose}
                                onClose={() => handleClose(false)}
                            />
                        )}

                        <Modal.Body>
                            {typeof content === 'function'
                                ? content({ close: handleClose })
                                : content || (message ? <p className="ModalMessage">{message}</p> : null)}
                        </Modal.Body>

                        {(footer || type === 'confirm' || type === 'alert') && (
                            <Modal.Footer>
                                {typeof footer === 'function'
                                    ? footer({ close: handleClose })
                                    : footer || (
                                        <>
                                            {type === 'confirm' && (
                                                <button
                                                    type="button"
                                                    className="ModalBtn ModalBtn--cancel"
                                                    onClick={() => handleClose(false)}
                                                >
                                                    {cancelText}
                                                </button>
                                            )}
                                            <button
                                                type="button"
                                                className={`ModalBtn ModalBtn--confirm ${isDestructive ? 'ModalBtn--danger' : ''}`}
                                                onClick={handleConfirm}
                                                autoFocus
                                            >
                                                {confirmText}
                                            </button>
                                        </>
                                    )}
                            </Modal.Footer>
                        )}
                    </Modal>
                );
            })}
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

