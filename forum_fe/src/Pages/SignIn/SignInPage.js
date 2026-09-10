import "./SignInPage.css";
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { loginUser } from "../../api";

async function handleLogin(id, pwd, valid, navigate, setFail) {

    if (valid) {
        try {
            const result = await loginUser(id, pwd);
            if (result.success) {
                localStorage.setItem("accessToken", result.accessToken)
                localStorage.setItem("userId", id);
                localStorage.setItem("userRole", result.userRole);
                localStorage.setItem("userGrade", result.userGrade)
                if (result.userName) {
                    localStorage.setItem("userName", result.userName);
                }
                navigate("/");
            } else {
                setFail(true);
            }
        } catch (error) {
            setFail(true);
        }
    }
}

function SignInPage() {
    const navigate = useNavigate();
    const [id, setId] = useState("");
    const [pwd, setPwd] = useState("");
    const [fail, setFail] = useState(false);

    // 아이디와 비밀번호가 모두 공백이 아닐 때만 유효(활성화)
    const isFormValid = id.trim().length > 0 && pwd.trim().length > 0;

    useEffect(() => {
        const isLoggedIn = !!localStorage.getItem("accessToken");
        if (isLoggedIn) {
            navigate("/");
        }
    }, [navigate]);

    const handleIdChange = (e) => {
        setId(e.target.value);
        if (fail) setFail(false);
    };

    const handlePwdChange = (e) => {
        setPwd(e.target.value);
        if (fail) setFail(false);
    };

    const handleEnter = (event) => {
        if (event.key === 'Enter' && isFormValid) {
            handleLogin(id.trim(), pwd, isFormValid, navigate, setFail);
        }
    };

    return (
        <div className="SignIn">
            <div className="SignInWrapper">
                <div className="SignInInputWrapper">
                    <div className="Id">
                        <input
                            type="text"
                            placeholder="아이디"
                            value={id}
                            onChange={handleIdChange}
                            onKeyDown={handleEnter}
                        />
                    </div>
                    <div className="Password">
                        <input
                            type="password"
                            placeholder="비밀번호"
                            value={pwd}
                            onChange={handlePwdChange}
                            onKeyDown={handleEnter}
                        />
                    </div>
                    <div className="Validation">
                        {
                            fail ? <p className="Message">아이디 혹은 비밀번호가 틀립니다.</p> : <p></p>
                        }
                    </div>
                </div>
                <button
                    className="SignInButton"
                    disabled={!isFormValid}
                    onClick={() => handleLogin(id.trim(), pwd, isFormValid, navigate, setFail)}
                >
                    로그인
                </button>
                <button
                    type="button"
                    className="SignUpButtonSecondary"
                    onClick={() => navigate('/sign-up')}
                >
                    회원가입
                </button>
            </div>
        </div>
    );
}

export default SignInPage;