export const Role = {
    USER: "USER",
    ADMIN: "ADMIN"
}
Object.freeze(Role);

export const Category = {
    ALL: "전체",
    NOTICE: "공지사항",
    TALK: "자유",
    INFO: "정보 공유",
    ASK: "질문",
    REVIEW: "후기"
}
Object.freeze(Category);

export const FileMaximum = {
    MAX_SIZE: 20 * 1024 * 1024,
    TOTAL_MAX_SIZE: 100 * 1024 * 1024,
    TOTAL_IMAGE_NUMBER: 10
}