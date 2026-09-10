import { useState } from "react";
import { formatDate } from "../../utils";
import { deleteComment } from '../../api';
import CommentForm from "./CommentForm";
import { useToast } from "../../Components/Toast/ToastContext";
import { useModal } from "../../Components/Modal/ModalContext";

function CommentItem({ postId, isLoggedIn, currentUserId, comment, isReply = false, onRefresh }) {
    const toast = useToast();
    const { confirm } = useModal();
    const isAuthor = Boolean(isLoggedIn && currentUserId && comment && comment.author === currentUserId);
    const [isAddComment, setIsAddComment] = useState(false);
    const [isEditMode, setIsEditMode] = useState(false);

    const handleDelete = async () => {
        const isConfirmed = await confirm({
            title: "댓글 삭제",
            message: "댓글을 삭제하시겠습니까?",
            confirmText: "삭제",
            cancelText: "취소",
            isDestructive: true,
        });

        if (isConfirmed) {
            try {
                await deleteComment(postId, comment.id);
                toast.success("댓글이 삭제되었습니다.");
                onRefresh();
            } catch (error) {
                toast.error("댓글 삭제에 실패했습니다.");
            }
        }
    };

    return (
        <div className={isReply ? "comment-item comment-item-child" : "comment-item"}>
            {isEditMode ? (
                <CommentForm
                    postId={postId}
                    comment={comment}
                    onSuccess={() => {
                        setIsEditMode(false);
                        onRefresh();
                    }}
                    onCancel={() => setIsEditMode(false)}
                />
            ) : (
                <div className="comment-main">
                    <div className="comment-item-header">
                        <div className="comment-author-info">
                            <span className="comment-author">{comment.author || "익명"}</span>
                            <span className="comment-date">{formatDate(comment.createdAt)}</span>
                        </div>
                    </div>

                    <div className="comment-content">{comment.content}</div>

                    {!comment.isDeleted
                        && <div className="comment-actions">
                            {isLoggedIn
                                &&
                                <button className="comment-action-btn" onClick={() => setIsAddComment(!isAddComment)}>
                                    {isAddComment ? "답글 취소" : "답글"}
                                </button>
                            }
                            {isAuthor && (
                                <>
                                    <button className="comment-action-btn" onClick={() => setIsEditMode(true)}>수정</button>
                                    <button className="comment-action-btn delete-btn" onClick={handleDelete}>삭제</button>
                                </>
                            )}
                        </div>
                    }
                </div>
            )}

            {isAddComment && (
                <div className="reply-form-wrapper">
                    <CommentForm
                        postId={postId}
                        parentId={comment.id}
                        onSuccess={() => {
                            setIsAddComment(false);
                            onRefresh();
                        }}
                        onCancel={() => setIsAddComment(false)}
                    />
                </div>
            )}

            {comment.children && comment.children.length > 0 && (
                <div className="comment-children">
                    {comment.children.map((child) => (
                        <CommentItem
                            key={child.id}
                            postId={postId}
                            isLoggedIn={isLoggedIn}
                            currentUserId={currentUserId}
                            comment={child}
                            isReply={true}
                            onRefresh={onRefresh}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}

export default CommentItem;
