import axios from 'axios';
import { showToast } from './Components/Toast/ToastContext';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
});

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
    failedQueue.forEach((prom) => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve(token);
        }
    });
    failedQueue = [];
};

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // 401 에러이고 재발급 요청 자체가 아닌 경우 Refresh Token 쿠키로 자동 재발급 시도
        if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url?.includes('/auth/reissue')) {
            if (isRefreshing) {
                // 다른 요청이 이미 재발급을 진행 중이면 대기 큐에 저장
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                })
                    .then(() => {
                        return api(originalRequest);
                    })
                    .catch((err) => Promise.reject(err));
            }

            originalRequest._retry = true;
            isRefreshing = true;

            try {
                const res = await axios.post(`${API_BASE_URL}/auth/reissue`, {}, { withCredentials: true });
                if (res.data?.success) {
                    processQueue(null);
                    return api(originalRequest);
                } else {
                    throw new Error('토큰 재발급 응답이 올바르지 않습니다.');
                }
            } catch (reissueError) {
                processQueue(reissueError, null);
                localStorage.removeItem('userId');
                localStorage.removeItem('userName');
                localStorage.removeItem('userRole');
                localStorage.removeItem('userGrade');
                showToast.error('세션이 만료되었습니다. 다시 로그인해주세요.');
                window.location.replace('/sign-in');
                return Promise.reject(reissueError);
            } finally {
                isRefreshing = false;
            }
        }
        return Promise.reject(error);
    }
);

// ===== Auth APIs (실제 백엔드 연동) =====

export const registerUser = async (userId, userPassword, userName) => {
    try {
        const response = await api.post('/auth/signup', { userId, userPassword, userName });
        return response.data;
    } catch (error) {
        if (error.response && error.response.data) {
            return error.response.data;
        }
        console.error("Signup error:", error);
        throw error;
    }
};

export const loginUser = async (userId, userPassword) => {
    try {
        const response = await api.post('/auth/login', { userId, userPassword });
        return response.data;
    } catch (error) {
        if (error.response && error.response.data) {
            return error.response.data;
        }
        console.error("Login error:", error);
        throw error;
    }
};

export const logoutUser = async () => {
    try {
        const response = await api.post('/auth/logout');
        return response.data;
    } catch (error) {
        if (error.response && error.response.data) {
            return error.response.data;
        }
        console.error("Logout error:", error);
        throw error;
    }
};

export const reissueToken = async () => {
    try {
        const response = await axios.post(`${API_BASE_URL}/auth/reissue`, {}, { withCredentials: true });
        return response.data;
    } catch (error) {
        if (error.response?.data) {
            return error.response.data;
        }
        throw error;
    }
};

// ===== User APIs (마이페이지 연동) =====

export const getUserInfo = async () => {
    try {
        const response = await api.get('/users/me');
        return response.data;
    } catch (error) {
        console.error("Get user info error:", error);
        throw error;
    }
};

export const updateUserInfo = async (userData) => {
    try {
        const response = await api.put('/users/me', userData);
        return response.data;
    } catch (error) {
        console.error("Update user info error:", error);
        throw error;
    }
};

// ===== Post APIs (백엔드 실제 연동) =====

export const fetchPosts = async (page = 1, limit = 20, sort = "latest", category = "all", keyword = null, option = null, options = {}) => {
    try {
        const response = (keyword == null || option == null) 
        ? await api.get(`/posts?page=${page}&limit=${limit}&sort=${sort}&category=${category}`, options)
        : await api.get(`/posts?page=${page}&limit=${limit}&sort=${sort}&category=${category}&keyword=${keyword}&option=${option}`, options);
        return {
            data: response?.data.data ?? [],
            pagination: response?.data.pagination ?? null
        };

    } catch (error) {
        if (axios.isCancel(error)) {
            throw error;
        }
        console.error("Fetch posts error:", error);
        throw error;
    }
};

export const getPost = async (id, options = {}) => {
    try {
        const response = await api.get(`/posts/${id}`, options);
        if (response.data && response.data.data) {
            return response.data.data;
        }
        return response.data;
    } catch (error) {
        if (axios.isCancel(error)) {
            throw error;
        }
        console.error("Get post error:", error);
        throw error;
    }
};

