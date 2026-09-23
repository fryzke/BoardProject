import { useState, useEffect } from 'react';
import './SearchBar.css';
import { searchOption, searchValidation } from '../../enum';
import { useNavigate, useSearchParams } from 'react-router-dom';

export default function SearchBar() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [keyword, setKeyword] = useState(searchParams.get('keyword') || "");
    const [option, setOption] = useState(searchParams.get('option') || searchOption.TITLE);
    const isValid = keyword.trim() && keyword.trim().length > 0;

    useEffect(() => {
        setKeyword(searchParams.get('keyword') || "");
        setOption(searchParams.get('option') || searchOption.TITLE);
    }, [searchParams]);

    const handleSearch = () => {
        const trimmed = keyword.trim();
        if (!trimmed) {
            return;
        }
        navigate(`/search?keyword=${encodeURIComponent(trimmed)}&option=${option}`);
    };

    const handleKeyDown = (event) => {
        if (event.key === 'Enter') {
            handleSearch();
        }
    };

    return (
        <div className="SearchBar">
            <select
                className="SearchSelect"
                id="option"
                value={option}
                onChange={(e) => setOption(e.target.value)}
            >
                <option value={searchOption.TITLE}>제목</option>
                <option value={searchOption.CONTENT}>본문</option>
                <option value={searchOption.BOTH}>제목+본문</option>
            </select>
            <input
                className="SearchInput"
                type="text"
                maxLength={searchValidation.MAX_KEYWORD_LENGTH}
                placeholder="검색어를 입력하세요"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                onKeyDown={handleKeyDown}
            />
            <button
                className="SearchButton"
                type="button"
                disabled={!isValid}
                onClick={handleSearch}
            >
                검색
            </button>
        </div>
    );
}

