import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { Role, Category, PostValidation } from '../../enum';
import { getPost, createPost, updatePost, deleteBatchImages } from '../../api';
import './PostEditPage.css';
import TiptapEditor from './TiptapEditor';
import { useToast } from '../../Components/Toast/ToastContext';

function PostEditPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const toast = useToast();
    const isEditMode = !!id;

    const [title, setTitle] = useState("");
    const [selectedCategory, setSelectedCategory] = useState(Category.TALK);
    const [fileIdList, setFileIdList] = useState([]);
    const [content, setContent] = useState("");
    const [isPinned, setIsPinned] = useState(false);
    const [loading, setLoading] = useState(isEditMode);
    const userId = localStorage.getItem("userId");
    const userRole = localStorage.getItem("userRole");

    useEffect(() => {
        const controller = new AbortController();

        if (isEditMode) {
            const fetchPost = async () => {
                try {
                    const data = await getPost(id, { signal: controller.signal });
                    if (userId !== data.author) {
                        toast.error("본인 게시글만 수정할 수 있습니다.");
                        navigate("/");
                        return;
                    }
                    setTitle(data.title);
                    setContent(data.content);
                    if (data.category) setSelectedCategory(data.category);
                    setIsPinned(Boolean(data.isPinned ?? data.pinned));
                } catch (error) {
                    if (!axios.isCancel(error)) {
                        toast.error("게시글을 불러올 수 없습니다.");
                        navigate("/");
                    }
                } finally {
                    if (!controller.signal.aborted) {
                        setLoading(false);
                    }
                }
            };
            fetchPost();
        }

        return () => {
            controller.abort();
        };
    }, [id, isEditMode, userId, navigate, toast]);

    const handleSubmit = async () => {
        if (title.trim().length < PostValidation.MIN_TITLE_LENGTH || title.trim().length > PostValidation.MAX_TITLE_LENGTH) {
            toast.warning(`제목은 ${PostValidation.MIN_TITLE_LENGTH}자 이상 ${PostValidation.MAX_TITLE_LENGTH}자 이하로 입력해주세요.`);
            return;
        }
        if (!content.trim() || content.length > PostValidation.MAX_CONTENT_LENGTH) {
            toast.warning(`본문 내용을 입력해주세요 (최대 ${PostValidation.MAX_CONTENT_LENGTH.toLocaleString()}자).`);
            return;
        }

        if (!selectedCategory.trim()) {
            toast.warning("카테고리를 선택해주세요.");
            return;
        }

        if (selectedCategory === Category.NOTICE && userRole !== Role.ADMIN) {
            toast.warning("공지사항은 관리자만 작성할 수 있습니다.");
            return;
        }

        const finalPinned = (userRole === Role.ADMIN) ? isPinned : false;

        try {
            if (isEditMode) {
                await updatePost(id, title, selectedCategory, content, finalPinned);
                toast.success("게시글이 수정되었습니다.");
                navigate(`/post/${id}`);
            } else {
                await createPost(title, selectedCategory, content, finalPinned);
                toast.success("게시글이 등록되었습니다.");
                navigate("/");
            }
        } catch (error) {
            const msg = error.response?.data?.message || "처리 중 오류가 발생했습니다.";
            toast.error(msg);
        }
    };

    const handleCancel = async () => {
        if (fileIdList.length > 0) {
            try {
                await deleteBatchImages(fileIdList);
            } catch (error) {
                console.warn("취소 시 이미지 즉시 삭제 실패 (추후 스케줄러가 자동 정리함):", error);
            }
        }
        navigate(-1);
    };

    if (loading) {
        return <div className="PostEditContainer" style={{ textAlign: 'center', padding: '60px' }}>게시글을 불러오는 중...</div>;
    }

    return (
        <div className="PostEditContainer">
            <h1 className="PostEditHeader">{isEditMode ? "게시글 수정" : "새 게시글 작성"}</h1>

            <div className="PostEditForm">
                <div className="PostEditMetaRow">
                    <select
                        className="PostCategory"
                        id="category"
                        value={selectedCategory}
                        onChange={(e) => setSelectedCategory(e.target.value)}>
                        {
                            Object.values(Category)
                                .filter(category => (category !== Category.NOTICE || userRole === Role.ADMIN) && category !== Category.ALL)
                                .map((category) => (
                                    <option key={category} value={category}>{category}</option>
                                ))
                        }
                    </select>

                    {userRole === Role.ADMIN && (
                        <label className="PinnedCheckboxLabel">
                            <input
                                type="checkbox"
                                checked={isPinned}
                                onChange={(e) => setIsPinned(e.target.checked)}
                            />
                            <span>📌 상단 고정 (최대 5개)</span>
                        </label>
                    )}
                </div>

                <input
                    className="PostEditTitleInput"
                    type="text"
                    placeholder="제목을 입력하세요"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                />

                <TiptapEditor
                    className="PostEditContentInput"
                    onChange={setContent}
                    content={content}
                    postId={id ? Number(id) : null}
                    setFileIdList={setFileIdList}
                />
            </div>

            <div className="PostEditActions">
                <button className="CancelBtn" onClick={handleCancel}>취소</button>
                <button className="SubmitBtn" onClick={handleSubmit}>
                    {isEditMode ? "수정 완료" : "등록하기"}
                </button>
            </div>
        </div>
    );
}

export default PostEditPage;
