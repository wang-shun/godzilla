package cn.godzilla.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import cn.godzilla.dao.ClientConfigMapper;
import cn.godzilla.service.FunRightService;
import cn.godzilla.service.UserService;
import cn.godzilla.web.GodzillaApplication;
import cn.godzilla.web.WebsocketController;

/**
 * 身份验证
 * 
 * @author 201407280166
 *
 */
public class Authentication extends GodzillaApplication implements Filter {

	private final Logger logger = LogManager.getLogger(Authentication.class);
	
	
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		logger.info("Authentication init身份验证");
		escapeUrls.add("/user/welcome");
		escapeUrls.add("/user/login");
		
		context = filterConfig.getServletContext();  
		applicationContext = WebApplicationContextUtils.getWebApplicationContext(context); 
		userService = (UserService)applicationContext.getBean("userService");
		funRightService = (FunRightService)applicationContext.getBean("funRightService");
        //两种获取方式都报错  
       /* ApplicationContext ac = new FileSystemXmlApplicationContext("classpath:applicationContext.xml");
        userService = (UserService)ac.getBean("userService"); */
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		logger.info("authentication");
		HttpServletResponse resp = (HttpServletResponse) response;
		HttpServletRequest req = (HttpServletRequest)request;
		
		try {
			if(!this.escapeUrl(request)) {
				String sid = super.getSidFromUrl(request);
				ReturnCodeEnum userStatus = this.checkUser(userService, sid);
				logger.info(">>>|>>request sid : " + sid);
				if(userStatus == ReturnCodeEnum.NO_LOGIN) {
					throw new BusinessException("还未登录或sid失效");
				} else if(userStatus == ReturnCodeEnum.OK_CHECKUSER) {
					this.initContext(userService, sid); //将sid保存到 threadlocal
				}
			}
		} catch(BusinessException e1) {
			logger.error(e1.getMessage());
			String prefix = req.getContextPath();
			resp.sendRedirect(prefix+"/user/welcome.do");
			return ;
		}
		chain.doFilter(request, response);
		distroyContext(); //清空 threadlocal
	}
	

	@Override
	public void destroy() {

	}
}
