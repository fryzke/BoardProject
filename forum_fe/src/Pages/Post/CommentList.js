import CommentItem from './CommentItem';

function CommentList({ postId, comments, isLoggedIn, currentUserId, onRefresh }) {
    return (
        <div className="comment-list">
            {comments?.map((comment) => (
                <div>
                    <CommentItem 
                        key={comment.id}
                        postId={postId}
                        isLoggedIn={isLoggedIn}
                        currentUserId={currentUserId}
                        comment={comment}
                        onRefresh={onRefresh} />
                </div>
            ))}
        </div>
    );
}

export default CommentList;