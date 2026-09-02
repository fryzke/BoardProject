import { useState } from "react";
import { createComment, updateComment } from "../../api";

function CommentForm({ postId, comment, parentId, onSuccess, onCancel }) {
    const [content, setContent] = useState(comment?.content || "");
    const isEditMode = !!comment;

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!content.trim() || content.length > 400) {
            alert("본문 내용을 입력해주세요 (최대 400자).");
            return;
        }

        try {
            if (isEditMode) {
                await updateComment(postId, comment.id, content, comment.parentId);
                alert("댓글이 수정되었습니다.");
            } else {
                await createComment(postId, content, parentId || null);
                alert("댓글이 등록되었습니다.");
                setContent("");
            }

            if (onSuccess) {
                onSuccess();
            }
        } catch (error) {
            console.error("댓글 처리 오류:", error);
            alert(error.response?.data?.message || "처리 중 오류가 발생했습니다.");
        }
    };

    return (
        <form className="comment-form" onSubmit={handleSubmit}>
            <div className="comment-area">
                <label htmlFor="comment">댓글:</label>
                <textarea
                    id="comment"
                    name="comment"
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    placeholder="댓글을 입력해주세요..."
                    rows={3}
                />
            </div>
            <div className="comment-form-actions">
                <button type="submit" className="comment-submit-btn">{isEditMode ? "수정" : "게시"}</button>
                {isEditMode && onCancel && (
                    <button type="button" className="comment-cancel-btn" onClick={onCancel}>취소</button>
                )}
                {!isEditMode && onCancel && (
                    <button type="button" className="comment-cancel-btn" onClick={onCancel}>취소</button>
                )}
            </div>

        </form>
    );
}

export default CommentForm;
