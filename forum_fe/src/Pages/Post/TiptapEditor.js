import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Image from '@tiptap/extension-image';
import TextAlign from '@tiptap/extension-text-align';
import DOMPurify from 'dompurify';
import { useRef, useCallback, useEffect } from 'react';
import {
    Bold, Italic, Strikethrough, Heading1, Heading2, Heading3,
    List, ListOrdered, ImageIcon, AlignLeft, AlignCenter, AlignRight, AlignJustify, FileUp
} from 'lucide-react';
import { normalizeContentForEditor } from '../../utils';
import { uploadFile } from '../../api';
import { FileMaximum } from '../../enum';
import { useToast } from '../../Components/Toast/ToastContext';
import './TiptapEditor.css';

const MenuBar = ({ editor, onFileUploadClick, onImageUploadClick }) => {
    if (!editor) {
        return null;
    }

    return (
        <div className="menu-bar">
            <button
                type="button"
                onClick={() => editor.chain().focus().toggleBold().run()}
                className={editor.isActive('bold') ? 'is-active' : ''}
                title="굵게"
            >
                <Bold size={18} />
            </button>
            <button
                type="button"
                onClick={() => editor.chain().focus().toggleItalic().run()}
                className={editor.isActive('italic') ? 'is-active' : ''}
                title="기울임"
            >
                <Italic size={18} />
            </button>
            <button
                type="button"
                onClick={() => editor.chain().focus().toggleStrike().run()}
                className={editor.isActive('strike') ? 'is-active' : ''}
                title="취소선"
            >
                <Strikethrough size={18} />
            </button>
            <button
                type="button"
                onClick={() => editor.chain().focus().toggleHeading({ level: 1 }).run()}
                className={editor.isActive('heading', { level: 1 }) ? 'is-active' : ''}
                title="제목 1"
            >
                <Heading1 size={18} />
            </button>
            <button
                type="button"
                onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
                className={editor.isActive('heading', { level: 2 }) ? 'is-active' : ''}
                title="제목 2"
            >
                <Heading2 size={18} />
            </button>
            <button
                type="button"
                onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}
                className={editor.isActive('heading', { level: 3 }) ? 'is-active' : ''}
                title="제목 3"
            >
                <Heading3 size={18} />
            </button>
            <button
                type="button"
                onClick={() => editor.chain().focus().setTextAlign('left').run()}
                className={editor.isActive({ textAlign: 'left' }) ? 'is-active' : ''}
                title="왼쪽 정렬"
            >
                <AlignLeft size={18} />
            </button>
            <button
                type="button"
                onClick={() => editor.chain().focus().setTextAlign('center').run()}
                className={editor.isActive({ textAlign: 'center' }) ? 'is-active' : ''}
                title="중앙 정렬"
            >
                <AlignCenter size={18} />
            </button>
            <button
                type="button"
                onClick={() => editor.chain().focus().setTextAlign('right').run()}
                className={editor.isActive({ textAlign: 'right' }) ? 'is-active' : ''}
                title="오른쪽 정렬"
            >
                <AlignRight size={18} />
            </button>
            <button
                type="button"
                onClick={() => editor.chain().focus().setTextAlign('justify').run()}
                className={editor.isActive({ textAlign: 'justify' }) ? 'is-active' : ''}
                title="양쪽 정렬"
            >
                <AlignJustify size={18} />
            </button>
            <button
                type="button"
                onClick={() => editor.chain().focus().toggleBulletList().run()}
                className={editor.isActive('bulletList') ? 'is-active' : ''}
                title="글머리 기호"
            >
                <List size={18} />
            </button>
            <button
                type="button"
                onClick={() => editor.chain().focus().toggleOrderedList().run()}
                className={editor.isActive('orderedList') ? 'is-active' : ''}
                title="번호 매기기"
            >
                <ListOrdered size={18} />
            </button>
            <button
                type="button"
                onClick={onImageUploadClick}
                title="이미지 업로드"
            >
                <ImageIcon size={18} />
            </button>
            <button
                type="button"
                onClick={onFileUploadClick}
                title="첨부파일 업로드"
            >
                <FileUp size={18} />
            </button>
        </div>
    );
};

