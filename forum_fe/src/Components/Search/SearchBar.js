import { useState } from 'react';
import './SearchBar.css';
import { searchOption, searchValidation } from '../../enum';

export default function SearchBar() {
    const [keyword, setKeyword] = useState("");
    const [option, setOption] = useState(searchOption.TITLE);
    const isValid = keyword && keyword.trim().length > 0;

    const handleSearch = async () => {

    };

    const handleEnter = (event) => {
        if (event.key === 'Enter' && isValid) {
            handleSearch();
        }
    };

    return (
        <div>
            <select
                id="option"
                onChange={(e) => setOption(e.target.value)}
            >
                <option value={searchOption.TITLE}>제목</option>
                <option value={searchOption.COTENT}>본문</option>
                <option value={searchOption.BOTH}>제목+본문</option>
            </select>
            <input
                type="text"
                maxLength={searchValidation.MAX_KEYWORD_LENGTH}
                placeholder='검색어를 입력하세요.'
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)} />
            <button
                onClick={handleSearch}
                onKeyDown={handleEnter}
            >검색</button>
        </div>
    );
}
