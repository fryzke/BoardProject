import DOMPurify from 'dompurify';
import axios from 'axios';

const SANITIZE_CONFIG = {
    ADD_ATTR: ['target', 'download', 'class', 'data-filename', 'data-filesize', 'data-type'],
    ADD_TAGS: ['span', 'div'],
};

export const formatDate = (dateValue) => {
    if (!dateValue) return "-";
    try {
        const date = new Date(dateValue);
        if (isNaN(date.getTime())) return String(dateValue);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}.${month}.${day}`;
    } catch {
        return String(dateValue);
    }
};

export const formatFileSize = (bytes) => {
    if (!bytes || isNaN(bytes)) return '';
    const numBytes = Number(bytes);
    if (numBytes < 1024) return `${numBytes} B`;
    if (numBytes < 1024 * 1024) return `${(numBytes / 1024).toFixed(1)} KB`;
    return `${(numBytes / (1024 * 1024)).toFixed(1)} MB`;
};

export const normalizeContentForEditor = (raw) => {
    if (!raw) return '<p></p>';

    const sanitized = DOMPurify.sanitize(raw, SANITIZE_CONFIG);
    const hasHtmlTag = /<[a-z][\s\S]*>/i.test(sanitized);
    if (!hasHtmlTag) {
        return sanitized
            .split('\n')
            .map((line) => `<p>${line || '<br>'}</p>`)
            .join('');
    }

    let formatted = sanitized;
    formatted = formatted.replace(/<br\s*\/?>/gi, '</p><p>');
    formatted = formatted.replace(/<p>\s*<\/p>/gi, '<p><br></p>');

    return formatted;
};

export const formatDetailContent = (content) => {
    if (!content) return "";
    const sanitized = DOMPurify.sanitize(content, SANITIZE_CONFIG);
    const hasHtmlTag = /<[a-z][\s\S]*>/i.test(sanitized);
    if (!hasHtmlTag) {
        return sanitized
            .split('\n')
            .map(line => `<p>${line || '<br />'}</p>`)
            .join('');
    }
    return sanitized.replace(/<p>\s*<\/p>/gi, '<p><br /></p>');
};

/**
 * 첨부파일 다운로드 실행 함수
 * Blob을 생성하여 브라우저에서 직접 파일 다운로드를 트리거합니다.
 */
export const downloadFile = async (fileUrl, suggestedFileName) => {
    try {
        const token = localStorage.getItem('accessToken');
        const headers = token ? { Authorization: `Bearer ${token}` } : {};

        const response = await axios.get(fileUrl, {
            responseType: 'blob',
            headers,
        });

        // Content-Disposition 헤더에서 파일명 추출 시도
        let fileName = suggestedFileName;
        const disposition = response.headers['content-disposition'];
        if (disposition && disposition.includes('filename*=')) {
            const filenameRegex = /filename\*=UTF-8''([^;]+)/i;
            const matches = filenameRegex.exec(disposition);
            if (matches && matches[1]) {
                fileName = decodeURIComponent(matches[1]);
            }
        } else if (disposition && disposition.includes('filename=')) {
            const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
            const matches = filenameRegex.exec(disposition);
            if (matches && matches[1]) {
                fileName = matches[1].replace(/['"]/g, '');
            }
        }

        if (!fileName) {
            // URL에서 파일명 추출
            const urlParts = fileUrl.split('/');
            fileName = urlParts[urlParts.length - 1] || 'download_file';
        }

        const blob = new Blob([response.data]);
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(downloadUrl);
    } catch (error) {
        console.error('File download error:', error);
        // Blob 다운로드 실패 시 fallback으로 URL 직접 오픈
        const link = document.createElement('a');
        link.href = fileUrl;
        link.download = suggestedFileName || '';
        link.target = '_blank';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
};
