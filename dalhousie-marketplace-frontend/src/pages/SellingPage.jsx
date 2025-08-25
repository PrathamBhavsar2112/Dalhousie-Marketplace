import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { BASE_URL } from "../constant_url";
import "../css/SellingPage.css"
import axios from 'axios';
import { useDarkMode } from "../pages/DarkModeContext";


const Items = () => {
  const navigate = useNavigate();
  const { isDarkMode } = useDarkMode();
  const [formData, setFormData] = useState({
    title: "",
    description: "",
    price: "",
    categoryId: "",
    images: [],
    purchaseDate: "",
    quantity: "",
    biddingAllowed: "no",
    startingBid: "",
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) {
      navigate("/login");
      return;
    }

    axios.get(`${BASE_URL}/api/categories`)
      .then(response => {
        console.log("API Response:", response.data); // Debugging
        setCategories(response.data.categories || response.data || []); // Ensure it's an array
      })
      .catch(error => console.error("Error fetching categories:", error));
  }, [navigate, setCategories]);

  useEffect(() => {
    const appContainer = document.querySelector(".items-app-container");
    if (isDarkMode) {
      appContainer?.classList.add("dark");
    } else {
      appContainer?.classList.remove("dark");
    }
  }, [isDarkMode]);

  const handleChange = (e) => {
    const { name, value } = e.target;

    if (["price", "startingBid"].includes(name)) {
      if (value === "" || /^(\d*\.?\d*)$/.test(value)) {
        setFormData((prevData) => ({ ...prevData, [name]: value }));
      }
    } else if (name === "quantity") {
      if (value === "" || /^[1-9]\d*$/.test(value)) {
        setFormData((prevData) => ({ ...prevData, [name]: value }));
      }
    } else if (name === "biddingAllowed" && value === "no") {
      setFormData((prevData) => ({
        ...prevData,
        [name]: value,
        startingBid: "",
      }));
    } else {
      setFormData((prevData) => ({ ...prevData, [name]: value }));
    }
  };

  const handleFileChange = (e) => {
    setFormData({ ...formData, images: Array.from(e.target.files) });
  };

  const validateForm = () => {
    let newErrors = {};

    if (!formData.title.trim()) newErrors.title = "Title is required.";
    if (!formData.description.trim())
      newErrors.description = "Description is required.";
    if (!formData.price || parseFloat(formData.price) <= 0)
      newErrors.price = "Price must be greater than zero.";
    if (!formData.categoryId)
      newErrors.categoryId = "Please select a category.";
    if (!formData.purchaseDate)
      newErrors.purchaseDate = "Purchase date is required.";
    if (!formData.quantity || parseInt(formData.quantity) <= 0)
      newErrors.quantity = "Quantity must be greater than zero.";
    if (formData.images.length === 0)
      newErrors.images = "Please select at least one image.";

    if (
      formData.biddingAllowed === "yes" &&
      (!formData.startingBid || parseFloat(formData.startingBid) <= 0)
    ) {
      newErrors.startingBid = "Starting bid must be greater than zero.";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrors({});
    setLoading(true);

    if (!validateForm()) {
      setLoading(false);
      return;
    }

    try {
      const token = localStorage.getItem("token");
      const formDataToSubmit = new FormData();

      Object.keys(formData).forEach((key) => {
        if (key === "images") {
          formData.images.forEach((image) => {
            formDataToSubmit.append("images", image);
          });
        } else if (key === "biddingAllowed") {
          formDataToSubmit.append(key, formData[key] === "yes");
        } else {
          formDataToSubmit.append(key, formData[key]);
        }
      });

      const response = await fetch(`${BASE_URL}/api/listings/create`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: formDataToSubmit,
      });

      if (!response.ok) {
        throw new Error((await response.text()) || "Failed to post product.");
      }

      setMessage("Product posted successfully!");
      //navigate("/buying");

      setTimeout(() => {
        window.location.reload();
      }, 2000);
    } catch (error) {
      console.error("Error:", error);
      setErrors({ general: error.message || "Failed to post product." });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={`items-container ${isDarkMode ? "dark" : ""}`}>
      <div className="items-content-wrapper">
        <div className="items-form-container">
          <h2 className="items-form-title">Sell Your Product</h2>
          {errors.general && (
            <p className="items-error-message">{errors.general}</p>
          )}

          <form onSubmit={handleSubmit} className="items-form">
            <div className="items-form-group">
              <label>Title</label>
              <input
                type="text"
                name="title"
                value={formData.title}
                onChange={handleChange}
              />
              {errors.title && (
                <p className="items-validation-error">{errors.title}</p>
              )}
            </div>

            <div className="items-form-group">
              <label>Description</label>
              <textarea
                name="description"
                value={formData.description}
                onChange={handleChange}
              />
              {errors.description && (
                <p className="items-validation-error">{errors.description}</p>
              )}
            </div>

            <div className="items-form-group">
              <label>Price ($)</label>
              <input
                type="number"
                name="price"
                value={formData.price}
                onChange={handleChange}
              />
              {errors.price && (
                <p className="items-validation-error">{errors.price}</p>
              )}
            </div>

            <div className="items-form-group">
              <label>Category</label>
              <select
                name="categoryId"
                value={formData.categoryId}
                onChange={handleChange}
              >
                <option value="">Select a Category</option>
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>{cat.name}</option>
                ))}
              </select>
              {errors.categoryId && (
                <p className="items-validation-error">{errors.categoryId}</p>
              )}
            </div>

            <div className="items-form-group">
              <label>Product Images</label>
              <input
                type="file"
                name="images"
                onChange={handleFileChange}
                accept="image/*"
                multiple
              />
              {errors.images && (
                <p className="items-validation-error">{errors.images}</p>
              )}
            </div>

            <div className="items-form-group">
              <label>Purchase Date</label>
              <input
                type="date"
                name="purchaseDate"
                value={formData.purchaseDate}
                onChange={handleChange}
              />
              {errors.purchaseDate && (
                <p className="items-validation-error">
                  {errors.purchaseDate}
                </p>
              )}
            </div>

            <div className="items-form-group">
              <label>Stock Quantity</label>
              <input
                type="number"
                name="quantity"
                value={formData.quantity}
                onChange={handleChange}
              />
              {errors.quantity && (
                <p className="items-validation-error">{errors.quantity}</p>
              )}
            </div>

            <div className="items-form-group">
              <label>Bidding Allowed</label>
              <div className="items-bidding-options">
                <label className="items-radio-option">
                  <input
                    type="radio"
                    name="biddingAllowed"
                    value="yes"
                    checked={formData.biddingAllowed === "yes"}
                    onChange={handleChange}
                  />
                  Yes
                </label>
                <label className="items-radio-option">
                  <input
                    type="radio"
                    name="biddingAllowed"
                    value="no"
                    checked={formData.biddingAllowed === "no"}
                    onChange={handleChange}
                  />
                  No
                </label>
              </div>
            </div>

            {formData.biddingAllowed === "yes" && (
              <div className="items-form-group">
                <label>Starting Bid Price ($)</label>
                <input
                  type="number"
                  name="startingBid"
                  value={formData.startingBid}
                  onChange={handleChange}
                />
                {errors.startingBid && (
                  <p className="items-validation-error">
                    {errors.startingBid}
                  </p>
                )}
              </div>
            )}

            <button
              type="submit"
              className="items-submit-button"
              disabled={loading}
            >
              {loading ? "Posting..." : "Post Product"}
            </button>
            {message && (
              <div
                style={{
                  color: message.includes("Please") ? "red" : "green",
                  textAlign: "center",
                }}
              >
                {message}
              </div>
            )}
          </form>
        </div>
      </div>
    </div>
  );
};

export default Items;