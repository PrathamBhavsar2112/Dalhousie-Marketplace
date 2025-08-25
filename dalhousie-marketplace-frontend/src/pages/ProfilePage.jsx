import React, { useState, useEffect } from "react";
import axios from "axios";
import "../css/ProfilePage.css";
import { BASE_URL } from "../constant_url";

const ProfilePage = () => {
    const [user, setUser] = useState(null);
    const [showDeletePopup, setShowDeletePopup] = useState(false);
    const [showPasswordPopup, setShowPasswordPopup] = useState(false);
    const [passwords, setPasswords] = useState({
        oldPassword: "",
        newPassword: "",
        confirmPassword: "",
    });

    useEffect(() => {
        const fetchUserProfile = async () => {
            const token = localStorage.getItem("token");
            const userId = localStorage.getItem("userId");

            if (!token || !userId) {
                alert("User not authenticated. Please log in.");
                localStorage.clear();
                window.location.href = "/login";
                return;
            }

            try {
                const response = await axios.get(`${BASE_URL}/api/auth/user/${userId}`, {
                    headers: { Authorization: `Bearer ${token}` },
                });

                if (response.data) {
                    localStorage.setItem("user", JSON.stringify(response.data));
                    setUser(response.data);
                } else {
                    alert("Failed to load user data. Please log in again.");
                    localStorage.clear();
                    window.location.href = "/login";
                }
            } catch (error) {
                alert("Error fetching user details.");
                localStorage.clear();
                window.location.href = "/login";
            }
        };

        fetchUserProfile();
    }, []);

    const handleChangePassword = async () => {
        if (!passwords.oldPassword || !passwords.newPassword || !passwords.confirmPassword) {
            alert("Please fill all fields.");
            return;
        }

        if (passwords.newPassword.length < 8) {
            alert("New password must be at least 8 characters.");
            return;
        }

        if (passwords.newPassword !== passwords.confirmPassword) {
            alert("New passwords do not match.");
            return;
        }
        const token = localStorage.getItem("token");

        if (!token) {
            alert("Session expired. Please log in again.");
            localStorage.clear();
            window.location.href = "/login";
            return;
        }
        try {
            await axios.put(
                `${BASE_URL}/api/auth/change-password`,
                {
                    oldPassword: passwords.oldPassword,
                    newPassword: passwords.newPassword,
                },
                {
                    headers: { Authorization: `Bearer ${token}` },
                }
            );

            alert("Password changed successfully! Please log in again with your new password.");
            localStorage.clear();
            window.location.href = "/login";
        } catch (error) {
            console.error("Change Password Error:", error);
            alert(error.response?.data || "Failed to change password. Please try again.");
        }
    };

    const handleDelete = async () => {
        const token = localStorage.getItem("token");
        if (!token) {
            alert("Session expired. Please log in again.");
            localStorage.clear();
            window.location.href = "/login";
            return;
        }
        
        try {
            await axios.delete(`${BASE_URL}/api/auth/delete-account`, {
                headers: { Authorization: `Bearer ${token}` },
            });

            alert("Your account has been permanently deleted.");
            localStorage.clear();
            window.location.href = "/login";
        } catch (error) {
            alert("Error: " + (error.response?.data?.message || "Something went wrong!"));
        }
    };

    if (!user) return <p>Loading user details...</p>;

    return (
        <div className="user-profile-container">
            <h2>User Profile</h2>
            <form className="user-profile-form">
                <input type="text" name="fullName" value={user.fullName || ""} placeholder="Full Name" readOnly />
                <input type="email" name="email" value={user.email || ""} placeholder="Email" readOnly />
                <input type="text" name="bannerId" value={user.bannerId || ""} placeholder="Banner ID" readOnly />
            </form>

            <div className="user-buttons">
                <button onClick={() => setShowPasswordPopup(true)} className="user-change-password-btn">Change Password</button>
                <button onClick={() => setShowDeletePopup(true)} className="user-delete-btn">Delete Account</button>
            </div>

            {showPasswordPopup && (
                <div className="user-popup">
                    <div className="user-popup-content">
                        <h3>Change Password</h3>
                        <form className="user-password-form">
                            <input type="password" name="oldPassword" value={passwords.oldPassword} onChange={(e) => setPasswords({ ...passwords, oldPassword: e.target.value })} placeholder="Old Password" required />
                            <input type="password" name="newPassword" value={passwords.newPassword} onChange={(e) => setPasswords({ ...passwords, newPassword: e.target.value })} placeholder="New Password" required />
                            <input type="password" name="confirmPassword" value={passwords.confirmPassword} onChange={(e) => setPasswords({ ...passwords, confirmPassword: e.target.value })} placeholder="Confirm New Password" required />
                        </form>
                        <div className="user-popup-buttons">
                            <button onClick={handleChangePassword} className="user-password-confirm">Change Password</button>
                            <button onClick={() => setShowPasswordPopup(false)} className="user-password-cancel">Cancel</button>
                        </div>
                    </div>
                </div>
            )}

            {showDeletePopup && (
                <div className="user-popup">
                    <div className="user-popup-content">
                        <h3>Are you sure you want to delete your account?</h3>
                        <p>Your account will be permanently deleted, and you will no longer have access.</p>
                        <div className="user-popup-buttons">
                            <button onClick={handleDelete} className="user-delete-confirm">Yes, Delete</button>
                            <button onClick={() => setShowDeletePopup(false)} className="user-delete-cancel">Cancel</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ProfilePage;
