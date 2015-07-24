package cn.godzilla.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.context.support.WebApplicationContextUtils;

import cn.godzilla.common.BusinessException;
import cn.godzilla.common.ReturnCodeEnum;
import cn.godzilla.service.UserService;
import cn.godzilla.web.SuperController;

/**
 * 身份验证
 * 
 * @author 201407280166
 *
 */
public class Authentication extends SuperController implements Filter {

	private final Logger logger = LogManager.getLogger(Authentication.class);
	
	private ServletContext context;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		logger.info("Authentication init");
		escapeUrls.add("/user/welcome");
		escapeUrls.add("/user/login");
		
		context = filterConfig.getServletContext();  
		applicationContext = WebApplicationContextUtils.getWebApplicationContext(context); 
		userService = (UserService)applicationContext.getBean("userService");
        
        //两种获取方式都报错  
        
       /* ApplicationContext ac = new FileSystemXmlApplicationContext("classpath:applicationContext.xml");
        userService = (UserService)ac.getBean("userService"); */
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException, BusinessException {
		logger.info("authentication");
		HttpServletResponse resp = (HttpServletResponse) response;
		HttpServletRequest req = (HttpServletRequest)request;
		
		if(!this.escapeUrl(request)) {
			String sid = this.getSidFromUrl(request);
			ReturnCodeEnum userStatus = this.checkUser(userService, sid);
			switch(userStatus) {
			case NO_LOGIN:
				String prefix = req.getContextPath();
				resp.sendRedirect(prefix+"/user/welcome.do");
				return ;
			case OK_CHECKUSER:
				this.initContext(userService, sid); //将sid保存到 threadlocal
			}
		}
		chain.doFilter(request, response);
		distroyContext(); //清空 threadlocal
	}
	
	private boolean escapeUrl(ServletRequest request) {
		String pathInfo = ((HttpServletRequest)request).getContextPath()  + ((HttpServletRequest)request).getRequestURI() + (((HttpServletRequest)request).getPathInfo()==null ? "":((HttpServletRequest)request).getPathInfo());
		logger.info(">>>|>>request url : " + pathInfo);
		for(String escapeUrl:escapeUrls) {
			if(pathInfo.contains(escapeUrl)) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * url 的 第二个字符为 sid   例如 请求为   /godzilla-web/usr/122334/getUser.do?XX
	 * @param request
	 * @return sid
	 * @throws Exception 
	 */
	private String getSidFromUrl(ServletRequest request) throws BusinessException {
		String pathInfo = ((HttpServletRequest)request).getRequestURI();
		
		int end = pathInfo.lastIndexOf("/");
		if(end <0) 
			throw new BusinessException("url is wrong");
		int offset = pathInfo.lastIndexOf("/", end-1);
		if(offset <0) 
			throw new BusinessException("url is wrong");
		String sid = pathInfo.substring(offset+1, end);
		logger.info(">>>|>>request sid : " + sid);
		return sid;
	}

	@Override
	public void destroy() {

	}
}
