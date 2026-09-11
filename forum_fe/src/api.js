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

// 요청 시 JWT 토큰을 자동으로 헤더에 추가하는 인터셉터
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // 401 에러이고 재발급 요청 자체가 아닌 경우
        if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url?.includes('/auth/reissue')) {
            if (isRefreshing) {
                // 다른 요청이 이미 재발급을 진행 중이면 대기 큐에 저장
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                })
                    .then((token) => {
                        originalRequest.headers.Authorization = `Bearer ${token}`;
                        return api(originalRequest);
                    })
                    .catch((err) => Promise.reject(err));
            }

            originalRequest._retry = true;
            isRefreshing = true;

            try {
                const res = await axios.post(`${API_BASE_URL}/auth/reissue`, {}, { withCredentials: true });
                if (res.data?.success && res.data?.accessToken) {
                    const newAccessToken = res.data.accessToken;
                    localStorage.setItem('accessToken', newAccessToken);
                    api.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;
                    originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

                    processQueue(null, newAccessToken);
                    return api(originalRequest);
                } else {
                    throw new Error('토큰 재발급 응답이 올바르지 않습니다.');
                }
            } catch (reissueError) {
                processQueue(reissueError, null);
                localStorage.removeItem('accessToken');
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
        console.error("Login error:", error);
        throw error;
    }
}

export const reissueToken = async () => {
    const response = await api.post('/auth/reissue');
    return response.data;
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

export const fetchPosts = async (page = 1, limit = 20, sort = "latest", category = "all", options = {}) => {
    try {
        const response = await api.get(`/posts?page=${page}&limit=${limit}&sort=${sort}&category=${category}`, options);
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

export const createPost = async (title, category, content, isPinned = false) => {
    try {
        const response = await api.post('/posts', { title, category, content, isPinned });
        return response.data;
    } catch (error) {
        console.error("Create post error:", error);
        throw error;
    }
};

export const updatePost = async (id, title, category, content, isPinned = false) => {
    try {
        const response = await api.put(`/posts/${id}`, { title, category, content, isPinned });
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

export const uploadImage = async (file, postId) => {
    try {
        const formData = new FormData();
        formData.append('file', file);
        const url = (postId !== null && postId !== undefined && !isNaN(postId))
            ? `/images/upload?postId=${postId}`
            : '/images/upload';
        const response = await api.post(url, formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
        return response.data;
    } catch (error) {
        console.error("Upload image error:", error);
        throw error;
    }
};

export const getImages = async (imageId) => {
    try {
        const response = await api.get(`/images/${imageId}`);
        return response.data;
    } catch (error) {
        console.error("Get image error:", error);
        throw error;
    }
};

export const updateImage = async (file, imageId) => {
    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await api.put(`/images/${imageId}`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
        return response.data;
    } catch (error) {
        console.error("Update image error:", error);
        throw error;
    }
};

export const deleteImage = async (imageId) => {
    try {
        const response = await api.delete(`/images/${imageId}`);
        return response.data;
    } catch (error) {
        console.error("Delete image error:", error);
        throw error;
    }
};

export const deleteBatchImages = async (fileIds) => {
    try {
        const response = await api.post('/images/delete-batch', fileIds);
        return response.data;
    } catch (error) {
        console.error("Delete batch images error:", error);
        throw error;
    }
};

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
