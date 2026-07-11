package com.molnify.xlport.servlet;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import org.junit.Test;

public class TestHealthCheck {

  private static final HttpServlet healthCheck = new InitXlPort();

  @Test
  public void testAlive() throws ServletException, IOException {
    HttpServletRequest req = getMockRequest("/alive", "GET");
    HttpServletResponse resp = getMockResponse();
    healthCheck.service(req, resp);
    assertEquals(200, resp.getStatus());
  }

  @Test
  public void testNotReady() throws ServletException, IOException {
    HttpServletRequest req = getMockRequest("/ready", "GET");
    HttpServletResponse resp = getMockResponse();
    healthCheck.service(req, resp);
    assertTrue(resp.getStatus() > 500);
  }

  @Test
  public void testReady() throws ServletException, IOException {
    healthCheck.init(
        new ServletConfig() {

          @Override
          public String getServletName() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public ServletContext getServletContext() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public Enumeration<String> getInitParameterNames() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public String getInitParameter(String name) {
            // TODO Auto-generated method stub
            return null;
          }
        });
    HttpServletRequest req = getMockRequest("/ready", "GET");
    HttpServletResponse resp = getMockResponse();
    healthCheck.service(req, resp);
    assertEquals(200, resp.getStatus());
    healthCheck.init(null);
  }

