package io.github.xsession.example.web;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class XSessionTestController {
	
	static int sleepMilliSeconds = 50;
	
	@RequestMapping("/setSleepMilliSeconds")
	@ResponseBody
	public String setSleepMilliSeconds(HttpServletRequest request, HttpServletResponse response, @RequestParam int sleepMilliSeconds) {
		XSessionTestController.sleepMilliSeconds = sleepMilliSeconds;
		return "set success, sleepMilliSeconds:" + sleepMilliSeconds;
	}
	
	@RequestMapping("/nosession")
	@ResponseBody
	public String nosession(HttpServletRequest request, HttpServletResponse response) throws InterruptedException {
		System.out.print("nosession");
		TimeUnit.MILLISECONDS.sleep(sleepMilliSeconds);
		return "nosession";
	}
	
	@RequestMapping("/tomcatsession")
	@ResponseBody
	public String tomcatsession(HttpServletRequest request, HttpServletResponse response) throws InterruptedException {
		HttpSession session = request.getSession();
		if (session != null) {
			session.setAttribute("hello", "world");
//			System.out.println("session:" + session.getClass());
		}else {
			System.err.println("session is null");
		}
		TimeUnit.MILLISECONDS.sleep(sleepMilliSeconds);
		return "tomcatsession";
	}
	
	@RequestMapping("/xsession")
	@ResponseBody
	public String testSession(HttpServletRequest request, HttpServletResponse response) throws InterruptedException {
		HttpSession session = request.getSession();
		if (session != null) {
			session.setAttribute("hello", "world");
//			System.out.println("session:" + session.getClass());
		}else {
			System.err.println("session is null");
		}
		
		TimeUnit.MILLISECONDS.sleep(sleepMilliSeconds);
		return "xsession";
	}
}
