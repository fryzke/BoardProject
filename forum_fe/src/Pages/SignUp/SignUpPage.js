import "./SignUpPage.css";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { registerUser } from "../../api";
import { AuthValidation } from "../../enum";
import { useToast } from "../../Components/Toast/ToastContext";

function SignUpPage() {
    const navigate = useNavigate();
    const toast = useToast();
    const [nickname, setNickName] = useState("");
    const [id, setId] = useState("");
    const [pwd, setPwd] = useState("");

    const validNickname = nickname.trim().length > 0;
    const validId = id.trim().length >= AuthValidation.MIN_ID_LENGTH && id.trim().length <= AuthValidation.MAX_ID_LENGTH;
    const validPwd = AuthValidation.PASSWORD_REGEX.test(pwd);
    const validation = validId && validNickname && validPwd;

    const handleRegistration = async () => {
        if (validation) {
            try {
                const result = await registerUser(id, pwd, nickname);
                if (result.success) {
                    toast.success("회원가입이 완료되었습니다!");
                    navigate("/sign-in");
                } else {
                    toast.error(result.message || "회원가입에 실패했습니다.");
                }
            } catch (error) {
                toast.error("회원가입에 실패했습니다.");
            }
        }
    };

    const handleEnter = (event) => {
        if (event.key === 'Enter') {
            handleRegistration();
        }
    };

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
                    onClick={handleRegistration}
                >
                    회원가입
                </button>
            </div>
        </div>
    );
}

export default SignUpPage;