export const createPost = async (title, category, content, isPinned = false, fileIdList = []) => {
    try {
        const response = await api.post('/posts', { title, category, content, isPinned, fileIdList });
        return response.data;
    } catch (error) {
        console.error("Create post error:", error);
        throw error;
    }
};

export const updatePost = async (id, title, category, content, isPinned = false, fileIdList = []) => {
    try {
        const response = await api.put(`/posts/${id}`, { title, category, content, isPinned, fileIdList });
        return response.data;
    } catch (error) {
        console.error("Update post error:", error);
        throw error;
    }
};

export const deletePost = async (id) => {
    try {
        const response = await api.delete(`/posts/${id}`);
        return response.data;
    } catch (error) {
        console.error("Delete post error:", error);
        throw error;
    }
};

// ===== File/Image APIs (파일 및 이미지 통합 처리) =====

export const uploadFile = async (file, postId) => {
    try {
        const formData = new FormData();
        formData.append('file', file);
        const url = (postId !== null && postId !== undefined && !isNaN(postId))
            ? `/files/upload?postId=${postId}`
            : '/files/upload';
        const response = await api.post(url, formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
        return response.data;
    } catch (error) {
        console.error("Upload file error:", error);
        throw error;
    }
};

export const uploadFiles = async (files, postId) => {
    try {
        const formData = new FormData();
        files.forEach((file) => {
            formData.append('files', file);
        });
        const url = (postId !== null && postId !== undefined && !isNaN(postId))
            ? `/files/upload?postId=${postId}`
            : '/files/upload';
        const response = await api.post(url, formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
        return response.data;
    } catch (error) {
        console.error("Upload files error:", error);
        throw error;
    }
};

export const getFiles = async (postId) => {
    try {
        const response = await api.get(`/files/${postId}`);
        return response.data;
    } catch (error) {
        console.error("Get files error:", error);
        throw error;
    }
};

export const updateFile = async (file, fileId) => {
    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await api.put(`/files/${fileId}`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
        return response.data;
    } catch (error) {
        console.error("Update file error:", error);
        throw error;
    }
};

export const deleteFile = async (fileId) => {
    try {
        const response = await api.delete(`/files/${fileId}`);
        return response.data;
    } catch (error) {
        console.error("Delete file error:", error);
        throw error;
    }
};

export const deleteBatchFiles = async (fileIds) => {
    try {
        const response = await api.post('/files/delete-batch', fileIds);
        return response.data;
    } catch (error) {
        console.error("Delete batch files error:", error);
        throw error;
    }
};

// 하위 호환성 및 명시적 이미지 처리를 위한 Alias
export const uploadImage = uploadFile;
export const getImages = getFiles;
export const updateImage = updateFile;
export const deleteImage = deleteFile;
export const deleteBatchImages = deleteBatchFiles;

// ===== Comment APIs (백엔드 실제 연동) =====

export const getComments = async (postId, page = 1, limit = 10, options = {}) => {
    try {
        const response = await api.get(`/comments/${postId}?page=${page}&limit=${limit}`, options);
        return {
            data: response?.data.data ?? [],
            pagination: response?.data.pagination ?? null
        };
    } catch (error) {
        if (axios.isCancel(error)) {
            throw error;
        }
        console.error("Get comments error:", error);
        throw error;
    }
};

export const createComment = async (postId, content, parentId) => {
    try {
        const response = await api.post(`/comments/${postId}`, { content, parentId });
        return response.data;
    } catch (error) {
        console.error("Create comment error:", error);
        throw error;
    }
};

export const updateComment = async (postId, commentId, content, parentId) => {
    try {
        const response = await api.put(`/comments/${postId}/${commentId}`, { content, parentId });
        return response.data;
    } catch (error) {
        console.error("Update comment error:", error);
        throw error;
    }
};

export const deleteComment = async (postId, commentId) => {
    try {
        const response = await api.delete(`/comments/${postId}/${commentId}`);
        return response.data;
    } catch (error) {
        console.error("Delete comment error:", error);
        throw error;
    }
};
