import "./SignUpPage.css";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { registerUser } from "../../api";

const regexPwd = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[+=%_!@#$^&*?]).{8,}$/;

async function handleRegistration(nickname, id, pwd, valid, navigate) {
    if (valid) {
        try {
            const result = await registerUser(id, pwd, nickname);
            if (result.success) {
                alert("회원가입이 완료되었습니다!");
                navigate("/sign-in");
            } else {
                alert(result.message || "회원가입에 실패했습니다.");
            }
        } catch (error) {
            alert("회원가입에 실패했습니다.");
        }
    }
}

function SignUpPage() {
    const navigate = useNavigate();
    const [nickname, setNickName] = useState("");
    const [id, setId] = useState("");
    const [pwd, setPwd] = useState("");

    const validNickname = nickname.trim().length > 0;
    const validId = id.trim().length >= 4 && id.trim().length <= 16;
    const validPwd = regexPwd.test(pwd);
    const validation = validId && validNickname && validPwd;

    const handleEnter = (event) => {
        if (event.key === 'Enter') {
            handleRegistration(nickname, id, pwd, validation, navigate);
        }
    }

    return (
        <div className="SignUp">
            <div className="SignUpWrapper">
                <div className="SignUpInputWrapper">
                    <div className="Id">
                        <label className="InputLabel">아이디</label>
                        <input type="text" placeholder="최소 4자 최대 16자" onChange={(e) => setId(e.target.value)} onKeyDown={handleEnter}
                        />
                        <div className="Validation">
                            {
                                (id === "" || validId) ? <p></p> : <p className="Message">아이디는 최소 4자 최대 16자까지 입니다.</p>
                            }
                        </div>
                    </div>
                    <div className="Password">
                        <label className="InputLabel">비밀번호</label>
                        <input type="password" placeholder="영문숫자 및 특수문자(+=%_!@#$^&*?)포함 8자 이상" onChange={(e) => setPwd(e.target.value)} onKeyDown={handleEnter}
                        />
                        <div className="Validation">
                            {
                                (pwd === "" || validPwd) ? <p></p> : <p className="Message">영문숫자 및 특수문자(+=%_!@#$^&*?)포함 8자 이상</p>
                            }
                        </div>
                    </div>
                    <div className="Nickname">
                        <label className="InputLabel">닉네임</label>
                        <input type="text" placeholder="닉네임" onChange={(e) => setNickName(e.target.value)} onKeyDown={handleEnter}
                        />
                        <div className="Validation">
                            {
                                (nickname === "" || validNickname) ? <p></p> : <p className="Message">닉네임을 입력해주세요</p>
                            }
                        </div>
                    </div>
                </div>
                <button className="SignUpButton" disabled={!validation}
                    onClick={() => handleRegistration(nickname, id, pwd, validation, navigate)}
                >
                    회원가입
                </button>
            </div>
        </div>
    );
}

export default SignUpPage;