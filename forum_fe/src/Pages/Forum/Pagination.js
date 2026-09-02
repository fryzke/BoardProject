import './Pagination.css';

function Pagination({ currentPage, totalPages, onPageChange }) {
    const pages = [];
    for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
    }

    return (
        <div className="Pagination">
            <button 
                className="PageButton" 
                disabled={currentPage === 1}
                onClick={() => onPageChange(currentPage - 1)}
            >
                이전
            </button>
            {pages.map((page) => (
                <button 
                    key={page} 
                    className={`PageButton ${currentPage === page ? 'Active' : ''}`}
                    onClick={() => onPageChange(page)}
                >
                    {page}
                </button>
            ))}
            <button 
                className="PageButton" 
                disabled={currentPage === totalPages || totalPages === 0}
                onClick={() => onPageChange(currentPage + 1)}
            >
                다음
            </button>
        </div>
    );
}

export default Pagination;
