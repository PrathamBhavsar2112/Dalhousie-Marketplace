import React, { useState } from 'react';
import '../css/login.css';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { FaEye, FaEyeSlash } from "react-icons/fa";
import { BASE_URL } from "../constant_url";

axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

const Login = () => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [emailError, setEmailError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [showForgotPasswordPopup, setShowForgotPasswordPopup] = useState(false);
  const [resetEmail, setResetEmail] = useState('');
  const [resetMessage, setResetMessage] = useState('');
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();


  const handleSubmit = async (e) => {
    e.preventDefault();
    setEmailError('');
    setPasswordError('');
    setLoading(true);

    if (!email.endsWith('@dal.ca')) {
      setEmailError('NetID is required');
      setLoading(false);
      return;
    }

    if (password.length < 8) {
      setPasswordError('Invalid Credentials');
      setLoading(false);
      return;
    }

    try {
      localStorage.clear();

      const response = await axios.post(
        `${BASE_URL}/api/auth/login`,
        { email, passwordHash: password },
        { headers: { "Content-Type": "application/json" } }
      );

      //setMessage(response.data.message);
      setMessage(response.data.message);
      if (response.data.token && response.data.userId) {
        localStorage.setItem("token", response.data.token);
        localStorage.setItem("userId", response.data.userId);
        localStorage.setItem("username", response.data.username);

        const userResponse = await axios.get(`${BASE_URL}/api/auth/user/${response.data.userId}`, {
          headers: { Authorization: `Bearer ${response.data.token}` },
        });

        const userData = userResponse.data;

        if (!userData || userData.deleted) {
          setMessage("Your account does not exist or has been deleted. Please sign up again.");
          setLoading(false);
          localStorage.clear();
          navigate("/signup");
          return;
        }

        localStorage.setItem("user", JSON.stringify(userData));

        navigate("/dashboard");
      } else {
        setPasswordError("Login failed. Please try again.");
        setLoading(false);
      }
    } catch (error) {
      if (error.response?.status === 401) {
        setMessage(error.response.data.message);
        localStorage.clear();
        setLoading(false);
        navigate("/login");
      } else {
        setPasswordError(error.response?.data?.message || "Login failed. Please try again.");
        setLoading(false);
      }
    }
  };

  const handleForgotPassword = async () => {
    if (!resetEmail.endsWith('@dal.ca')) {
      setResetMessage('Please enter a valid Dalhousie NetID.');
      return;
    }

    try {
      const response = await axios.post(`${BASE_URL}/api/auth/forgot-password?email=${resetEmail}`);

      if (response.status === 200) {
        setResetMessage('A reset link has been sent to your email.');
      } else {
        setResetMessage('Error sending reset link. Please try again.');
      }
    } catch (error) {
      setResetMessage('Error sending reset link. Please try again.');
    }
  };

  return (
    <div className="App">
      {showForgotPasswordPopup && (
        <div className="forgot-password-popup">
          <div className="popup-content">
            <h4>Reset Your Password</h4>
            <input
              type="email"
              placeholder="Enter your NetID"
              value={resetEmail}
              onChange={(e) => setResetEmail(e.target.value)}
            />
            <div className="popup-buttons">
              <button onClick={handleForgotPassword} className="send-reset-btn">
                Send Reset Link
              </button>
              <button
                className="close-btn"
                onClick={() => setShowForgotPasswordPopup(false)}
              >
                Close
              </button>
            </div>
            {resetMessage && <div className="reset-message">{resetMessage}</div>}
          </div>
        </div>
      )}

      <div className="login-container">
        <h1 className="welcome-header">Welcome to Dalhousie Marketplace</h1>
        <div className="addUser">
          <h3>Log In</h3>
          <form className="addUserForm" onSubmit={handleSubmit}>
            <div className="inputGroup">
              <label htmlFor="email">NetID: </label>
              <input
                type="email"
                id="email"
                autoComplete="off"
                placeholder="Enter your NetID"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
              {emailError && <div className="login-error-message">{emailError}</div>}

              <label htmlFor="password">Password: </label>
              <div className="login-password-container">
                <input
                  type={showPassword ? "text" : "password"}
                  id="password"
                  autoComplete="off"
                  placeholder="Enter your Password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
                {showPassword ? (
                  <FaEyeSlash className="login-password-icon" onClick={() => setShowPassword(false)} />
                ) : (
                  <FaEye className="login-password-icon" onClick={() => setShowPassword(true)} />
                )}
              </div>
              {passwordError && <div className="login-error-message">{passwordError}</div>}

              <Link
                onClick={() => setShowForgotPasswordPopup(true)}
                className="forgot-password-link"
              >
                Forgot Password?
              </Link>

              <button type="submit" className="btn btn-warning"  disabled={loading}>{loading ? "Logging in..." : "Login"}</button>
            </div>
            {message && (
              <div style={{
                color: message.includes("Please") ? "red" : "green",
                textAlign: "center"
              }}>
                {message}
              </div>
            )}
          </form>

          <div className="signup-link">
            <p>Don't have an account yet? <Link to="/signup">Sign up</Link></p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;
