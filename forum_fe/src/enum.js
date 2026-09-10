export const Role = {
    USER: "USER",
    ADMIN: "ADMIN",
};
Object.freeze(Role);

export const Category = {
    ALL: "전체",
    NOTICE: "공지사항",
    TALK: "자유",
    INFO: "정보 공유",
    ASK: "질문",
    REVIEW: "후기",
};
Object.freeze(Category);

export const SortType = {
    LATEST: "latest",
    POPULAR: "popular",
};
Object.freeze(SortType);

export const FileMaximum = {
    MAX_SIZE: 20 * 1024 * 1024,
    TOTAL_MAX_SIZE: 100 * 1024 * 1024,
    TOTAL_IMAGE_NUMBER: 10,
};
Object.freeze(FileMaximum);

export const AuthValidation = {
    MIN_ID_LENGTH: 4,
    MAX_ID_LENGTH: 16,
    MIN_NAME_LENGTH: 2,
    MAX_NAME_LENGTH: 20,
    PASSWORD_REGEX: /^(?=.*[A-Za-z])(?=.*\d)(?=.*[+=%_!@#$^&*?]).{8,}$/,
};
Object.freeze(AuthValidation);

export const PostValidation = {
    MIN_TITLE_LENGTH: 2,
    MAX_TITLE_LENGTH: 100,
    MAX_CONTENT_LENGTH: 20000,
    MAX_PINNED_COUNT: 5,
};
Object.freeze(PostValidation);

export const CommentValidation = {
    MAX_CONTENT_LENGTH: 400,
};
Object.freeze(CommentValidation);

export const PaginationConfig = {
    POSTS_PER_PAGE: 20,
    COMMENTS_PER_PAGE: 10,
};
Object.freeze(PaginationConfig);