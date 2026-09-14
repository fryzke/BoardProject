import React, { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { AlertTriangle, Info, X } from 'lucide-react';
import './Modal.css';

/**
 * 범용 Modal 컴포넌트 (Compound Component 패턴)
 * 
 * 선언적 사용 예시:
 * <Modal isOpen={isOpen} onClose={handleClose} size="md">
 *   <Modal.Header title="모달 제목" onClose={handleClose} />
 *   <Modal.Body>내용</Modal.Body>
 *   <Modal.Footer>
 *     <button onClick={handleClose}>닫기</button>
 *   </Modal.Footer>
 * </Modal>
 */
export default function Modal({
    isOpen = true,
    onClose,
    size = 'md',
    closeOnOverlayClick = true,
    closeOnEsc = true,
    zIndex,
    className = '',
    children,
    config,
    onConfirm,
    onCancel,
}) {
    // Esc 키 핸들링
    useEffect(() => {
        if (!isOpen) return;

        const handleKeyDown = (e) => {
            if (e.key === 'Escape' && closeOnEsc) {
                if (onClose) onClose();
                else if (onCancel) onCancel();
            }
        };

        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [isOpen, closeOnEsc, onClose, onCancel]);

    // Body Scroll Lock
    useEffect(() => {
        if (!isOpen) return;

        const originalOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';

        return () => {
            document.body.style.overflow = originalOverflow;
        };
    }, [isOpen]);

    if (!isOpen) return null;

    const modalRoot = (typeof document !== 'undefined' && document.getElementById('modal-root')) || (typeof document !== 'undefined' ? document.body : null);
    if (!modalRoot) return null;

    const overlayStyle = zIndex ? { zIndex } : undefined;

    const handleOverlayClick = (e) => {
        if (e.target === e.currentTarget && closeOnOverlayClick) {
            if (onClose) onClose();
            else if (onCancel) onCancel();
        }
    };

    // 기존 confirm / alert 렌더링
    if (config && !children) {
        const {
            type = 'confirm',
            title,
            message,
            confirmText = '확인',
            cancelText = '취소',
            isDestructive = false,
        } = config;

        return createPortal(
            <div className="ModalOverlay" onClick={handleOverlayClick} style={overlayStyle} role="dialog" aria-modal="true">
                <div className={`ModalContainer ModalContainer--${size} ${className}`} onClick={(e) => e.stopPropagation()}>
                    <Modal.Header
                        title={title}
                        isDestructive={isDestructive}
                        onClose={onCancel || onClose}
                    />
                    <Modal.Body>
                        <p className="ModalMessage">{message}</p>
                    </Modal.Body>
                    <Modal.Footer>
                        {type === 'confirm' && (
                            <button
                                type="button"
                                className="ModalBtn ModalBtn--cancel"
                                onClick={onCancel || onClose}
                            >
                                {cancelText}
                            </button>
                        )}
                        <button
                            type="button"
                            className={`ModalBtn ModalBtn--confirm ${isDestructive ? 'ModalBtn--danger' : ''}`}
                            onClick={onConfirm || onClose}
                            autoFocus
                        >
                            {confirmText}
                        </button>
                    </Modal.Footer>
                </div>
            </div>,
            modalRoot
        );
    }

    return createPortal(
        <div className="ModalOverlay" onClick={handleOverlayClick} style={overlayStyle} role="dialog" aria-modal="true">
            <div className={`ModalContainer ModalContainer--${size} ${className}`} onClick={(e) => e.stopPropagation()}>
                {children}
            </div>
        </div>,
        modalRoot
    );
}

//컴포넌트

Modal.Header = function ModalHeader({
    title,
    icon,
    isDestructive = false,
    showClose = true,
    onClose,
    children,
    className = '',
}) {
    return (
        <div className={`ModalHeader ${className}`}>
            <div className="ModalHeaderTitle">
                {icon !== undefined ? (
                    icon
                ) : isDestructive ? (
                    <div className="ModalIconWrapper ModalIconWrapper--danger">
                        <AlertTriangle size={20} />
                    </div>
                ) : title ? (
                    <div className="ModalIconWrapper ModalIconWrapper--info">
                        <Info size={20} />
                    </div>
                ) : null}
                {title && <h3 className="ModalTitle">{title}</h3>}
                {children}
            </div>
            {showClose && onClose && (
                <button
                    type="button"
                    className="ModalCloseBtn"
                    onClick={onClose}
                    aria-label="닫기"
                >
                    <X size={18} />
                </button>
            )}
        </div>
    );
};

Modal.Body = function ModalBody({ children, className = '' }) {
    return <div className={`ModalBody ${className}`}>{children}</div>;
};

Modal.Footer = function ModalFooter({ children, className = '' }) {
    return <div className={`ModalFooter ${className}`}>{children}</div>;
};

Modal.CloseButton = function ModalCloseButton({ onClick, className = '' }) {
    return (
        <button
            type="button"
            className={`ModalCloseBtn ${className}`}
            onClick={onClick}
            aria-label="닫기"
        >
            <X size={18} />
        </button>
    );
};
