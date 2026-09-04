import CommentItem from './CommentItem';

function CommentList({ postId, comments, isLoggedIn, currentUserId, onRefresh }) {
    return (
        <div className="comment-list">
            {comments && comments.length > 0 ? (
                comments.map((comment) => (
                    <CommentItem 
                        key={comment.id}
                        postId={postId}
                        isLoggedIn={isLoggedIn}
                        currentUserId={currentUserId}
                        comment={comment}
                        onRefresh={onRefresh}
                    />
                ))
            ) : (
                <div className="comment-empty">등록된 댓글이 없습니다.</div>
            )}
        </div>
    );
}

export default CommentList;