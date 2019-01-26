package com.xiaoji.duan.zuulserver.filter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

public class AuthorizeFilter extends ZuulFilter {

	@Override
	public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        String upgradeHeader = ctx.getRequest().getHeader("Upgrade");
		if (null == upgradeHeader) {
			upgradeHeader = ctx.getRequest().getHeader("upgrade");
		}
		return ctx.getBoolean("authorized")
			&& !(null != upgradeHeader && "websocket".equalsIgnoreCase(upgradeHeader))
    		&& !ctx.getRequest().getRequestURI().endsWith("/shortapplication.json")
//			&& !ctx.getRequest().getRequestURI().endsWith(".php")
			&& !ctx.getRequest().getRequestURI().endsWith(".js")
			&& !ctx.getRequest().getRequestURI().endsWith(".ico")
			&& !ctx.getRequest().getRequestURI().endsWith(".png")
    		&& !ctx.getRequest().getRequestURI().endsWith(".jpeg")
    		&& !ctx.getRequest().getRequestURI().endsWith(".gif")
			&& !ctx.getRequest().getRequestURI().endsWith(".jpg")
			&& !ctx.getRequest().getRequestURI().endsWith(".css")
    		&& !ctx.getRequest().getRequestURI().endsWith(".woff2")
    		&& !ctx.getRequest().getRequestURI().endsWith(".woff")
    		&& !ctx.getRequest().getRequestURI().endsWith(".ttf")
    		&& !ctx.getRequest().getRequestURI().endsWith(".map")
			&& (ctx.getRequest().getRequestURI().indexOf('/', 1) > 0 ? ctx.getRequest().getRequestURI().substring(1, ctx.getRequest().getRequestURI().indexOf('/', 1)).length() == 3 : true);
	}

	@Override
	public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();

        String token = (String) ctx.get("authorized_user");
        String openid = (String) ctx.get("authorized_openid");
        
        if (!ctx.getBoolean("hasAuthorizedCookie")) {
        	Cookie authorized_userCookie = new Cookie("authorized_user", token);
        	authorized_userCookie.setPath("/");
        	ctx.getResponse().addCookie(authorized_userCookie);

        	Cookie authorized_openidCookie = new Cookie("authorized_openid", openid);
        	authorized_openidCookie.setPath("/");
        	ctx.getResponse().addCookie(authorized_openidCookie);
        } else {
            HttpServletRequest req = ctx.getRequest();
            Cookie[] cookies = req.getCookies();
            String origintoken = "";
            
        	if (cookies != null) {
	        	for (Cookie cookie : cookies) {
	        	    switch(cookie.getName()){
	        	        case "authorized_user":
	        	        	origintoken = cookie.getValue();
	        	        default:
	        	            break;
	        	    }
	        	}
	        	
	        	if (!token.equals(origintoken)) {
	            	Cookie authorized_userCookie = new Cookie("authorized_user", token);
	            	authorized_userCookie.setPath("/");
	            	authorized_userCookie.setMaxAge(7200);
	            	ctx.getResponse().addCookie(authorized_userCookie);

	            	Cookie authorized_openidCookie = new Cookie("authorized_openid", openid);
	            	authorized_openidCookie.setPath("/");
	            	authorized_openidCookie.setMaxAge(7200);
	            	ctx.getResponse().addCookie(authorized_openidCookie);
	        	}
        	}
        }

        System.out.println("Zuul Gateway Request URI [" + ctx.getRequest().getRequestURI() + "] has authorized!");
        
		return null;
	}

	@Override
	public String filterType() {
		return FilterConstants.POST_TYPE;
	}

	@Override
	public int filterOrder() {
		return 1;
	}

}
