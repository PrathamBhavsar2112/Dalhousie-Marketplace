import React, { useState } from "react";
import axios from "axios";
import "../css/signup.css";
import "../css/otp.css";
import { Link, useNavigate } from "react-router-dom";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import { BASE_URL } from "../constant_url";

const Signup = () => {
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [bannerID, setBannerID] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [showOTPModal, setShowOTPModal] = useState(false);
  const [otp, setOtp] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [OTPmessage, setOTPMessage] = useState("");
  const navigate = useNavigate();

  const handleSignup = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    if (!fullName.trim()) {
      setError("Full Name is required.");
      setLoading(false);
      return;
    }

    const bannerIDRegex = /^B\d{8}$/;
    if (!bannerIDRegex.test(bannerID)) {
      setError("Banner ID must start with 'B' followed by 8 digits.");
      setLoading(false);
      return;
    }

    if (!email.endsWith("@dal.ca")) {
      setError("Please enter a valid NetID address (@dal.ca).");
      setLoading(false);
      return;
    }

    const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@])[A-Za-z\d@]{8,}$/;
    if (!passwordRegex.test(password)) {
      setError(
        "Password must be at least 8 characters long, include at least one letter, one number, and one special character (@)."
      );
      setLoading(false);
      return;
    }

    const signupData = {
      email,
      passwordHash: password,
      username: fullName,
      bannerId: bannerID,
    };

    try {
      const response = await axios.post(
        BASE_URL + "/api/auth/register",
        signupData,
        {
          headers: {
            "Content-Type": "application/json",
          },
        }
      );

      if (response.status === 201) {
        setShowOTPModal(true);
        setMessage("Signup successful, awaiting email verification.");
      }
    } catch (error) {
      if (error.response) {
        console.error("Signup Error:", error.response.data);
        if (error.response.status === 409) {
          setError("Email is already in use.");
        } else {
          setError(error.response.data.message || "BannerID or Email is already in use.");
        }
      } else {
        setError("Connection error. Please try again later.");
      }
    } finally {
      setLoading(false);
    }
  };


  const handleVerifyOTP = async (e) => {
    e.preventDefault();

    if (!otp.trim()) {
      setOTPMessage("Please enter the OTP.");
      return;
    }
    setLoading(true);
    try {
      const response = await axios.post(BASE_URL + "/api/auth/verify-otp", {
        email,
        otp,
      });

      if (response.status === 200) {
        setOTPMessage("Email Verified Successfully. Signup complete!");
        setShowOTPModal(false);
        navigate("/login");
      }
    } catch (error) {
      if (error.response?.status === 400) {
        setOTPMessage(error.response.data || "Invalid OTP. Please try again.");
      } else if (error.response?.status === 404) {
        setOTPMessage("User or OTP not found. Please re-check.");
      } else {
        setOTPMessage("Error during OTP verification. Please try again.");
        console.error("OTP Verification Error:", error);
      }

    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="signup-App">
      <h1 className="signup-welcome-header">Welcome to Dalhousie Marketplace</h1>
      <div className="signup-container">
        <div className="signup-addUser">
          <h3>Sign Up</h3>
          <form className="signup-addUserForm" onSubmit={handleSignup}>
            <div className="signup-inputGroup">
              <label htmlFor="fullName">Full Name: </label>
              <input
                type="text"
                id="fullName"
                autoComplete="off"
                placeholder="Enter your Full Name"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
              />
              {error.includes("Full Name") && (<div className="signup-error-message">{error}</div>)}

              <label htmlFor="bannerID">Banner ID: </label>
              <input
                type="text"
                id="bannerID"
                autoComplete="off"
                placeholder="Enter your Banner ID"
                value={bannerID}
                onChange={(e) => setBannerID(e.target.value)}
              />
              {error.includes("Banner ID") && <div className="signup-error-message">{error}</div>}

              <label htmlFor="email">NetID: </label>
              <input
                type="email"
                id="email"
                autoComplete="off"
                placeholder="Enter your NetID"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
              {error.includes("NetID") && <div className="signup-error-message">{error}</div>}

              <label htmlFor="password">Password: </label>
              <div className="signup-password-container">
                <input
                  type={showPassword ? "text" : "password"}
                  id="password"
                  autoComplete="off"
                  placeholder="Enter your Password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                {showPassword ? (
                  <FaEyeSlash className="signup-password-icon" onClick={() => setShowPassword(false)} />
                ) : (
                  <FaEye className="signup-password-icon" onClick={() => setShowPassword(true)} />
                )}
              </div>
              {error.includes("Password") && <div className="signup-error-message">{error}</div>}
              {error && !error.includes("Full Name") && !error.includes("Banner ID") && !error.includes("NetID") && !error.includes("Password") && (
                <div className="signup-error-message">{error}</div>
              )}
              <button type="submit" className="btn btn-warning" disabled={loading}>
                {loading ? "Signing Up..." : "Sign Up"}
              </button>
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

          <div className="signup-login">
            <p>Already have an account?<Link to="/login">Login</Link></p>
          </div>
        </div>
      </div>

      {showOTPModal && (
        <div className="otp-modal">
          <div className="otp-box">
            <h2>Verify OTP</h2>
            <p>Enter the OTP sent to your email address.</p>
            {OTPmessage && (
              <div style={{
                color: OTPmessage.includes("Please") ? "red" : "green",
                textAlign: "center"
              }}>
                {OTPmessage}
              </div>
            )}
            <form onSubmit={handleVerifyOTP}>
              <input
                type="text"
                placeholder="Enter OTP"
                value={otp}
                onChange={(e) => setOtp(e.target.value)}
                maxLength={6}
              />
              <button type="submit" className="btn btn-success"disabled={loading}>
              {loading ? "verfying..." : "Verify"}
              </button>
            </form>
            <button
              className="btn btn-secondary"
              onClick={() => setShowOTPModal(false)}
            >
              Cancel
            </button>

          </div>
        </div>
      )}
    </div>
  );
};

export default Signup;
