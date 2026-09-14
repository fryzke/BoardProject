import React from 'react';
import { Paperclip, X, FileText } from 'lucide-react';
import { formatFileSize } from '../../utils';
import { FileMaximum } from '../../enum';
import './AttachedFileList.css';

export default function AttachedFileList({ files = [], onRemoveFile }) {
    if (!files || files.length === 0) {
        return null;
    }

    const totalSize = files.reduce((acc, file) => acc + (file.fileSize || file.size || 0), 0);

    return (
        <div className="AttachedFileListContainer">
            <div className="AttachedFileListHeader">
                <div className="AttachedFileListTitle">
                    <Paperclip size={16} />
                    <span>첨부파일</span>
                    <span className="AttachedFileCount">
                        ({files.length} / {FileMaximum.TOTAL_IMAGE_NUMBER}개)
                    </span>
                </div>
                <div className="AttachedFileTotalSize">
                    총 {formatFileSize(totalSize)} / {FileMaximum.TOTAL_MAX_SIZE / (1024 * 1024)}MB
                </div>
            </div>

            <div className="AttachedFileListItems">
                {files.map((file, idx) => {
                    const fileName = file.originalName || file.name || '첨부파일';
                    const fileSize = file.fileSize || file.size || 0;
                    const fileId = file.id || file.fileId || idx;

                    return (
                        <div key={fileId || idx} className="AttachedFileItem">
                            <div className="AttachedFileItemLeft">
                                <FileText size={18} className="AttachedFileItemIcon" />
                                <span className="AttachedFileItemName" title={fileName}>
                                    {fileName}
                                </span>
                                <span className="AttachedFileItemSize">
                                    ({formatFileSize(fileSize)})
                                </span>
                            </div>
                            {onRemoveFile && (
                                <button
                                    type="button"
                                    className="AttachedFileItemDeleteBtn"
                                    onClick={() => onRemoveFile(file, idx)}
                                    title="파일 삭제"
                                    aria-label="파일 삭제"
                                >
                                    <X size={16} />
                                </button>
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
}
