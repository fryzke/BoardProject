import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { reissueToken, getAccessToken, setAccessToken } from './api';

const parseJwt = (token) => {
    try {
        return JSON.parse(atob(token.split('.')[1]));
    } catch (e) {
        return null;
    }
};

export default function ProtectedRoute({ children }) {
    const [loading, setLoading] = useState(true);
    const [isAuthenticated, setIsAuthenticated] = useState(false);

    useEffect(() => {
        const checkAuth = async () => {
            const token = getAccessToken();
            if (token) {
                const payload = parseJwt(token);
                if (payload && payload.exp && payload.exp * 1000 > Date.now()) {
                    setIsAuthenticated(true);
                    setLoading(false);
                    return;
                }
            }

            // 1. 메모리에 유효한 토큰이 없거나 만료된 경우: Refresh Token 쿠키로 자동 재발급 시도
            try {
                const res = await reissueToken();
                if (res?.success && res?.accessToken) {
                    setIsAuthenticated(true);
                } else {
                    setAccessToken(null);
                    localStorage.removeItem('userId');
                    localStorage.removeItem('userName');
                    localStorage.removeItem('userRole');
                    localStorage.removeItem('userGrade');
                    setIsAuthenticated(false);
                }
            } catch (e) {
                setAccessToken(null);
                localStorage.removeItem('userId');
                localStorage.removeItem('userName');
                localStorage.removeItem('userRole');
                localStorage.removeItem('userGrade');
                setIsAuthenticated(false);
            } finally {
                setLoading(false);
            }
        };

        checkAuth();
    }, []);

    if (loading) {
        return <div style={{ textAlign: 'center', padding: '60px', color: '#666' }}>인증 상태 확인 중...</div>;
    }

    if (!isAuthenticated) {
        return <Navigate to="/sign-in" replace />;
    }

    return children;
}