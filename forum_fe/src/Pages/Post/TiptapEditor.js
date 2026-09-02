import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Image from '@tiptap/extension-image';
import TextAlign from '@tiptap/extension-text-align';
import DOMPurify from 'dompurify';
import { useRef, useCallback, useEffect } from 'react';
import {
    Bold, Italic, Strikethrough, Heading1, Heading2, Heading3,
    List, ListOrdered, ImageIcon, AlignLeft, AlignCenter, AlignRight, AlignJustify
} from 'lucide-react';
import { normalizeContentForEditor } from '../../utils';
import { uploadImage } from '../../api';
import './TiptapEditor.css';

const MenuBar = ({ editor, onImageUploadClick}) => {
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
        </div>
    );
};

export default function TiptapEditor({ content, onChange, postId }) {
    const fileInputRef = useRef(null);

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

        try {
            const result = await uploadImage(file, postId);
            if (result.success && result.url) {
                editor.chain().focus().setImage({ src: result.url }).run();
            } else {
                alert(result.message || '이미지 업로드에 실패했습니다.');
            }
        } catch (error) {
            alert('이미지 업로드 중 오류가 발생했습니다.');
        } finally {
            event.target.value = '';
        }
    }, [editor, postId]);

    const triggerFileInput = useCallback(() => {
        fileInputRef.current?.click();
    }, []);

    if (!editor) {
        return null;
    }

    return (
        <div className="tiptap-container">
            <MenuBar editor={editor} onImageUploadClick={triggerFileInput} />
            <input
                type="file"
                ref={fileInputRef}
                style={{ display: 'none' }}
                accept="image/*"
                onChange={handleFileChange}
            />
            <EditorContent editor={editor} className="tiptap-content" />
        </div>
    );
}