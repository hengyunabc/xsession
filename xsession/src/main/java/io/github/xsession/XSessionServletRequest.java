package io.github.xsession;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

public class XSessionServletRequest extends HttpServletRequestWrapper {
	HttpSession session = null;

	SessionStore sessionStore;
	String sessionCookieName = XSessionFilter.DEFAULT_SESSION_COOKIE;

	int sessionMaxInactiveInterval = XSession.DEFAULT_MAXINACTIVEINTERVAL;

	public XSessionServletRequest(HttpServletRequest request) {
		super(request);
	}

	// public XSessionServletRequest(HttpServletRequest request,
	// SessionStore sessionStore, String sessionCookieName) {
	// this(request);
	// this.sessionStore = sessionStore;
	// this.sessionCookieName = sessionCookieName;
	// }

	public void setSessionStore(SessionStore sessionStore) {
		this.sessionStore = sessionStore;
	}

	public void setSessionCookieName(String sessionCookieName) {
		this.sessionCookieName = sessionCookieName;
	}

	public void setSessionMaxInactiveInterval(int sessionMaxInactiveInterval) {
		this.sessionMaxInactiveInterval = sessionMaxInactiveInterval;
	}

	@Override
	public HttpSession getSession(boolean create) {
		if (create) {
			return getSession();
		}
		return session;
	}

	@Override
	public HttpSession getSession() {
		if (session == null) {
			// 从Cookie中取出sessionId，再从缓存服务器中取出对应的session
			Cookie[] cookies = this.getCookies();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					if (cookie.getName().equals(sessionCookieName)) {
						XSession xSession = sessionStore.loadSession(cookie.getValue());
						if (xSession != null) {
							this.session = xSession;
						}
					}
				}
			}
		}
		//cookie中没有sessionId，或者从缓存服务器中查找不到，则创建新的Session
		if (session == null) {
			// TODO 这里初始化还没有完成
			this.session = new XSession(true);
			this.session.setMaxInactiveInterval(sessionMaxInactiveInterval);
		}
		return session;
	}

}
