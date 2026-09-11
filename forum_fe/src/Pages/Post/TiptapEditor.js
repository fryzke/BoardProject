import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Image from '@tiptap/extension-image';
import TextAlign from '@tiptap/extension-text-align';
import DOMPurify from 'dompurify';
import { useRef, useCallback, useEffect, useState } from 'react';
import {
    Bold, Italic, Strikethrough, Heading1, Heading2, Heading3,
    List, ListOrdered, ImageIcon, AlignLeft, AlignCenter, AlignRight, AlignJustify, FileUp
} from 'lucide-react';
import { normalizeContentForEditor } from '../../utils';
import { uploadImage } from '../../api';
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

export default function TiptapEditor({ content, onChange, postId, setFileIdList }) {
    const toast = useToast();
    const [totalSize, setTotalSize] = useState(0);
    const [totalFileCount, settotalFileCount] = useState(0);
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
        onUpdate: ({ editor }) => {
            const html = editor.getHTML();
            onChange(DOMPurify.sanitize(html));
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

    const handleFileChange = useCallback(async (event) => {
        const file = event.target.files?.[0];

        if (!file) return;

        // 1차 프론트엔드 검증 시작
        const allowedExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'pdf', 'doc', 'docx', 'txt', 'xlsx', 'pptx', 'zip'];
        const fileName = file.name || '';
        const fileExtension = fileName.includes('.') ? fileName.split('.').pop().toLowerCase() : '';

        if (!allowedExtensions.includes(fileExtension)) {
            toast.warning(`허용되지 않는 파일 형식입니다: .${fileExtension || 'unknown'}`);
            event.target.value = '';
            return;
        }

        if (totalFileCount >= FileMaximum.TOTAL_IMAGE_NUMBER) {
            toast.warning(`파일은 최대 ${FileMaximum.TOTAL_IMAGE_NUMBER}개까지 업로드 가능합니다.`);
            event.target.value = '';
            return;
        }

        const fileSize = file.size;

        if (fileSize > FileMaximum.MAX_SIZE) {
            toast.warning(`파일은 최대 ${FileMaximum.MAX_SIZE / (1024 * 1024)}MB까지 업로드 가능합니다.`);
            event.target.value = '';
            return;
        }

        if (totalSize + fileSize > FileMaximum.TOTAL_MAX_SIZE) {
            toast.warning(`파일은 총합 ${FileMaximum.TOTAL_MAX_SIZE / (1024 * 1024)}MB까지 업로드할 수 있습니다.`);
            event.target.value = '';
            return;
        }

        // 1차 검증 통과 후 백엔드 업로드 요청
        try {
            const result = await uploadImage(file, postId);
            if (result.success && result.url) {
                if (file.type.startsWith('image/')) {
                    editor.chain().focus().setImage({ src: result.url }).run();
                } else {
                    editor.chain().focus()
                        .insertContent({
                            type: 'text',
                            text: `📎 ${file.name}`,
                            marks: [
                                {
                                    type: 'link',
                                    attrs: {
                                        href: result.url,
                                        target: '_blank',
                                        class: 'attachment-card',
                                        'data-filename': file.name,
                                        'data-filesize': String(file.size),
                                    },
                                },
                            ],
                        })
                        .insertContent(' ')
                        .run();
                }
                setTotalSize(pre => pre + fileSize);
                settotalFileCount(pre => pre + 1);
                setFileIdList(pre => [...pre, result.data.id]);
                toast.success(`${file.name} 파일이 첨부되었습니다.`);
            } else {
                toast.error(result.message || '파일 업로드에 실패했습니다.');
            }
        } catch (error) {
            toast.error('파일 업로드 중 오류가 발생했습니다.');
        } finally {
            event.target.value = '';
        }
    }, [editor, postId, totalFileCount, totalSize, setFileIdList, toast]);

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
                onChange={handleFileChange}
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