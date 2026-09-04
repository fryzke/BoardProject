import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { formatDate, formatDetailContent } from '../../utils';
import { getPost, deletePost } from '../../api';
import './PostDetailPage.css';
import parse from 'html-react-parser';
import CommentSection from './CommentSection';

function PostDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [post, setPost] = useState(null);
    const [isLoggedIn, setIsLoggedIn] = useState(false);

    useEffect(() => {
        const token = localStorage.getItem("accessToken");
        if (token) {
            setIsLoggedIn(true);
        }

        const controller = new AbortController();

        const fetchPostDetail = async () => {
            try {
                const data = await getPost(id, { signal: controller.signal });
                setPost(data);
            } catch (error) {
                if (!axios.isCancel(error)) {
                    alert("게시글을 불러올 수 없습니다.");
                    navigate("/");
                }
            }
        };

        fetchPostDetail();

        return () => {
            controller.abort();
        };
    }, [id, navigate]);

    const handleDelete = async () => {
        if (window.confirm("정말 이 게시글을 삭제하시겠습니까?")) {
            try {
                await deletePost(id);
                alert("삭제되었습니다.");
                navigate("/");
            } catch (error) {
                alert("삭제에 실패했습니다.");
            }
        }
    };

    const currentUserId = localStorage.getItem("userId");
    const isAuthor = Boolean(isLoggedIn && currentUserId && post && post.author === currentUserId);

    if (!post) {
        return <div className="PostDetailWrapper">로딩 중...</div>;
    }

    return (
        <div className="PostDetailContainer">
            <div className="PostDetailHeader">
                <h1 className="PostDetailTitle">
                    {Boolean(post.isPinned ?? post.pinned) && (
                        <span className="DetailPinnedBadge">📌 고정글</span>
                    )}
                    {post.title}
                </h1>
                <div className="PostDetailInfo">
                    <span className="Author">작성자: {post.author || '-'}</span>
                    <span className="Date">작성일: {formatDate(post.createdAt || post.date)}</span>
                    <span className="Views">조회수: {post.viewCount ?? 0}</span>
                </div>
            </div>

            <div className="PostDetailContent">
                {parse(formatDetailContent(post.content))}
            </div>
            <CommentSection
                postId={id}
                isLoggedIn={isLoggedIn}
                currentUserId={currentUserId}
            />
            <div className="PostDetailActions">
                <button className="ActionBtn BackBtn" onClick={() => navigate("/")}>목록으로</button>

                {isAuthor && (
                    <div className="AuthActions">
                        <button className="ActionBtn EditBtn" onClick={() => navigate(`/edit/${id}`)}>수정</button>
                        <button className="ActionBtn DeleteBtn" onClick={handleDelete}>삭제</button>
                    </div>
                )}
            </div>
        </div>
    );
}

export default PostDetailPage;
