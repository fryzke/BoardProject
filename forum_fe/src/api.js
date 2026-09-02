import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
});

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
        // 401 에러이고 아직 재시도하지 않은 요청인 경우
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            try {
                const res = await api.post('/auth/reissue');
                if (res.data?.success && res.data?.accessToken) {
                    const newAccessToken = res.data.accessToken;
                    localStorage.setItem('accessToken', newAccessToken);
                    api.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;
                    originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                    return api(originalRequest); // api 인스턴스로 재요청
                }
            } catch (reissueError) {
                // Refresh Token도 만료되었거나 재발급 실패 시
                localStorage.removeItem('accessToken');
                localStorage.removeItem('userId');
                localStorage.removeItem('userName');
                alert('세션이 만료되었습니다. 다시 로그인해주세요.');
                window.location.replace('/sign-in');
                return Promise.reject(reissueError);
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

export const createPost = async (title, category, content) => {
    try {
        const response = await api.post('/posts', { title, category, content });
        return response.data;
    } catch (error) {
        console.error("Create post error:", error);
        throw error;
    }
};

export const updatePost = async (id, title, category, content) => {
    try {
        const response = await api.put(`/posts/${id}`, { title, category, content });
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

// ===== Comment APIs (백엔드 실제 연동) =====

export const getComments = async (id) => {
    try {
        const response = await api.get(`/comments/${id}`);
        if (response.data && Array.isArray(response.data.data)) {
            return response.data.data;
        }
        if (Array.isArray(response.data)) {
            return response.data;
        }
        return [];
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
