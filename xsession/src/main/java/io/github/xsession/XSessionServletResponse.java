package io.github.xsession;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

/**
 * 当request完成时，返回内容给客户端时，一定会调用{{@link #getOutputStream()} 或者 {
 * {@link #getWriter()}函数。 在这里把session保存到缓存服务器中，并把sessionId写到cookie里。 如果在
 * {@link javax.servlet.ServletRequestListener#requestDestroyed(javax.servlet.ServletRequestEvent)}
 * 函数里保存session，则太晚了，因为这时请求已经返回给浏览器时。
 * 
 * @author hengyunabc
 * 
 */
public class XSessionServletResponse extends HttpServletResponseWrapper {

	String sessionCookieName = XSessionFilter.DEFAULT_SESSION_COOKIE;
	boolean sessionCookieHttpOnly = true;
	int sessionCookieExpires = XSessionFilter.DEFAULT_SESSIONCOOKIEEXPIRES;

	XSessionServletRequest request;
	SessionStore sessionStore;

	boolean bAddCookieFlag = false;

	public XSessionServletResponse(HttpServletResponse response) {
		super(response);
	}

	// TODO 这个是否是必要的？
	// public void setSession(XSession session) {
	// this.session = session;
	// }

	public void setXSessionServletRequest(XSessionServletRequest request) {
		this.request = request;
	}

	public void setSessionCookieName(String sessionCookieName) {
		this.sessionCookieName = sessionCookieName;
	}

	public void setSessionStore(SessionStore sessionStore) {
		this.sessionStore = sessionStore;
	}

	public void setSessionCookieHttpOnly(boolean sessionCookieHttpOnly) {
		this.sessionCookieHttpOnly = sessionCookieHttpOnly;
	}

	public void setSessionCookieExpires(int sessionCookieExpires) {
		this.sessionCookieExpires = sessionCookieExpires;
	}

	/**
	 * 把sessionId设置到cookie里
	 * 
	 * @param session
	 */
	private void addCookie(HttpSession session) {
		// session从缓存加载的，不算是new的，则不用addCookie
		if (!bAddCookieFlag && session.isNew()) {
			Cookie cookie = new Cookie(sessionCookieName, session.getId());
			// TODO 是否要支持设置为session？
			cookie.setMaxAge(sessionCookieExpires);
			if (sessionCookieHttpOnly) {
				cookie.setHttpOnly(sessionCookieHttpOnly);
			}
			addCookie(cookie);
			bAddCookieFlag = true;
		}
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		HttpSession session = request.getSession(false);
		if (session != null) {
			sessionStore.commit((XSession) session);
			addCookie(session);
		}

		return super.getOutputStream();
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		HttpSession session = request.getSession(false);
		if (session != null) {
			sessionStore.commit((XSession) session);
			addCookie(session);
		}
		return super.getWriter();
	}

}
