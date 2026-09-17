import './SearchResultPage.css';
import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import axios from 'axios';
import Pagination from '../Forum/Pagination';
import { formatDate } from '../../utils';
import { fetchPosts, logoutUser } from '../../api';
import { SortType, PaginationConfig } from '../../enum';
import { useToast } from '../../Components/Toast/ToastContext';
import SearchBar from '../../Components/Search/SearchBar';

function SearchResultPage() {
    const navigate = useNavigate();
    const toast = useToast();
    const [searchParams] = useSearchParams();
    const [posts, setPosts] = useState([]);
    const [sort, setSort] = useState(SortType.LATEST);
    const keyword = searchParams.get('keyword') || null;
    const option = searchParams.get('option') || null;
    const [currentPage, setCurrentPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [totalPosts, setTotalPosts] = useState(0);
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const [userName, setUserName] = useState("");
    const userGrade = localStorage.getItem("userGrade");

    useEffect(() => {
        const storedUserName = localStorage.getItem("userName") || localStorage.getItem("userId");
        if (storedUserName) {
            setUserName(storedUserName);
        }

        if (localStorage.getItem("userId")) {
            setIsLoggedIn(true);
        }
    }, []);

    const loadPosts = useCallback(async (signal) => {
        setLoading(true);
        setError(null);
        try {
            const result = await fetchPosts(
                currentPage,
                PaginationConfig.POSTS_PER_PAGE,
                sort,
                "all",
                keyword,
                option,
                signal ? { signal } : {}
            );
            setPosts(result.data);
            setTotalPages(result.pagination?.totalPages || 1);
            setCurrentPage(result.pagination?.currentPage || 1);
            setTotalPosts(result.pagination?.totalPosts || 0);
        } catch (err) {
            if (!axios.isCancel(err)) {
                console.error("Failed to load search posts", err);
                setError("검색 결과를 불러오는 도중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
            }
        } finally {
            if (!signal || !signal.aborted) {
                setLoading(false);
            }
        }
    }, [currentPage, keyword, option, sort]);

    useEffect(() => {
        const controller = new AbortController();
        loadPosts(controller.signal);

        return () => {
            controller.abort();
        };
    }, [loadPosts]);

    const handleAuthAction = async () => {
        if (isLoggedIn) {
            localStorage.removeItem("userId");
            localStorage.removeItem("userName");
            localStorage.removeItem("userRole");
            localStorage.removeItem("userGrade");
            try {
                const result = await logoutUser();
                if (result.success) {
                    toast.success("로그아웃 되었습니다.");
                } else {
                    toast.error("로그아웃에 실패하였습니다.");
                    console.error("Failed to logout", result.message);
                }
                setIsLoggedIn(false);
                setUserName("");
            } catch (error) {
                toast.error("로그아웃에 실패하였습니다.");
                console.error("Failed to logout :", error);
            }
        } else {
            navigate("/sign-in");
        }
    };

    return (
        <div className="SearchResult">
            <div className="SearchResultContainer">
                <div className="SearchResultHeader">
                    <div className="SearchResultTitleSection">
                        <h1 className="SearchResultTitle" onClick={() => setCurrentPage(1)}>
                            검색 결과
                        </h1>
                        <span className="TotalCount">총 <strong>{totalPosts}</strong>건</span>
                    </div>

                    <div className="NavActions">
                        {isLoggedIn && (
                            <>
                                <span
                                    className="UserWelcome"
                                    style={{ cursor: 'pointer' }}
                                    onClick={() => navigate('/mypage')}
                                    title="마이페이지로 이동"
                                >
                                    <strong>{userName}</strong> 님
                                </span>
                                <div className='UserWelcome'>
                                    등급: {userGrade || '일반'}
                                </div>
                                <button className="ActionButton MyPageButton" onClick={() => navigate('/mypage')}>
                                    마이페이지
                                </button>
                                <button className="ActionButton WriteButton" onClick={() => navigate('/write')}>
                                    글쓰기
                                </button>
                            </>
                        )}
                        <button
                            className={`ActionButton ${isLoggedIn ? 'LogoutButton' : 'LoginButton'}`}
                            onClick={handleAuthAction}
                        >
                            {isLoggedIn ? "로그아웃" : "로그인"}
                        </button>
                    </div>
                </div>

                <div className="SearchResultFilterBar">
                    <div className="SearchResultSortOptions">
                        <span className={`SortOption ${sort === SortType.LATEST ? 'active' : ''}`}
                            onClick={() => setSort(SortType.LATEST)}>최신순</span>
                        <span className="SortDivider">|</span>
                        <span className={`SortOption ${sort === SortType.POPULAR ? 'active' : ''}`}
                            onClick={() => setSort(SortType.POPULAR)}>인기순</span>
                    </div>
                </div>

                <div className="TableWrapper">
                    <table className="SearchResultTable">
                        <thead>
                            <tr>
                                <th className="ThNo">번호</th>
                                <th className="ThCategory">카테고리</th>
                                <th className="ThTitle">제목</th>
                                <th className="ThAuthor">작성자</th>
                                <th className="ThDate">작성일</th>
                                <th className="ThViews">조회</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr>
                                    <td colSpan="6" className="EmptyMessage">
                                        <div className="FeedbackLoading">
                                            <div className="Spinner"></div>
                                            <span>검색 결과를 불러오는 중입니다...</span>
                                        </div>
                                    </td>
                                </tr>
                            ) : error ? (
                                <tr>
                                    <td colSpan="6" className="FeedbackCell">
                                        <div className="FeedbackCard ErrorCard">
                                            <div className="FeedbackIcon">⚠️</div>
                                            <h3 className="FeedbackTitle">검색 결과를 불러오지 못했습니다</h3>
                                            <p className="FeedbackDesc">{error}</p>
                                            <button className="RetryButton" onClick={() => loadPosts()}>
                                                다시 시도
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ) : posts.length === 0 ? (
                                <tr>
                                    <td colSpan="6" className="FeedbackCell">
                                        <div className="FeedbackCard EmptyCard">
                                            <div className="FeedbackIcon">🔍</div>
                                            <h3 className="FeedbackTitle">
                                                {keyword ? (
                                                    <><strong>"{keyword}"</strong>에 대한 검색 결과가 없습니다.</>
                                                ) : (
                                                    <>등록된 검색 결과가 없습니다.</>
                                                )}
                                            </h3>
                                            <div className="FeedbackTips">
                                                <p className="TipsTitle">💡 검색 팁</p>
                                                <ul>
                                                    <li>단어의 철자가 정확한지 확인해 보세요.</li>
                                                    <li>검색어의 단어 수를 줄이거나 보다 일반적인 키워드로 검색해 보세요.</li>
                                                    <li>검색 조건(제목/본문/제목+본문)을 다시 한번 확인해 보세요.</li>
                                                </ul>
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            ) : (
                                posts.map((post, idx) => {
                                    const isPinned = Boolean(post.isPinned ?? post.pinned);
                                    return (
                                        <tr
                                            key={post.id}
                                            className={`PostRow ${isPinned ? 'PinnedRow' : ''}`}
                                            onClick={() => navigate(`/post/${post.id}`)}
                                        >
                                            <td className="TdNo">
                                                {isPinned ? (
                                                    <span className="PinnedIconBadge">📌 고정</span>
                                                ) : (
                                                    totalPosts - (PaginationConfig.POSTS_PER_PAGE * (currentPage - 1)) - idx
                                                )}
                                            </td>
                                            <td className="TdCategory">{post.category}</td>
                                            <td className="TdTitle">
                                                <div className="TitleWrapper">
                                                    {isPinned && <span className="PinnedTitleTag">[고정]</span>}
                                                    <span className="TitleText">{post.title}</span>
                                                    {post.commentCount > 0 && (
                                                        <span className="CommentBadge" title={`댓글 ${post.commentCount}개`}>
                                                            <span className="CommentIcon">💬</span>
                                                            <span className="CommentCountNumber">{post.commentCount}</span>
                                                        </span>
                                                    )}
                                                </div>
                                            </td>
                                            <td className="TdAuthor">{post.author || '-'}</td>
                                            <td className="TdDate">{formatDate(post.createdAt || post.date)}</td>
                                            <td className="TdViews">{post.viewCount ?? 0}</td>
                                        </tr>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                </div>

                <div className="SearchResultFooter">
                    <div className="PaginationWrapper">
                        <Pagination
                            currentPage={currentPage}
                            totalPages={totalPages}
                            onPageChange={(page) => setCurrentPage(page)}
                        />
                    </div>
                    {isLoggedIn && (
                        <div className="BottomActions">
                            <button className="ActionButton WriteButton" onClick={() => navigate('/write')}>
                                글쓰기
                            </button>
                        </div>
                    )}
                </div>

                <div className="SearchResultSearchSection">
                    <SearchBar />
                </div>
            </div>
        </div>
    );
}
export default SearchResultPage;