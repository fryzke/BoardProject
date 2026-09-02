import DOMPurify from 'dompurify';

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

export const normalizeContentForEditor = (raw) => {
    if (!raw) return '<p></p>';

    const sanitized = DOMPurify.sanitize(raw);
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
    const sanitized = DOMPurify.sanitize(content);
    const hasHtmlTag = /<[a-z][\s\S]*>/i.test(sanitized);
    if (!hasHtmlTag) {
        return sanitized
            .split('\n')
            .map(line => `<p>${line || '<br />'}</p>`)
            .join('');
    }
    return sanitized.replace(/<p>\s*<\/p>/gi, '<p><br /></p>');
};
