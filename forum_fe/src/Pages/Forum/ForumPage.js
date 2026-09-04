import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import './ForumPage.css';
import Pagination from './Pagination';
import { formatDate } from '../../utils';
import { fetchPosts, logoutUser } from '../../api';
import { Category } from '../../enum';

function ForumPage() {
    const navigate = useNavigate();
    const [posts, setPosts] = useState([]);
    const [currentCategory, setCurrentCategory] = useState("전체");
    const [sort, setSort] = useState("latest");
    const [currentPage, setCurrentPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [totalPosts, setTotalPosts] = useState(0);
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [loading, setLoading] = useState(false);

    const [userName, setUserName] = useState("");
    const userGrade = localStorage.getItem("userGrade");

    useEffect(() => {
        const token = localStorage.getItem("accessToken");
        const storedUserName = localStorage.getItem("userName") || localStorage.getItem("userId");

        if (token) {
            setIsLoggedIn(true);
            if (storedUserName) setUserName(storedUserName);
        }
    }, []);

    useEffect(() => {
        const controller = new AbortController();

        const loadPosts = async () => {
            setLoading(true);
            var category = null;
            if (currentCategory === "전체") {
                category = "all";
            } else {
                category = currentCategory;
            }
            try {
                const result = await fetchPosts(currentPage, 20, sort, category, { signal: controller.signal });
                setPosts(result.data);
                setTotalPages(result.pagination.totalPages);
                setCurrentPage(result.pagination.currentPage);
                setTotalPosts(result.pagination.totalPosts);
            } catch (error) {
                if (!axios.isCancel(error)) {
                    console.error("Failed to load posts", error);
                }
            } finally {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            }
        };

        loadPosts();

        return () => {
            controller.abort();
        };
    }, [currentPage, currentCategory, sort]);

    const handleAuthAction = async () => {
        if (isLoggedIn) {
            localStorage.removeItem("accessToken");
            localStorage.removeItem("userId");
            localStorage.removeItem("userName");
            try {
                const result = await logoutUser();
                if (result.success) {
                    alert("로그아웃 되었습니다.");
                } else {
                    alert("로그아웃에 실패하였습니다.");
                    console.error("Failed to logout", result.message);
                }
                setIsLoggedIn(false);
                setUserName("");
            } catch (error) {
                alert("로그아웃에 실패하였습니다.");
                console.error("Failed to logout :", error);
            }
        } else {
            navigate("/sign-in");
        }
    };

    return (
        <div className="Forum">
            <div className="ForumContainer">
                <div className="ForumHeader">
                    <div className="ForumTitleSection">
                        <h1 className="ForumTitle" onClick={() => setCurrentPage(1)}>
                            {currentCategory}게시판
                        </h1>
                        <span className="TotalCount">총 <strong>{totalPosts}</strong>건</span>
                    </div>

                    <div className="NavActions">
                        {isLoggedIn && (
                            <>
                                <span className="UserWelcome">
                                    <strong>{userName}</strong> 님 환영합니다
                                </span>
                                <div className='UserWelcome'>
                                    현재등급:{userGrade}
                                </div>
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

                <div className="ForumFilterBar">
                    <select
                        className="ForumCategory"
                        id="category"
                        value={currentCategory}
                        onChange={(e) => setCurrentCategory(e.target.value)}>
                        {
                            Object.values(Category).map((category) => (
                                <option value={category}>{category}</option>
                            ))
                        }
                    </select>

                    <div className="ForumSortOptions">
                        <span className={`SortOption ${sort === 'latest' ? 'active' : ''}`}
                            onClick={() => setSort("latest")}>최신순</span>
                        <span className="SortDivider">|</span>
                        <span className={`SortOption ${sort === 'popular' ? 'active' : ''}`}
                            onClick={() => setSort("popular")}>인기순</span>
                    </div>
                </div>

                <div className="TableWrapper">
                    <table className="ForumTable">
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
                                        게시글을 불러오는 중입니다...
                                    </td>
                                </tr>
                            ) : posts.length === 0 ? (
                                <tr>
                                    <td colSpan="6" className="EmptyMessage">
                                        등록된 게시글이 없습니다.
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
                                                    totalPosts - (20 * (currentPage - 1)) - idx
                                                )}
                                            </td>
                                            <td className="TdCategory">{post.category}</td>
                                            <td className="TdTitle">
                                                {isPinned && <span className="PinnedTitleTag">[고정]</span>}
                                                <span className="TitleText">{post.title}</span>
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

                <div className="ForumFooter">
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
            </div>
        </div>
    );
}

export default ForumPage;