import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { reissueToken } from './api';

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
            const token = localStorage.getItem('accessToken');
            if (!token) {
                setIsAuthenticated(false);
                setLoading(false);
                return;
            }

            const payload = parseJwt(token);
            if (!payload) {
                // 토큰 형식이 손상된 가짜 토큰인 경우
                localStorage.removeItem('accessToken');
                localStorage.removeItem('userId');
                localStorage.removeItem('userName');
                setIsAuthenticated(false);
                setLoading(false);
                return;
            }

            // 1. Access Token이 아직 만료되지 않은 경우 바로 통과
            if (payload.exp && payload.exp * 1000 > Date.now()) {
                setIsAuthenticated(true);
                setLoading(false);
                return;
            }

            // 2. Access Token이 만료된 경우: Refresh Token 쿠키로 자동 재발급 시도
            try {
                const res = await reissueToken();
                if (res?.success && res?.accessToken) {
                    localStorage.setItem('accessToken', res.accessToken);
                    setIsAuthenticated(true);
                } else {
                    localStorage.removeItem('accessToken');
                    localStorage.removeItem('userId');
                    localStorage.removeItem('userName');
                    setIsAuthenticated(false);
                }
            } catch (e) {
                localStorage.removeItem('accessToken');
                localStorage.removeItem('userId');
                localStorage.removeItem('userName');
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