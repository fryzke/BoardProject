import { useEffect, useState } from "react";
import { getComments } from "../../api";
import CommentForm from "./CommentForm";
import CommentList from "./CommentList";
import './CommentSection.css';

function CommentSection({ postId, isLoggedIn, currentUserId }) {
    const [comments, setComments] = useState([]);

    const fetchComments = async () => {
        try {
            const data = await getComments(postId);
            setComments(Array.isArray(data) ? data : []);
        } catch (error) {
            console.error("댓글을 불러올 수 없습니다:", error);
            setComments([]);
        }
    };

    useEffect(() => {
        if (postId) {
            fetchComments();
        }
    }, [postId]);

    return (
        <div className="comment-section">
            <div className="comment-header">
                <h3>댓글</h3>
            </div>

            <CommentList
                postId={postId}
                comments={comments}
                isLoggedIn={isLoggedIn}
                currentUserId={currentUserId}
                onRefresh={fetchComments}
            />
            <hr />
            {isLoggedIn
                && <CommentForm postId={postId} onSuccess={fetchComments} />
            }
        </div>
    );
}

export default CommentSection;
