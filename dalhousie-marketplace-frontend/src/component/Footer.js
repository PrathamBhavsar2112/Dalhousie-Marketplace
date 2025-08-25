import React from 'react'
function Footer() {
    return ( 
        <>
        <footer className="w3l-footer-29-main">
          <div className="footer-29 py-5">
            <div className="container py-lg-4">
              <h2>
                Special Offer All Branded Sandals are <br />
                Flat 50% Discount
              </h2>
              <div className="row footer-top-29 mt-md-5">
                <div className="col-lg-3 col-md-6 footer-list-29 footer-3 pe-lg-5">
                  <h6 className="footer-title-29">USeful Links</h6>
                  <div className="footer-listw3-grids">
                    <ul className="footer-listw3">
                      <li>
                        <a href="index.html">Home</a>
                      </li>
                      <li>
                        <a href="about.html">About</a>
                      </li>
                      <li>
                        <a href="blog.html">Blogs</a>
                      </li>
                      <li>
                        <a href="contact.html">Contact</a>
                      </li>
                      <li>
                        <a href="#support">Support</a>
                      </li>
                      <li>
                        <a href="#news">News &amp; Media</a>
                      </li>
                      <li>
                        <a href="#Careers">Careers</a>
                      </li>
                    </ul>
                  </div>
                </div>
                <div className="col-lg-3 col-md-6 footer-list-29 footer-1 ps-lg-5 mt-lg-0 mt-md-5 mt-4">
                  <h6 className="footer-title-29">Information</h6>
                  <div className="footer-listw3-grids">
                    <ul className="footer-listw3">
                      <li>
                        <a href="#terms">Terms &amp; Conditions</a>
                      </li>
                      <li>
                        <a href="#deivery">Delivery Terms</a>
                      </li>
                      <li>
                        <a href="#order">Order Tracking</a>
                      </li>
                      <li>
                        <a href="#returns">Returns Policy</a>
                      </li>
                      <li>
                        <a href="#support">Privacy Policy</a>
                      </li>
                      <li>
                        <a href="#faq">FAQ</a>
                      </li>
                      <li>
                        <a href="#shop">The Shop</a>
                      </li>
                    </ul>
                  </div>
                </div>
                <div className="col-lg-3 col-md-6 footer-list-29 footer-3 mt-lg-0 mt-md-5 mt-4 pe-lg-5">
                  <h6 className="footer-title-29">Address</h6>
                  <div className="footer-listw3-grids">
                    <ul className="footer-listw3">
                      <li>London, UK</li>
                      <li>998 13h Street, Office 436</li>
                      <li>Harlo 61466</li>
                      <li>
                        <a href="tel:+(21) 255 999 8888">+(21) 255 999 8888</a>
                      </li>
                      <li>
                        <a href="mailto:ShoppyKart@mail.com" className="mail">
                          ShoppyKart@mail.com
                        </a>
                      </li>
                    </ul>
                  </div>
                </div>
                <div className="col-lg-3 col-md-6 footer-list-29 footer-4 mt-lg-0 mt-md-5 mt-4  ps-lg-5">
                  <h6 className="footer-title-29">Payment Method</h6>
                  <ul className="pay-method-grids">
                    <li>
                      <a className="pay-method" href="#">
                        <span className="fab fa-cc-visa" aria-hidden="true" />
                      </a>
                    </li>
                    <li>
                      <a className="pay-method" href="#">
                        <span
                          className="fab fa-cc-discover"
                          aria-hidden="true"
                        />
                      </a>
                    </li>
                    <li>
                      <a className="pay-method" href="#">
                        <span className="fab fa-cc-paypal" aria-hidden="true" />
                      </a>
                    </li>
                    <li>
                      <a className="pay-method" href="#">
                        <span
                          className="fab fa-cc-mastercard"
                          aria-hidden="true"
                        />
                      </a>
                    </li>
                    <li>
                      <a className="pay-method" href="#">
                        <span className="fab fa-cc-stripe" aria-hidden="true" />
                      </a>
                    </li>
                    <li>
                      <a className="pay-method" href="#">
                        <span className="fab fa-cc-amex" aria-hidden="true" />
                      </a>
                    </li>
                    <li>
                      <a className="pay-method" href="#">
                        <span className="fab fa-cc-jcb" aria-hidden="true" />
                      </a>
                    </li>
                    <li>
                      <a className="pay-method" href="#">
                        <span className="cc-diners-club" aria-hidden="true" />
                      </a>
                    </li>
                  </ul>
                </div>
              </div>
              <div className="row bottom-copies">
                <p className="copy-footer-29 col-lg-8">
                  © 2021 ShoppyKart. All rights reserved | Designed by{" "}
                  <a href="https://w3layouts.com/" target="_blank">
                    W3layouts
                  </a>
                </p>
                <div className="col-lg-4 main-social-right mt-lg-0 mt-4">
                  <div className="main-social-footer-29">
                    <a href="#facebook" className="facebook">
                      <span className="fab fa-facebook-f" />
                    </a>
                    <a href="#twitter" className="twitter">
                      <span className="fab fa-twitter" />
                    </a>
                    <a href="#instagram" className="instagram">
                      <span className="fab fa-instagram" />
                    </a>
                    <a href="#linkd" className="linkd">
                      <span className="fab fa-linkedin-in" />
                    </a>
                  </div>
                </div>
              </div>
            </div>
          </div>
          {/* move top */}
          <button onClick="topFunction()" id="movetop" title="Go to top">
            <span className="fa fa-angle-up" />
          </button>
          {/* //move top */}
        </footer>
        </>
     );
}

export default Footer;