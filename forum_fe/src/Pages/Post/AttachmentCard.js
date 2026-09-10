import React, { useState } from 'react';
import { FileText, Download, Loader2 } from 'lucide-react';
import { downloadFile, formatFileSize } from '../../utils';
import { useToast } from '../../Components/Toast/ToastContext';
import './AttachmentCard.css';

export default function AttachmentCard({ href, fileName, fileSize }) {
    const toast = useToast();
    const [isDownloading, setIsDownloading] = useState(false);

    const cleanFileName = (fileName || '첨부파일').replace(/^📎\s*/, '').trim();

    const handleDownload = async (e) => {
        e.preventDefault();
        e.stopPropagation();

        if (isDownloading) return;

        setIsDownloading(true);
        try {
            await downloadFile(href, cleanFileName);
        } catch (err) {
            console.error('Download failed:', err);
            toast.error('파일 다운로드 중 오류가 발생했습니다.');
        } finally {
            setIsDownloading(false);
        }
    };

    return (
        <span 
            className="AttachmentCard" 
            onClick={handleDownload}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    handleDownload(e);
                }
            }}
            title={`${cleanFileName} 다운로드`}
        >
            <span className="AttachmentCardIconWrapper">
                <FileText className="AttachmentCardIcon" size={20} />
            </span>
            <span className="AttachmentCardInfo">
                <span className="AttachmentCardName" title={cleanFileName}>
                    {cleanFileName}
                </span>
                <span className="AttachmentCardMeta">
                    {fileSize ? formatFileSize(fileSize) : '클릭하여 다운로드'}
                </span>
            </span>
            <span className="AttachmentCardDownloadBtn">
                {isDownloading ? (
                    <Loader2 className="AttachmentCardSpinner" size={18} />
                ) : (
                    <Download size={18} />
                )}
            </span>
        </span>
    );
}
