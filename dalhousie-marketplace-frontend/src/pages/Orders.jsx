import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Receipt,
  Download,
  Eye,
  AlertCircle,
  CheckCircle,
  Truck,
  Clock,
  XCircle,
  Calendar,
  ChevronDown,
  ChevronUp,
  Filter,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  Package,
  ExternalLink
} from "lucide-react";
import "../css/Orders.css";
import { BASE_URL } from "../constant_url";
import { useLocation } from "react-router-dom";



// Status icon component to show visual indicators based on order status
const StatusIcon = ({ status }) => {
  switch(status) {
    case 'COMPLETED':
      return <CheckCircle size={18} className="text-green-500" />;
    case 'SHIPPED':
      return <Truck size={18} className="text-blue-500" />;
    case 'PENDING':
      return <Clock size={18} className="text-yellow-500" />;
    case 'FAILED':
      return <XCircle size={18} className="text-red-500" />;
    case 'CANCELLED':
      return <AlertCircle size={18} className="text-red-500" />;
    default:
      return <Clock size={18} className="text-gray-500" />;
  }
};

const OrderItem = ({ order, isDarkMode }) => {
  const [isHovered, setIsHovered] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [orderItems, setOrderItems] = useState([]);
  const [loadingItems, setLoadingItems] = useState(false);
  const [error, setError] = useState(null);
  const navigate = useNavigate();
  
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };
  
  const getStatusColor = (status) => {
    switch(status) {
      case 'COMPLETED':
        return 'text-green-500';
      case 'PENDING':
        return 'text-yellow-500';
      case 'SHIPPED':
        return 'text-blue-500';
      case 'FAILED':
        return 'text-red-500';
      case 'CANCELLED':
        return 'text-red-500';
      default:
        return '';
    }
  };

  const fetchOrderItems = async () => {
    setLoadingItems(true);
    try {
      const token = localStorage.getItem("token");
      if (!token) {
        navigate('/login');
        return;
      }

      const response = await fetch(`${BASE_URL}/api/orders/${order.orderId}/items`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (!response.ok) {
        throw new Error('Failed to fetch order items');
      }

      const data = await response.json();
      setOrderItems(data);
    } catch (err) {
      console.error('Error fetching order items:', err);
      setError(err.message || 'Failed to load order items');
    } finally {
      setLoadingItems(false);
    }
  };

  const handleExpand = () => {
    const newExpandedState = !isExpanded;
    setIsExpanded(newExpandedState);
    
    if (newExpandedState && orderItems.length === 0) {
      fetchOrderItems();
    }
  };

  const handleDownloadReceipt = (e) => {
    e.stopPropagation();
    
    if (order.receiptUrl) {
      // Open receipt URL in a new tab
      window.open(order.receiptUrl, '_blank');
    } else {
      alert("Receipt not available for this order");
    }
  };

  const handleViewDetails = (e) => {
    e.stopPropagation();
    handleExpand();
  };
  
  return (
    <div 
      className={`order-item ${isDarkMode ? "dark" : ""}`}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <div className="order-header" onClick={handleExpand}>
        <div className="order-basic-info">
          <h3>Order #{order.orderId}</h3>
          <p>Placed: {formatDate(order.createdAt || new Date())}</p>
        </div>
        <div className="order-status-price">
          <span>${order.total.toFixed(2)} {order.currency}</span>
          <span className={`status-badge ${getStatusColor(order.status)}`}>
            <StatusIcon status={order.status} />
            {order.status}
          </span>
          <button 
            onClick={handleExpand}
            className="expand-button"
            aria-label={isExpanded ? "Collapse order details" : "Expand order details"}
          >
            {isExpanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
          </button>
        </div>
      </div>
      
      {(isHovered || isExpanded) && (
        <div className="order-details">
          <div className="order-actions">
            <button 
              className="order-action-btn" 
              onClick={handleViewDetails}
            >
              <Eye size={16} />
              {isExpanded ? "Hide Details" : "View Details"}
            </button>
            <button 
              className="order-action-btn" 
              onClick={handleDownloadReceipt}
              disabled={!order.receiptUrl}
              title={order.receiptUrl ? "View Receipt" : "Receipt not available"}
            >
              {order.receiptUrl ? <ExternalLink size={16} /> : <Download size={16} />}
              {order.receiptUrl ? "View Receipt" : "Receipt N/A"}
            </button>
          </div>
          
          <div className="order-info">
            <div>
              <p><strong>Payment Method:</strong> {order.paymentMethod}</p>
              <p><strong>Order Date:</strong> {formatDate(order.createdAt)}</p>
            </div>
            <div>
              <p><strong>Last Updated:</strong> {formatDate(order.updatedAt)}</p>
              <p><strong>Order ID:</strong> {order.orderId}</p>
            </div>
          </div>
          
          {isExpanded && (
            <div className="expanded-order-content">
              <h4>Order Items:</h4>
              {loadingItems ? (
                <div className="order-items-loading">
                  <div className="loading-spinner-small"></div>
                  <span>Loading order items...</span>
                </div>
              ) : error ? (
                <div className="order-items-error">
                  <AlertCircle size={16} />
                  <span>{error}</span>
                </div>
              ) : orderItems.length > 0 ? (
                <div className="order-items-detailed">
                  {orderItems.map((item) => (
                    <div key={item.orderItemId} className="order-item-detailed">
                      <div className="order-item-image-placeholder">
                        <Package size={24} />
                      </div>
                      <div className="order-item-info">
                        <h5 className="order-item-title" onClick={() => navigate(`/listing/${item.listing.id}`)}>
                          {item.listing.title}
                        </h5>
                        <p className="order-item-desc">{item.listing.description}</p>
                        <div className="order-item-meta">
                          <span>Seller: {item.listing.seller.username}</span>
                          <span>Quantity: {item.quantity}</span>
                        </div>
                      </div>
                      <div className="order-item-price">
                        ${(item.price * item.quantity).toFixed(2)}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="no-items-message">Item details not available</p>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

// Date Range Picker component
const DateRangePicker = ({ startDate, endDate, onStartDateChange, onEndDateChange }) => {
  return (
    <div className="date-range-picker">
      <div className="date-input-group">
        <label>From:</label>
        <input 
          type="date" 
          value={startDate} 
          onChange={(e) => onStartDateChange(e.target.value)}
        />
      </div>
      <div className="date-input-group">
        <label>To:</label>
        <input 
          type="date" 
          value={endDate} 
          onChange={(e) => onEndDateChange(e.target.value)}
        />
      </div>
    </div>
  );
};

// Amount Range Filter component
const AmountRangeFilter = ({ minAmount, maxAmount, onMinAmountChange, onMaxAmountChange }) => {
  return (
    <div className="amount-range-filter">
      <div className="amount-input-group">
        <label>Min ($):</label>
        <input 
          type="number" 
          min="0" 
          value={minAmount} 
          onChange={(e) => onMinAmountChange(parseFloat(e.target.value) || 0)}
        />
      </div>
      <div className="amount-input-group">
        <label>Max ($):</label>
        <input 
          type="number" 
          min="0" 
          value={maxAmount} 
          onChange={(e) => onMaxAmountChange(parseFloat(e.target.value) || 0)}
        />
      </div>
    </div>
  );
};

// Pagination component
const Pagination = ({ 
  currentPage, 
  totalPages, 
  onPageChange, 
  itemsPerPage, 
  onItemsPerPageChange, 
  totalItems
}) => {
  const pageSizes = [5, 10, 15, 20];
  
  // Generate page numbers to display
  const getPageNumbers = () => {
    const pageNumbers = [];
    const maxPagesToShow = 5;
    
    if (totalPages <= maxPagesToShow) {
      // Show all pages if total pages is less than or equal to maxPagesToShow
      for (let i = 1; i <= totalPages; i++) {
        pageNumbers.push(i);
      }
    } else {
      // Always include first page
      pageNumbers.push(1);
      
      // Calculate start and end of page range
      let startPage = Math.max(2, currentPage - 1);
      let endPage = Math.min(totalPages - 1, currentPage + 1);
      
      // If we're at the start, show more pages after
      if (currentPage <= 2) {
        endPage = Math.min(totalPages - 1, 4);
      }
      
      // If we're at the end, show more pages before
      if (currentPage >= totalPages - 1) {
        startPage = Math.max(2, totalPages - 3);
      }
      
      // Add ellipsis if there's a gap after first page
      if (startPage > 2) {
        pageNumbers.push('...');
      }
      
      // Add the page range
      for (let i = startPage; i <= endPage; i++) {
        pageNumbers.push(i);
      }
      
      // Add ellipsis if there's a gap before last page
      if (endPage < totalPages - 1) {
        pageNumbers.push('...');
      }
      
      // Always include last page
      pageNumbers.push(totalPages);
    }
    
    return pageNumbers;
  };
  
  return (
    <div className="pagination-container">
      <div className="pagination-info">
        <span>
          Showing {totalItems === 0 ? 0 : (currentPage - 1) * itemsPerPage + 1} to {Math.min(currentPage * itemsPerPage, totalItems)} of {totalItems} entries
        </span>
        
        <div className="items-per-page-selector">
          <span>Show</span>
          <select 
            value={itemsPerPage} 
            onChange={(e) => onItemsPerPageChange(Number(e.target.value))}
          >
            {pageSizes.map(size => (
              <option key={size} value={size}>{size}</option>
            ))}
          </select>
          <span>entries</span>
        </div>
      </div>
      
      <div className="pagination-controls">
        <button 
          className="pagination-btn first-page" 
          onClick={() => onPageChange(1)}
          disabled={currentPage === 1}
          title="First Page"
        >
          <ChevronsLeft size={16} />
        </button>
        
        <button 
          className="pagination-btn prev-page" 
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 1}
          title="Previous Page"
        >
          <ChevronLeft size={16} />
        </button>
        
        <div className="page-numbers">
          {getPageNumbers().map((page, index) => (
            page === '...' ? (
              <span key={`ellipsis-${index}`} className="ellipsis">...</span>
            ) : (
              <button 
                key={page} 
                className={`pagination-btn page-number ${currentPage === page ? 'active' : ''}`}
                onClick={() => onPageChange(page)}
              >
                {page}
              </button>
            )
          ))}
        </div>
        
        <button 
          className="pagination-btn next-page" 
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage === totalPages || totalPages === 0}
          title="Next Page"
        >
          <ChevronRight size={16} />
        </button>
        
        <button 
          className="pagination-btn last-page" 
          onClick={() => onPageChange(totalPages)}
          disabled={currentPage === totalPages || totalPages === 0}
          title="Last Page"
        >
          <ChevronsRight size={16} />
        </button>
      </div>
    </div>
  );
};

const Orders = () => {
  const navigate = useNavigate();
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const location = useLocation();
  
  
  // Filter states
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [showFilterOptions, setShowFilterOptions] = useState(false);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [minAmount, setMinAmount] = useState(0);
  const [maxAmount, setMaxAmount] = useState(0);
  const searchParams = new URLSearchParams(location.search);
  const searchQuery = searchParams.get("search") || "";
  // const [searchQuery, setSearchQuery] = useState('');
  
  // Sorting states
  const [sortField, setSortField] = useState('createdAt');
  const [sortDirection, setSortDirection] = useState('desc'); // 'asc' or 'desc'
  
  // Pagination states
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);

  useEffect(() => {
    const fetchOrders = async () => {
      try {
        const userId = localStorage.getItem("userId");
        const token = localStorage.getItem("token");
        
        if (!userId || !token) {
          navigate('/login');
          return;
        }
        
        // Fetch payments data for the current user
        const response = await fetch(`${BASE_URL}/api/orders/user/${userId}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
        
        if (!response.ok) {
          throw new Error("Failed to fetch orders");
        }
        
        const paymentsData = await response.json();
        
        // Group payments by order ID and use the latest payment for each order
        const paymentsByOrder = {};
        paymentsData.forEach(payment => {
          if (!paymentsByOrder[payment.orderId]) {
            paymentsByOrder[payment.orderId] = [];
          }
          paymentsByOrder[payment.orderId].push(payment);
        });
        
        // Convert to a list of orders with the latest payment information
        const processedOrders = Object.keys(paymentsByOrder).map(orderId => {
          const orderPayments = paymentsByOrder[orderId];
          const latestPayment = orderPayments.reduce((latest, current) => {
            return new Date(current.updatedAt) > new Date(latest.updatedAt) ? current : latest;
          }, orderPayments[0]);
          
          return {
            orderId: parseInt(orderId),
            createdAt: latestPayment.orderDate,
            updatedAt: latestPayment.updatedAt,
            status: latestPayment.orderStatus,
            total: latestPayment.amount,
            currency: latestPayment.currency,
            paymentMethod: latestPayment.paymentMethod,
            paymentId: latestPayment.paymentId,
            receiptUrl: latestPayment.receiptUrl // Include the receipt URL
          };
        });
        
        // Sort orders by date (newest first)
        processedOrders.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        
        // Find max amount for the amount filter
        if (processedOrders.length > 0) {
          const maxOrderAmount = Math.max(...processedOrders.map(order => order.total));
          setMaxAmount(maxOrderAmount);
        }
        
        setOrders(processedOrders);
        setLoading(false);
      } catch (err) {
        console.error("Error fetching orders:", err);
        setError(err.message || "Error fetching orders");
        setLoading(false);
      }
    };

    const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    setIsDarkMode(prefersDark);
    fetchOrders();
  }, [navigate]);

  useEffect(() => {
    // Reset to first page when filters change
    setCurrentPage(1);
  }, [statusFilter, startDate, endDate, minAmount, maxAmount, searchQuery, sortField, sortDirection, itemsPerPage]);

  // const handleLogout = () => {
  //   localStorage.removeItem("token");
  //   localStorage.removeItem("userId");
  //   navigate("/login");
  // };
  
  // Toggle sort direction or change sort field
  const handleSort = (field) => {
    if (sortField === field) {
      // Toggle direction if same field
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      // Default to descending for new field
      setSortField(field);
      setSortDirection('desc');
    }
  };
  
  // Reset all filters
  const resetFilters = () => {
    setStatusFilter('ALL');
    setStartDate('');
    setEndDate('');
    setMinAmount(0);
    setMaxAmount(Math.max(...orders.map(order => order.total)));
    //setSearchQuery('');
  };
  
  // Apply all filters and sorting
  const getFilteredAndSortedOrders = () => {
    let result = [...orders];
    
    // Apply status filter
    if (statusFilter !== 'ALL') {
      result = result.filter(order => order.status === statusFilter);
    }
    
    // Apply date filter
    if (startDate) {
      const startDateObj = new Date(startDate);
      result = result.filter(order => new Date(order.createdAt) >= startDateObj);
    }
    
    if (endDate) {
      const endDateObj = new Date(endDate);
      // Set time to end of day
      endDateObj.setHours(23, 59, 59, 999);
      result = result.filter(order => new Date(order.createdAt) <= endDateObj);
    }
    
    // Apply amount filter
    if (minAmount > 0) {
      result = result.filter(order => order.total >= minAmount);
    }
    
    if (maxAmount > 0) {
      result = result.filter(order => order.total <= maxAmount);
    }
    
    // Apply search query (search by order ID, status, or payment method)
    if (searchQuery) {
      result = result.filter(order => 
        order.orderId.toString().includes(searchQuery) ||
        order.status.toLowerCase().includes(searchQuery.toLowerCase()) ||
        order.paymentMethod?.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }
    
    // Apply sorting
    result.sort((a, b) => {
      let comparison = 0;
      
      switch(sortField) {
        case 'orderId':
          comparison = a.orderId - b.orderId;
          break;
        case 'createdAt':
          comparison = new Date(a.createdAt) - new Date(b.createdAt);
          break;
        case 'total':
          comparison = a.total - b.total;
          break;
        case 'status':
          comparison = a.status.localeCompare(b.status);
          break;
        default:
          comparison = 0;
      }
      
      return sortDirection === 'asc' ? comparison : -comparison;
    });
    
    return result;
  };
  
  const filteredOrders = getFilteredAndSortedOrders();
  
  // Get paginated orders
  const getPaginatedOrders = () => {
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    return filteredOrders.slice(startIndex, endIndex);
  };
  
  const paginatedOrders = getPaginatedOrders();
  const totalPages = Math.ceil(filteredOrders.length / itemsPerPage);

  if (loading) {
    return (
      <div className="loading-container">
        <div className="loading-spinner"></div>
        <p>Loading your orders...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="error-message">
        <AlertCircle size={32} style={{ marginBottom: '10px' }} />
        <h2>Error Loading Orders</h2>
        <p>{error}</p>
        <button onClick={() => window.location.reload()} className="checkout-button" style={{ marginTop: '20px' }}>
          Try Again
        </button>
      </div>
    );
  }

  return (
    // <div className={`app-container ${isDarkMode ? "dark" : ""}`}>
    //   {/* <div className="sidebar">
    //     <img src={DalLogo} alt="Logo" className="logo" onClick={() => navigate("/dashboard")} />

    //     <div className="sidebar-icons">
    //       <IconButton Icon={LayoutGrid} onClick={() => navigate("/dashboard")} />
    //       <IconButton Icon={ShoppingBag} onClick={() => navigate("/buying")} />
    //       <IconButton Icon={FileText} onClick={() => navigate("/selling")} />
    //       <IconButton Icon={Heart} onClick={() => navigate("/wishlist")} />
    //       <IconButton Icon={User} onClick={() => navigate("/profilepage")} />
    //       <IconButton Icon={Settings} onClick={() => navigate("/settings")} />
    //       <IconButton className="LogOut-Icon" Icon={LogOut} onClick={handleLogout} />
    //     </div>
    //   </div> */}

    //   <div className="main-content">
    //     {/* <div className="top-bar">
    //       <div className="search-container-mpListing">
    //         <input 
    //           type="text" 
    //           placeholder="Search by order ID, status, or payment method..." 
    //           className="search-input" 
    //           value={searchQuery}
    //           onChange={(e) => setSearchQuery(e.target.value)}
    //         />
    //         <Search className="search-icon" />
    //       </div>
    //       <div className="top-bar-icons">
    //         <CartCounter
    //           onClick={() => navigate("/cart")}
    //           onCartUpdate={(count) => console.log("Cart updated:", count)}
    //         />
    //         <IconButton
    //           Icon={Receipt}
    //           onClick={() => navigate("/orders")}
    //           title="Orders & Receipts"
    //         />
    //         <IconButton
    //           Icon={Bell}
    //           onClick={() => console.log("Notifications clicked")}
    //         />
    //         <IconButton
    //           Icon={isDarkMode ? Sun : Moon}
    //           onClick={() => setIsDarkMode(!isDarkMode)}
    //           darkModeIcon={true}
    //         />
    //         <img src={smillingWoman} alt="Profile" className="profile-image" />
    //       </div>
    //     </div> */}

    //     <div className="cart-content">
    //       <div className="cart-container">
    //         <div className="order-page-header">
    //           <h1>My Orders</h1>
              
    //           <div className="order-controls">
    //             <div className="order-filters">
    //               <button 
    //                 onClick={() => setStatusFilter('ALL')} 
    //                 className={`filter-btn ${statusFilter === 'ALL' ? 'active' : ''}`}
    //               >
    //                 All
    //               </button>
    //               <button 
    //                 onClick={() => setStatusFilter('COMPLETED')} 
    //                 className={`filter-btn ${statusFilter === 'COMPLETED' ? 'active' : ''}`}
    //               >
    //                 Completed
    //               </button>
    //               <button 
    //                 onClick={() => setStatusFilter('PENDING')} 
    //                 className={`filter-btn ${statusFilter === 'PENDING' ? 'active' : ''}`}
    //               >
    //                 Pending
    //               </button>
    //               <button 
    //                 onClick={() => setStatusFilter('FAILED')} 
    //                 className={`filter-btn ${statusFilter === 'FAILED' ? 'active' : ''}`}
    //               >
    //                 Failed
    //               </button>
    //             </div>
                
    //             <button 
    //               className="filter-toggle-btn"
    //               onClick={() => setShowFilterOptions(!showFilterOptions)}
    //               title="Advanced Filters"
    //             >
    //               <Filter size={16} />
    //               {showFilterOptions ?  'Show Filters': 'Hide Filters'}
    //             </button>
    //           </div>
    //         </div>
            
    //         {!showFilterOptions && (
    //           <div className="advanced-filters">
    //             <div className="filter-section">
    //               <h3><Calendar size={16} /> Date Range</h3>
    //               <DateRangePicker 
    //                 startDate={startDate}
    //                 endDate={endDate}
    //                 onStartDateChange={setStartDate}
    //                 onEndDateChange={setEndDate}
    //               />
    //             </div>
                
    //             <div className="filter-section">
    //               <h3><Receipt size={16} /> Amount Range</h3>
    //               <AmountRangeFilter 
    //                 minAmount={minAmount}
    //                 maxAmount={maxAmount}
    //                 onMinAmountChange={setMinAmount}
    //                 onMaxAmountChange={setMaxAmount}
    //               />
    //             </div>
                
    //             <div className="filter-actions">
    //               <button className="reset-filters-btn" onClick={resetFilters}>
    //                 Reset Filters
    //               </button>
    //             </div>
    //           </div>
    //         )}
            
    //         <div className="sorting-controls">
    //           <div className="sort-button-group">
    //             <span>Sort by:</span>
    //             <button 
    //               className={`sort-button ${sortField === 'createdAt' ? 'active' : ''}`}
    //               onClick={() => handleSort('createdAt')}
    //             >
    //               Date
    //               {sortField === 'createdAt' && (
    //                 sortDirection === 'asc' ? <ChevronUp size={14} /> : <ChevronDown size={14} />
    //               )}
    //             </button>
    //             <button 
    //               className={`sort-button ${sortField === 'total' ? 'active' : ''}`}
    //               onClick={() => handleSort('total')}
    //             >
    //               Amount
    //               {sortField === 'total' && (
    //                 sortDirection === 'asc' ? <ChevronUp size={14} /> : <ChevronDown size={14} />
    //               )}
    //             </button>
    //             <button 
    //               className={`sort-button ${sortField === 'status' ? 'active' : ''}`}
    //               onClick={() => handleSort('status')}
    //             >
    //               Status
    //               {sortField === 'status' && (
    //                 sortDirection === 'asc' ? <ChevronUp size={14} /> : <ChevronDown size={14} />
    //               )}
    //             </button>
    //             <button 
    //               className={`sort-button ${sortField === 'orderId' ? 'active' : ''}`}
    //               onClick={() => handleSort('orderId')}
    //             >
    //               Order ID
    //               {sortField === 'orderId' && (
    //                 sortDirection === 'asc' ? <ChevronUp size={14} /> : <ChevronDown size={14} />
    //               )}
    //             </button>
    //           </div>
    //         </div>

    //         <div className="orders-list">
    //           {filteredOrders.length === 0 ? (
    //             <div className="empty-orders">
    //               <Receipt size={64} style={{ margin: '0 auto 20px', opacity: 0.5 }} />
    //               {orders.length === 0 ? (
    //                 <>
    //                   <h2>No orders yet</h2>
    //                   <p>When you place orders, they will appear here</p>
    //                   <button
    //                     className="cart-button"
    //                     onClick={() => navigate("/buying")}
    //                   >
    //                     Start Shopping
    //                   </button>
    //                 </>
    //               ) : (
    //                 <>
    //                   <h2>No matching orders found</h2>
    //                   <p>Try changing your filters</p>
    //                   <button
    //                     className="cart-button"
    //                     onClick={resetFilters}
    //                   >
    //                     Reset Filters
    //                   </button>
    //                 </>
    //               )}
    //             </div>
    //           ) : (
    //             paginatedOrders.map((order) => (
    //               <OrderItem
    //                 key={order.orderId}
    //                 order={order}
    //                 isDarkMode={isDarkMode}
    //               />
    //             ))
    //           )}
    //         </div>
            
    //         {filteredOrders.length > 0 && (
    //           <Pagination 
    //             currentPage={currentPage}
    //             totalPages={totalPages}
    //             onPageChange={setCurrentPage}
    //             itemsPerPage={itemsPerPage}
    //             onItemsPerPageChange={setItemsPerPage}
    //             totalItems={filteredOrders.length}
    //           />
    //         )}
    //       </div>
    //     </div>
    //   </div>
    // </div>
     <div className={`cart-content ${isDarkMode ? "dark" : ""}`}>

    
    {/* <div className="cart-content"> */}
    <div className="cart-container">
      <div className="order-page-header">
        <h1>My Orders</h1>
        
        <div className="order-controls">
          <div className="order-filters">
            <button 
              onClick={() => setStatusFilter('ALL')} 
              className={`filter-btn ${statusFilter === 'ALL' ? 'active' : ''}`}
            >
              All
            </button>
            <button 
              onClick={() => setStatusFilter('COMPLETED')} 
              className={`filter-btn ${statusFilter === 'COMPLETED' ? 'active' : ''}`}
            >
              Completed
            </button>
            <button 
              onClick={() => setStatusFilter('PENDING')} 
              className={`filter-btn ${statusFilter === 'PENDING' ? 'active' : ''}`}
            >
              Pending
            </button>
            <button 
              onClick={() => setStatusFilter('FAILED')} 
              className={`filter-btn ${statusFilter === 'FAILED' ? 'active' : ''}`}
            >
              Failed
            </button>
          </div>
          
          <button 
            className="filter-toggle-btn"
            onClick={() => setShowFilterOptions(!showFilterOptions)}
            title="Advanced Filters"
          >
            <Filter size={16} />
            {showFilterOptions ?  'Show Filters': 'Hide Filters'}
          </button>
        </div>
      </div>
      
      {!showFilterOptions && (
        <div className="advanced-filters">
          <div className="filter-section">
            <h3><Calendar size={16} /> Date Range</h3>
            <DateRangePicker 
              startDate={startDate}
              endDate={endDate}
              onStartDateChange={setStartDate}
              onEndDateChange={setEndDate}
            />
          </div>
          
          <div className="filter-section">
            <h3><Receipt size={16} /> Amount Range</h3>
            <AmountRangeFilter 
              minAmount={minAmount}
              maxAmount={maxAmount}
              onMinAmountChange={setMinAmount}
              onMaxAmountChange={setMaxAmount}
            />
          </div>
          
          <div className="filter-actions">
            <button className="reset-filters-btn" onClick={resetFilters}>
              Reset Filters
            </button>
          </div>
        </div>
      )}
      
      <div className="sorting-controls">
        <div className="sort-button-group">
          <span>Sort by:</span>
          <button 
            className={`sort-button ${sortField === 'createdAt' ? 'active' : ''}`}
            onClick={() => handleSort('createdAt')}
          >
            Date
            {sortField === 'createdAt' && (
              sortDirection === 'asc' ? <ChevronUp size={14} /> : <ChevronDown size={14} />
            )}
          </button>
          <button 
            className={`sort-button ${sortField === 'total' ? 'active' : ''}`}
            onClick={() => handleSort('total')}
          >
            Amount
            {sortField === 'total' && (
              sortDirection === 'asc' ? <ChevronUp size={14} /> : <ChevronDown size={14} />
            )}
          </button>
          <button 
            className={`sort-button ${sortField === 'status' ? 'active' : ''}`}
            onClick={() => handleSort('status')}
          >
            Status
            {sortField === 'status' && (
              sortDirection === 'asc' ? <ChevronUp size={14} /> : <ChevronDown size={14} />
            )}
          </button>
          <button 
            className={`sort-button ${sortField === 'orderId' ? 'active' : ''}`}
            onClick={() => handleSort('orderId')}
          >
            Order ID
            {sortField === 'orderId' && (
              sortDirection === 'asc' ? <ChevronUp size={14} /> : <ChevronDown size={14} />
            )}
          </button>
        </div>
      </div>

      <div className="orders-list">
        {filteredOrders.length === 0 ? (
          <div className="empty-orders">
            <Receipt size={64} style={{ margin: '0 auto 20px', opacity: 0.5 }} />
            {orders.length === 0 ? (
              <>
                <h2>No orders yet</h2>
                <p>When you place orders, they will appear here</p>
                <button
                  className="cart-button"
                  onClick={() => navigate("/buying")}
                >
                  Start Shopping
                </button>
              </>
            ) : (
              <>
                <h2>No matching orders found</h2>
                <p>Try changing your filters</p>
                <button
                  className="cart-button"
                  onClick={resetFilters}
                >
                  Reset Filters
                </button>
              </>
            )}
          </div>
        ) : (
          paginatedOrders.map((order) => (
            <OrderItem
              key={order.orderId}
              order={order}
              isDarkMode={isDarkMode}
            />
          ))
        )}
      </div>
      
      {filteredOrders.length > 0 && (
        <Pagination 
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
          itemsPerPage={itemsPerPage}
          onItemsPerPageChange={setItemsPerPage}
          totalItems={filteredOrders.length}
        />
      )}
    </div>
  </div>
  //</div>
  );
};

export default Orders;