  protected static HttpServletResponse getMockResponse() {
    return new HttpServletResponse() {

      private int status = 0;

      @Override
      public String getCharacterEncoding() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getContentType() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public ServletOutputStream getOutputStream() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public PrintWriter getWriter() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public void setCharacterEncoding(String charset) {
        // TODO Auto-generated method stub

      }

      @Override
      public void setContentLength(int len) {
        // TODO Auto-generated method stub

      }

      @Override
      public void setContentLengthLong(long len) {
        // TODO Auto-generated method stub

      }

      @Override
      public void setContentType(String type) {
        // TODO Auto-generated method stub

      }

      @Override
      public void setBufferSize(int size) {
        // TODO Auto-generated method stub

      }

      @Override
      public int getBufferSize() {
        // TODO Auto-generated method stub
        return 0;
      }

      @Override
      public void flushBuffer() {
        // TODO Auto-generated method stub

      }

      @Override
      public void resetBuffer() {
        // TODO Auto-generated method stub

      }

      @Override
      public boolean isCommitted() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public void reset() {
        // TODO Auto-generated method stub

      }

      @Override
      public void setLocale(Locale loc) {
        // TODO Auto-generated method stub

      }

      @Override
      public Locale getLocale() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public void addCookie(Cookie cookie) {
        // TODO Auto-generated method stub

      }

      @Override
      public boolean containsHeader(String name) {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public String encodeURL(String url) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String encodeRedirectURL(String url) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public void sendError(int sc, String msg) {
        // TODO Auto-generated method stub

      }

      @Override
      public void sendError(int sc) {
        // TODO Auto-generated method stub

      }

      @Override
      public void sendRedirect(String location) {
        // TODO Auto-generated method stub

      }

      @Override
      public void setDateHeader(String name, long date) {
        // TODO Auto-generated method stub

      }

      @Override
      public void addDateHeader(String name, long date) {
        // TODO Auto-generated method stub

      }

      @Override
      public void setHeader(String name, String value) {
        // TODO Auto-generated method stub

      }

      @Override
      public void addHeader(String name, String value) {
        // TODO Auto-generated method stub

      }

      @Override
      public void setIntHeader(String name, int value) {
        // TODO Auto-generated method stub

      }

      @Override
      public void addIntHeader(String name, int value) {
        // TODO Auto-generated method stub

      }

      @Override
      public void setStatus(int sc) {
        status = sc;
      }

      @Override
      public int getStatus() {
        return status;
      }

      @Override
      public String getHeader(String name) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Collection<String> getHeaders(String name) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Collection<String> getHeaderNames() {
        // TODO Auto-generated method stub
        return null;
      }
    };
  }

  protected static HttpServletRequest getMockRequest(String uri, String method) {
    return new HttpServletRequest() {
      private final Map<String, String[]> params = new HashMap<>();

      @Override
      public String getRequestId() {
        return null;
      }

      @Override
      public String getProtocolRequestId() {
        return null;
      }

      @Override
      public jakarta.servlet.ServletConnection getServletConnection() {
        return null;
      }

      public Map<String, String[]> getParameterMap() {
        return params;
      }

      public String getParameter(String name) {
        String[] matches = params.get(name);
        if (matches == null || matches.length == 0) return null;
        return matches[0];
      }

      @Override
      public Object getAttribute(String arg0) {
        return null;
      }

      @Override
      public Enumeration<String> getAttributeNames() {
        return null;
      }

      @Override
      public String getCharacterEncoding() {
        return null;
      }

      @Override
      public int getContentLength() {
        return 0;
      }

      @Override
      public String getContentType() {
        return null;
      }

      @Override
      public ServletInputStream getInputStream() {
        return null;
      }

      @Override
      public String getLocalAddr() {
        return null;
      }

      @Override
      public String getLocalName() {
        return null;
      }

      @Override
      public int getLocalPort() {
        return 0;
      }

      @Override
      public Locale getLocale() {
        return null;
      }

      @Override
      public Enumeration<Locale> getLocales() {
        return null;
      }

      @Override
      public Enumeration<String> getParameterNames() {
        return null;
      }

      @Override
      public String[] getParameterValues(String arg0) {
        return null;
      }

      @Override
      public String getProtocol() {
        return null;
      }

      @Override
      public BufferedReader getReader() {
        return null;
      }

      @Override
      public String getRemoteAddr() {
        return null;
      }

      @Override
      public String getRemoteHost() {
        return null;
      }

      @Override
      public int getRemotePort() {
        return 0;
      }

      @Override
      public RequestDispatcher getRequestDispatcher(String arg0) {
        return null;
      }

      @Override
      public String getScheme() {
        return null;
      }

      @Override
      public String getServerName() {
        return null;
      }

      @Override
      public int getServerPort() {
        return 0;
      }

      @Override
      public boolean isSecure() {
        return false;
      }

      @Override
      public void removeAttribute(String arg0) {}

      @Override
      public void setAttribute(String arg0, Object arg1) {}

      @Override
      public void setCharacterEncoding(String arg0) {}

      @Override
      public String getAuthType() {
        return null;
      }

      @Override
      public String getContextPath() {
        return null;
      }

      @Override
      public Cookie[] getCookies() {
        return null;
      }

      @Override
      public long getDateHeader(String arg0) {
        return 0;
      }

      @Override
      public String getHeader(String arg0) {
        return null;
      }

      @Override
      public Enumeration<String> getHeaderNames() {
        return null;
      }

      @Override
      public Enumeration<String> getHeaders(String arg0) {
        return null;
      }

      @Override
      public int getIntHeader(String arg0) {
        return 0;
      }

      @Override
      public String getMethod() {
        return method;
      }

      @Override
      public String getPathInfo() {
        return null;
      }

      @Override
      public String getPathTranslated() {
        return null;
      }

      @Override
      public String getQueryString() {
        return null;
      }

      @Override
      public String getRemoteUser() {
        return null;
      }

      @Override
      public String getRequestURI() {
        return uri;
      }

      @Override
      public StringBuffer getRequestURL() {
        return null;
      }

      @Override
      public String getRequestedSessionId() {
        return null;
      }

      @Override
      public String getServletPath() {
        return null;
      }

      @Override
      public HttpSession getSession() {
        return null;
      }

      @Override
      public HttpSession getSession(boolean arg0) {
        return null;
      }

      @Override
      public Principal getUserPrincipal() {
        return () -> "test@example.com";
      }

      @Override
      public boolean isRequestedSessionIdFromCookie() {
        return false;
      }

      @Override
      public boolean isRequestedSessionIdFromURL() {
        return false;
      }

      @Override
      public boolean isRequestedSessionIdValid() {
        return false;
      }

      @Override
      public boolean isUserInRole(String arg0) {
        return true;
      }

      @Override
      public long getContentLengthLong() {
        // TODO Auto-generated method stub
        return 0;
      }

      @Override
      public ServletContext getServletContext() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public AsyncContext startAsync() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
          throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public boolean isAsyncStarted() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public boolean isAsyncSupported() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public AsyncContext getAsyncContext() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public DispatcherType getDispatcherType() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String changeSessionId() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public boolean authenticate(HttpServletResponse response) {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public void login(String username, String password) {
        // TODO Auto-generated method stub

      }

      @Override
      public void logout() {
        // TODO Auto-generated method stub

      }

      @Override
      public Collection<Part> getParts() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Part getPart(String name) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
        // TODO Auto-generated method stub
        return null;
      }
    };
  }
}
