import { useEffect, useState, useCallback } from "react";
import { getComments } from "../../api";
import CommentForm from "./CommentForm";
import CommentList from "./CommentList";
import Pagination from "../Forum/Pagination";
import './CommentSection.css';

function CommentSection({ postId, isLoggedIn, currentUserId }) {
    const [comments, setComments] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [totalComments, setTotalComments] = useState(0);

    const fetchComments = useCallback(async (page = 1) => {
        if (!postId) return;
        try {
            const result = await getComments(postId, page, 10);
            setComments(result.data || []);
            if (result.pagination) {
                setTotalPages(result.pagination.totalPages);
                setCurrentPage(result.pagination.currentPage);
                setTotalComments(result.pagination.totalComments);
            }
        } catch (error) {
            console.error("댓글을 불러올 수 없습니다:", error);
            setComments([]);
        }
    }, [postId]);

    useEffect(() => {
        if (postId) {
            fetchComments(currentPage);
        }
    }, [postId, currentPage, fetchComments]);

    const handlePageChange = (newPage) => {
        setCurrentPage(newPage);
    };

    const handleCreateSuccess = () => {
        if (currentPage === 1) {
            fetchComments(1);
        } else {
            setCurrentPage(1);
        }
    };

    return (
        <div className="comment-section">
            <div className="comment-header">
                <h3>댓글 {totalComments > 0 && <span className="comment-count">({totalComments})</span>}</h3>
            </div>

            <CommentList
                postId={postId}
                comments={comments}
                isLoggedIn={isLoggedIn}
                currentUserId={currentUserId}
                onRefresh={() => fetchComments(currentPage)}
            />

            {totalPages > 0 && (
                <div className="comment-pagination">
                    <Pagination
                        currentPage={currentPage}
                        totalPages={totalPages}
                        onPageChange={handlePageChange}
                    />
                </div>
            )}

            <hr />
            {isLoggedIn && (
                <CommentForm postId={postId} onSuccess={handleCreateSuccess} />
            )}
        </div>
    );
}

export default CommentSection;