export default function TiptapEditor({
    content,
    onChange,
    postId,
    attachedFiles = [],
    setAttachedFiles,
    setFileIdList
}) {
    const toast = useToast();
    const fileInputRef = useRef(null);
    const imageInputRef = useRef(null);

    const editor = useEditor({
        extensions: [
            StarterKit,
            TextAlign.configure({
                types: ['heading', 'paragraph'],
                alignments: ['left', 'center', 'right', 'justify'],
                defaultAlignment: 'left',
            }),
            Image.configure({
                inline: true,
                allowBase64: true,
            }),
        ],
        content: normalizeContentForEditor(content),
        onCreate: ({ editor }) => {
            const html = DOMPurify.sanitize(editor.getHTML());
            const plainText = editor.getText().trim();
            onChange(html, plainText);
        },
        onUpdate: ({ editor }) => {
            const html = DOMPurify.sanitize(editor.getHTML());
            const plainText = editor.getText().trim();
            onChange(html, plainText);
        },
    });

    useEffect(() => {
        if (editor && content !== undefined) {
            const normalized = normalizeContentForEditor(content);
            if (editor.getHTML() !== normalized && editor.getHTML() !== content) {
                editor.commands.setContent(normalized);

            }
        }
    }, [content, editor]);

    // 첨부파일 및 이미지 공통 1차 유효성 검증
    const validateFile = useCallback((file, isImageOnly = false) => {
        const allowedExtensions = isImageOnly
            ? ['jpg', 'jpeg', 'png', 'gif', 'webp']
            : ['jpg', 'jpeg', 'png', 'gif', 'webp', 'pdf', 'doc', 'docx', 'txt', 'xlsx', 'pptx', 'zip'];
        const fileName = file.name || '';
        const fileExtension = fileName.includes('.') ? fileName.split('.').pop().toLowerCase() : '';

        if (!allowedExtensions.includes(fileExtension)) {
            toast.warning(`허용되지 않는 파일 형식입니다: .${fileExtension || 'unknown'}`);
            return false;
        }

        const currentCount = attachedFiles.length;
        if (currentCount >= FileMaximum.TOTAL_IMAGE_NUMBER) {
            toast.warning(`파일은 최대 ${FileMaximum.TOTAL_IMAGE_NUMBER}개까지 업로드 가능합니다.`);
            return false;
        }

        const fileSize = file.size;
        if (fileSize > FileMaximum.MAX_SIZE) {
            toast.warning(`파일은 최대 ${FileMaximum.MAX_SIZE / (1024 * 1024)}MB까지 업로드 가능합니다.`);
            return false;
        }

        const currentTotalSize = attachedFiles.reduce((acc, f) => acc + (f.fileSize || f.size || 0), 0);
        if (currentTotalSize + fileSize > FileMaximum.TOTAL_MAX_SIZE) {
            toast.warning(`파일은 총합 ${FileMaximum.TOTAL_MAX_SIZE / (1024 * 1024)}MB까지 업로드할 수 있습니다.`);
            return false;
        }

        return true;
    }, [attachedFiles, toast]);

    // 1) 이미지 업로드 (본문 인라인 삽입)
    const handleImageChange = useCallback(async (event) => {
        const file = event.target.files?.[0];
        if (!file) return;

        if (!validateFile(file, true)) {
            event.target.value = '';
            return;
        }

        try {
            const result = await uploadFile(file, postId);
            if (result.success && result.url) {
                editor?.chain().focus().setImage({ src: result.url }).run();
                if (setFileIdList && result.data?.id) {
                    setFileIdList(pre => [...pre, result.data.id]);
                }
                toast.success(`${file.name} 이미지가 삽입되었습니다.`);
            } else {
                toast.error(result.message || '이미지 업로드에 실패했습니다.');
            }
        } catch (error) {
            toast.error('이미지 업로드 중 오류가 발생했습니다.');
        } finally {
            event.target.value = '';
        }
    }, [editor, postId, setFileIdList, toast, validateFile]);

    // 2) 첨부파일 업로드 (에디터 밖 하단 독립 영역에 추가)
    const handleFileChange = useCallback(async (event) => {
        const file = event.target.files?.[0];
        if (!file) return;

        if (!validateFile(file, false)) {
            event.target.value = '';
            return;
        }

        try {
            const result = await uploadFile(file, postId);
            if (result.success && result.data) {
                const uploadedFile = {
                    id: result.data.id,
                    originalName: result.data.originalName || file.name,
                    fileSize: result.data.fileSize || file.size,
                    accessUrl: result.data.accessUrl || result.url,
                    contentType: result.data.contentType || file.type,
                };

                if (setAttachedFiles) {
                    setAttachedFiles(prev => [...prev, uploadedFile]);
                }
                if (setFileIdList) {
                    setFileIdList(prev => [...prev, uploadedFile.id]);
                }
                toast.success(`${file.name} 파일이 첨부되었습니다.`);
            } else {
                toast.error(result.message || '파일 업로드에 실패했습니다.');
            }
        } catch (error) {
            toast.error('파일 업로드 중 오류가 발생했습니다.');
        } finally {
            event.target.value = '';
        }
    }, [postId, setAttachedFiles, setFileIdList, toast, validateFile]);

    const triggerFileInput = useCallback(() => {
        fileInputRef.current?.click();
    }, []);

    const triggerImageInput = useCallback(() => {
        imageInputRef.current?.click();
    }, []);

    if (!editor) {
        return null;
    }

    return (
        <div className="tiptap-container">
            <MenuBar editor={editor} onFileUploadClick={triggerFileInput} onImageUploadClick={triggerImageInput} />
            <input
                type="file"
                ref={imageInputRef}
                style={{ display: 'none' }}
                accept="image/*"
                onChange={handleImageChange}
            />
            <input
                type="file"
                ref={fileInputRef}
                style={{ display: 'none' }}
                accept="application/pdf, application/msword, text/plain, 
                application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, 
                application/vnd.openxmlformats-officedocument.presentationml.presentation, 
                application/zip"
                onChange={handleFileChange}
            />
            <EditorContent editor={editor} className="tiptap-content" />
        </div>
    );
}