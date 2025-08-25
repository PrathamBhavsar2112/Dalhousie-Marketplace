import React, { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import axios from "axios";
import { FaEye, FaEyeSlash } from "react-icons/fa"; 
import "../css/resetPassword.css";
import { BASE_URL } from "../constant_url";

const ResetPassword = () => {
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [message, setMessage] = useState("");
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const token = searchParams.get("token");

  useEffect(() => {
    if (token) {
      localStorage.setItem("resetToken", token); 
    }
  }, [token]);

  const handleResetPassword = async (e) => {
    e.preventDefault();
    setMessage("");

    if (newPassword.length < 8) {
      setMessage("Password must be at least 8 characters long.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setMessage("Passwords do not match.");
      return;
    }

    try {
      const storedToken = localStorage.getItem("resetToken");

      const response = await axios.post(
        BASE_URL+"/api/auth/reset-password",
        {}, 
        {
          params: {
            token: storedToken, 
            newPassword, 
          },
          headers: { "Content-Type": "application/json" },
        }
      );

      if (response.status === 200) {
        setMessage("Password successfully reset! Redirecting to login...");
        localStorage.removeItem("resetToken");

        localStorage.removeItem("forgotPasswordPopup");

        setTimeout(() => {
          navigate("/login", { replace: true });  
          window.location.reload(); 
        }, 2000);
      } else {
        setMessage(response.data || "Something went wrong.");
      }
    } catch (error) {
      setMessage(error.response?.data?.message || "Invalid or expired reset token.");
    }
  };

  return (
    <div className="reset-password-container">
      <h2>Reset Your Password</h2>
      {message.includes("successfully reset") ? (
        <p className="success-message">{message}</p>
      ) : (
        <form onSubmit={handleResetPassword}>
          <div className="input-group1">
            <label htmlFor="newPassword">New Password:</label>
            <div className="password-container">
              <input
                type={showNewPassword ? "text" : "password"}
                id="newPassword"
                placeholder="Enter new password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
              />
              <button
                type="button"
                className="toggle-btn"
                onClick={() => setShowNewPassword(!showNewPassword)}
              >
                {showNewPassword ? <FaEyeSlash /> : <FaEye />}
              </button>
            </div>
          </div>

          <div className="input-group1">
            <label htmlFor="confirmPassword">Confirm Password:</label>
            <div className="password-container">
              <input
                type={showConfirmPassword ? "text" : "password"}
                id="confirmPassword"
                placeholder="Confirm new password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
              <button
                type="button"
                className="toggle-btn"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
              >
                {showConfirmPassword ? <FaEyeSlash /> : <FaEye />}
              </button>
            </div>
          </div>

          {message && <p className="message">{message}</p>}

          <button type="submit" className="reset-btn">Reset Password</button>
        </form>
      )}
    </div>
  );
};

export default ResetPassword;
