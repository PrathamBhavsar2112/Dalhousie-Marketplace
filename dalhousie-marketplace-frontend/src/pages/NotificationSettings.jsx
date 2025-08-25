import React, { useState, useEffect } from "react";
import { BASE_URL } from "../constant_url";
import "../css/NotificationSettings.css";
import { useDarkMode } from "../pages/DarkModeContext";

const NotificationSettings = () => {
    const userId = localStorage.getItem("userId");
    const { isDarkMode } = useDarkMode();
    const [preferences, setPreferences] = useState({
        receiveMessages: true,
        receiveItems: true,
        receiveBids: true,
    });
    const [keywords, setKeywords] = useState([]);
    const [newKeyword, setNewKeyword] = useState("");
    // Fetch User Preferences and keywords
    // useEffect(() => {
    //     fetch(`${BASE_URL}/api/user/preferences/${userId}`)
    //         .then((res) => res.json())
    //         .then((data) => setPreferences(data));

    useEffect(() => {
        const fetchPreferences = async () => {
            try {
                const response = await fetch(`${BASE_URL}/api/user/preferences/${userId}`);
                if (!response.ok) throw new Error("Failed to fetch preferences");
                const data = await response.json();
                setPreferences(data);
            } catch (error) {
                console.error("Error fetching preferences:", error.message);
            }
        };

            const fetchKeywords = async () => {
                try {
                    const response = await fetch(`${BASE_URL}/api/user/preferences/${userId}/keywords`);
                    if (!response.ok) throw new Error("Failed to fetch keywords");
                    const text = await response.text();
                    const data = text ? JSON.parse(text) : [];
                    if (Array.isArray(data)) {
                        setKeywords(data);
                    } else {
                        setKeywords([]);
                    }
                } catch (error) {
                    console.error("Error fetching keywords:", error.message);
                    setKeywords([]);
                }
            };
            fetchPreferences();
            fetchKeywords();
        }, [userId]);


    //         fetch(`${BASE_URL}/api/user/preferences/${userId}/keywords`)
    //         .then(res => {
    //             if (!res.ok) throw new Error("Failed to fetch");
    //             return res.text(); // Read as text first
    //         })
    //         .then(text => {
    //             if (text) {
    //                 try {
    //                     const data = JSON.parse(text); // Parse the text as JSON
    //                     if (Array.isArray(data)) {
    //                         setKeywords(data);
    //                     } else {
    //                         setKeywords([]); // Handle non-array response
    //                     }
    //                 } catch (error) {
    //                     throw new Error("Failed to parse JSON");
    //                 }
    //             } else {
    //                 setKeywords([]); // Handle empty response
    //             }
    //         })
    //         .catch(err => {
    //             console.error("Error fetching keywords:", err.message);
    //             setKeywords([]); // Handle errors gracefully
    //         });
    // }, [userId]);

    // Handle Checkbox Change
    const updatePreferences = (e) => {
        const { name, checked } = e.target;
        setPreferences({ ...preferences, [name]: checked });

        fetch(`${BASE_URL}/api/user/preferences/${userId}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ ...preferences, [name]: checked }),
        });
    };
    const addKeyword = () => {
        if (!newKeyword.trim()) return;

        fetch(`${BASE_URL}/api/user/preferences/${userId}/keywords`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ keyword: newKeyword.trim() }),
        })
            .then(() => {
                setKeywords([...keywords, newKeyword.trim()]);
                setNewKeyword("");
            });
    };

    const removeKeyword = (keyword) => {
        fetch(`${BASE_URL}/api/user/preferences/${userId}/keywords/${encodeURIComponent(keyword)}`, {
            method: "DELETE"
        }).then(() => {
            setKeywords(keywords.filter(k => k !== keyword));
        });
    };

    //     return (
    //       <div className={`notification-settings ${isDarkMode ? "dark" : ""}`}>
    //           <h2>Notification Settings</h2>
    //           <p>Manage which notifications you want to receive.</p>

    //           <div className="settings-container">
    //               <div className="settings-item">
    //                   <span>Receive Messages</span>
    //                   <label className="switch">
    //                       <input 
    //                           type="checkbox" 
    //                           name="receiveMessages" 
    //                           checked={preferences.receiveMessages} 
    //                           onChange={updatePreferences} 
    //                       />
    //                       <span className="slider round"></span>
    //                   </label>
    //               </div>

    //               <div className="settings-item">
    //                   <span>Receive New Items</span>
    //                   <label className="switch">
    //                       <input 
    //                           type="checkbox" 
    //                           name="receiveItems" 
    //                           checked={preferences.receiveItems} 
    //                           onChange={updatePreferences} 
    //                       />
    //                       <span className="slider round"></span>
    //                   </label>
    //               </div>

    //               <div className="settings-item">
    //                   <span>Receive Bids</span>
    //                   <label className="switch">
    //                       <input 
    //                           type="checkbox" 
    //                           name="receiveBids" 
    //                           checked={preferences.receiveBids} 
    //                           onChange={updatePreferences} 
    //                       />
    //                       <span className="slider round"></span>
    //                   </label>
    //               </div>
    //           </div>
    //       </div>
    //   );
    return (
        <div className={`notification-settings ${isDarkMode ? "dark" : ""}`}>
            <h2>Notification Settings</h2>
            <p>Manage which notifications you want to receive.</p>

            <div className="settings-container">
                {["Messages", "New Items", "Bids"].map((label, i) => {
                    const name = ["receiveMessages", "receiveItems", "receiveBids"][i];
                    return (
                        <div key={name} className="settings-item">
                            <span>Receive {label}</span>
                            <label className="switch">
                                <input
                                    type="checkbox"
                                    name={name}
                                    checked={preferences[name]}
                                    onChange={updatePreferences}
                                />
                                <span className="slider round"></span>
                            </label>
                        </div>
                    );
                })}
            </div>

            {/* Keyword section */}
            <div className="keyword-section">
                <h3>Item Keywords</h3>
                <p className="keyword-description">Add keywords to get item alerts (e.g., "laptop", "bike").</p>

                <div className="keyword-input-group">
                    <input
                        type="text"
                        value={newKeyword}
                        onChange={(e) => setNewKeyword(e.target.value)}
                        placeholder="Enter keyword"
                        className="keyword-input"
                    />
                    <button className="keyword-add-btn" onClick={addKeyword}>Add</button>
                </div>

                <div className="keyword-list">
                    {keywords.map((kw, idx) => (
                        <div className="keyword-chip" key={idx}>
                            {kw}
                            <button className="remove-btn" onClick={() => removeKeyword(kw)}>×</button>
                        </div>
                    ))}
                </div>
            </div>

        </div>
    );

};

export default NotificationSettings;