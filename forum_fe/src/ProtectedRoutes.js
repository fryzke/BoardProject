import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { getUserInfo } from './api';

export default function ProtectedRoute({ children }) {
    const [loading, setLoading] = useState(true);
    const [isAuthenticated, setIsAuthenticated] = useState(false);

    useEffect(() => {
        const checkAuth = async () => {
            const userId = localStorage.getItem('userId');
            if (!userId) {
                setIsAuthenticated(false);
                setLoading(false);
                return;
            }

            try {
                const res = await getUserInfo();
                if (res?.success) {
                    setIsAuthenticated(true);
                } else {
                    localStorage.removeItem('userId');
                    localStorage.removeItem('userName');
                    localStorage.removeItem('userRole');
                    localStorage.removeItem('userGrade');
                    setIsAuthenticated(false);
                }
            } catch (e) {
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