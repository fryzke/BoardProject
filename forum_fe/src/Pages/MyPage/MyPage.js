import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, FileText, MessageSquare, Shield, Award, Calendar, LogOut, ArrowLeft, Save } from 'lucide-react';
import { getUserInfo, updateUserInfo, logoutUser } from '../../api';
import { formatDate } from '../../utils';
import { AuthValidation, Role } from '../../enum';
import { useToast } from '../../Components/Toast/ToastContext';
import { useModal } from '../../Components/Modal/ModalContext';
import './MyPage.css';

export default function MyPage() {
    const navigate = useNavigate();
    const toast = useToast();
    const { confirm } = useModal();

    const [userInfo, setUserInfo] = useState(null);
    const [loading, setLoading] = useState(true);

    // 수정 폼 상태
    const [userName, setUserName] = useState('');
    const [password, setPassword] = useState('');
    const [passwordConfirm, setPasswordConfirm] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        const fetchUserData = async () => {
            try {
                const res = await getUserInfo();
                if (res.data) {
                    setUserInfo(res.data);
                    setUserName(res.data.userName || '');
                }
            } catch (error) {
                toast.error('회원 정보를 불러오지 못했습니다.');
                navigate('/');
            } finally {
                setLoading(false);
            }
        };

        fetchUserData();
    }, [navigate, toast]);

    // 유효성 검사
    const isNameValid = userName.trim().length >= AuthValidation.MIN_NAME_LENGTH && userName.trim().length <= AuthValidation.MAX_NAME_LENGTH;
    const isPasswordValid = !password || AuthValidation.PASSWORD_REGEX.test(password);
    const isPasswordMatch = !password || (password === passwordConfirm);

    const handleUpdate = async (e) => {
        e.preventDefault();

        if (!isNameValid) {
            toast.warning(`닉네임은 ${AuthValidation.MIN_NAME_LENGTH}자 이상 ${AuthValidation.MAX_NAME_LENGTH}자 이하로 입력해주세요.`);
            return;
        }

        if (password) {
            if (!AuthValidation.PASSWORD_REGEX.test(password)) {
                toast.warning('비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.');
                return;
            }
            if (password !== passwordConfirm) {
                toast.warning('비밀번호 확인이 일치하지 않습니다.');
                return;
            }
        }

        setIsSubmitting(true);
        try {
            const updatePayload = {
                userName: userName.trim(),
            };
            if (password.trim()) {
                updatePayload.userPassword = password.trim();
            }

            const res = await updateUserInfo(updatePayload);
            if (res.success) {
                toast.success('회원 정보가 성공적으로 수정되었습니다.');
                localStorage.setItem('userName', userName.trim());
                setPassword('');
                setPasswordConfirm('');
                // 최신 정보 갱신
                const refreshed = await getUserInfo();
                if (refreshed.data) setUserInfo(refreshed.data);
            } else {
                toast.error(res.message || '정보 수정에 실패했습니다.');
            }
        } catch (error) {
            const msg = error.response?.data?.message || '정보 수정 중 오류가 발생했습니다.';
            toast.error(msg);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleLogout = async () => {
        const isConfirmed = await confirm({
            title: '로그아웃',
            message: '로그아웃 하시겠습니까?',
            confirmText: '로그아웃',
            cancelText: '취소',
        });

        if (isConfirmed) {
            try {
                localStorage.removeItem('accessToken');
                localStorage.removeItem('userId');
                localStorage.removeItem('userName');
                localStorage.removeItem('userGrade');
                localStorage.removeItem('userRole');
                await logoutUser();
                toast.success('로그아웃 되었습니다.');
                navigate('/');
            } catch (err) {
                navigate('/');
            }
        }
    };

    if (loading) {
        return <div className="MyPageWrapper">회원 정보를 불러오는 중...</div>;
    }

    if (!userInfo) return null;

    return (
        <div className="MyPageContainer">
            <div className="MyPageHeader">
                <button className="MyPageBackBtn" onClick={() => navigate('/')}>
                    <ArrowLeft size={18} />
                    <span>홈으로</span>
                </button>
                <h1 className="MyPageTitle">마이페이지</h1>
                <div style={{ width: '70px' }}></div>
            </div>

            {/* 상단 프로필 요약 카드 */}
            <div className="ProfileCard">
                <div className="ProfileAvatarWrapper">
                    <User size={38} className="ProfileAvatarIcon" />
                </div>
                <div className="ProfileInfo">
                    <div className="ProfileNameRow">
                        <span className="ProfileName">{userInfo.userName || userInfo.userId}</span>
                        <span className="ProfileId">(@{userInfo.userId})</span>
                        {userInfo.role === Role.ADMIN && (
                            <span className="Badge Badge--admin">
                                <Shield size={12} />
                                <span>관리자</span>
                            </span>
                        )}
                        <span className="Badge Badge--grade">
                            <Award size={12} />
                            <span>{userInfo.grade || '일반회원'}</span>
                        </span>
                    </div>
                    <div className="ProfileMetaRow">
                        <span className="ProfileMetaItem">
                            <Calendar size={14} />
                            <span>가입일: {formatDate(userInfo.createdAt)}</span>
                        </span>
                    </div>
                </div>
            </div>

            {/* 내 활동 통계 */}
            <div className="StatsGrid">
                <div className="StatCard">
                    <div className="StatIconWrapper StatIconWrapper--post">
                        <FileText size={20} />
                    </div>
                    <div className="StatContent">
                        <span className="StatLabel">내가 쓴 게시글</span>
                        <span className="StatValue">{userInfo.postCount ?? 0}개</span>
                    </div>
                </div>
                <div className="StatCard">
                    <div className="StatIconWrapper StatIconWrapper--comment">
                        <MessageSquare size={20} />
                    </div>
                    <div className="StatContent">
                        <span className="StatLabel">내가 쓴 댓글</span>
                        <span className="StatValue">{userInfo.commentCount ?? 0}개</span>
                    </div>
                </div>
            </div>

            {/* 회원 정보 수정 폼 */}
            <div className="EditSectionCard">
                <h2 className="SectionTitle">회원 정보 수정</h2>
                <form className="EditForm" onSubmit={handleUpdate}>
                    <div className="FormGroup">
                        <label className="FormLabel" htmlFor="mypage-username">닉네임</label>
                        <input
                            id="mypage-username"
                            type="text"
                            className="FormInput"
                            value={userName}
                            onChange={(e) => setUserName(e.target.value)}
                            placeholder="닉네임을 입력하세요 (2~20자)"
                            maxLength={20}
                        />
                        {!isNameValid && userName.length > 0 && (
                            <span className="FormHint FormHint--error">닉네임은 2자 이상 20자 이하로 입력해주세요.</span>
                        )}
                    </div>

                    <div className="FormGroup">
                        <label className="FormLabel" htmlFor="mypage-password">새 비밀번호</label>
                        <input
                            id="mypage-password"
                            type="password"
                            className="FormInput"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="변경할 새 비밀번호 (변경 시에만 입력)"
                        />
                        {password && !isPasswordValid && (
                            <span className="FormHint FormHint--error">영문, 숫자, 특수문자(+=%_!@#$^&*?) 포함 8자 이상</span>
                        )}
                    </div>

                    {password.length > 0 && (
                        <div className="FormGroup">
                            <label className="FormLabel" htmlFor="mypage-password-confirm">새 비밀번호 확인</label>
                            <input
                                id="mypage-password-confirm"
                                type="password"
                                className="FormInput"
                                value={passwordConfirm}
                                onChange={(e) => setPasswordConfirm(e.target.value)}
                                placeholder="비밀번호를 한 번 더 입력해주세요"
                            />
                            {!isPasswordMatch && passwordConfirm.length > 0 && (
                                <span className="FormHint FormHint--error">비밀번호가 일치하지 않습니다.</span>
                            )}
                        </div>
                    )}

                    <div className="FormActions">
                        <button
                            type="submit"
                            className="SaveButton"
                            disabled={isSubmitting || !isNameValid || (!isPasswordValid && Boolean(password)) || (!isPasswordMatch && Boolean(password))}
                        >
                            <Save size={16} />
                            <span>{isSubmitting ? '저장 중...' : '변경사항 저장'}</span>
                        </button>
                    </div>
                </form>
            </div>

            {/* 하단 로그아웃 영역 */}
            <div className="MyPageFooter">
                <button type="button" className="LogoutButtonSecondary" onClick={handleLogout}>
                    <LogOut size={16} />
                    <span>로그아웃</span>
                </button>
            </div>
        </div>
    );
}
