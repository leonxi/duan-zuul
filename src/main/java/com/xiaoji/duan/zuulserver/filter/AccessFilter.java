package com.xiaoji.duan.zuulserver.filter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.xiaoji.duan.zuulserver.service.HttpService;

public class AccessFilter extends ZuulFilter {
    private static Logger logger = LoggerFactory.getLogger(AccessFilter.class);
    
    @Autowired
    private HttpService httpauth;
    
    @Override
    /**
     * 返回一个字符串代表过滤器的类型
     *      pre ： 可以在请求被路由之前调用
     *      routing ： 在路由请求时被调用
     *      post : 在routing和error过滤器之后被调用
     *      error : 处理请求时发生错误时被调用
     */
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    /**
     * 通过int值来定义过滤器的执行顺序
     */
    public int filterOrder() {
        return 1;
    }

    @Override
    /**
     * 返回一个boolean类型来判断该过滤器是否要执行
     */
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        String upgradeHeader = ctx.getRequest().getHeader("Upgrade");
		if (null == upgradeHeader) {
			upgradeHeader = ctx.getRequest().getHeader("upgrade");
		}
        return !ctx.getRequest().getRequestURI().endsWith(".js")
    			&& !(null != upgradeHeader && "websocket".equalsIgnoreCase(upgradeHeader))
        		&& !ctx.getRequest().getRequestURI().endsWith("/shortapplication.json")
        		&& !ctx.getRequest().getRequestURI().endsWith(".ico")
        		&& !ctx.getRequest().getRequestURI().endsWith(".png")
        		&& !ctx.getRequest().getRequestURI().endsWith(".jpg")
        		&& !ctx.getRequest().getRequestURI().endsWith(".jpeg")
        		&& !ctx.getRequest().getRequestURI().endsWith(".gif")
        		&& !ctx.getRequest().getRequestURI().endsWith(".css")
        		&& !ctx.getRequest().getRequestURI().endsWith(".woff2")
        		&& !ctx.getRequest().getRequestURI().endsWith(".woff")
        		&& !ctx.getRequest().getRequestURI().endsWith(".ttf")
        		&& !ctx.getRequest().getRequestURI().endsWith(".map")
				&& (ctx.getRequest().getRequestURI().indexOf('/', 1) > 0 ? ctx.getRequest().getRequestURI().substring(1, ctx.getRequest().getRequestURI().indexOf('/', 1)).length() == 3 : true);
    }

    @Value("${zuul.authorize.url}")
    private URL authurl;
    @Value("${zuul.authorize.path}")
    private String authpath;
    
    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        boolean isThirdPartyApps = request.getRequestURI().endsWith(".php");
        
        // 获取认证用户
        String token = request.getParameter("token");
        if (token == null || "".equals(token) || isThirdPartyApps) {
        	Cookie[] cookies = request.getCookies();
        	if (cookies != null) {
	        	for (Cookie cookie : cookies) {
	        	    switch(cookie.getName()){
	        	        case "authorized_user":
	        	        	ctx.set("hasAuthorizedCookie", true);
	        	            token = cookie.getValue();
	        	        default:
//	        	        	System.out.println("Cookie " + cookie.getName() + " = " + cookie.getValue());
	        	            break;
	        	    }
	        	}
        	}
        }

        // 获取强制登录代码
        String code = request.getParameter("code");
        if (!StringUtils.isEmpty(code) && !isThirdPartyApps) {
        	String state = request.getParameter("state");

        	String reqUri = ctx.getRequest().getRequestURI();
        	String originUrl = "";
        	if (reqUri.contains("/abb/")) {
        		String[] paths = reqUri.split("/");
        		
        		String base64 = paths[paths.length - 1];
        		
        		if (base64 == null || "".equals(base64)) {
        			base64 = paths[paths.length - 2];
        		}
        		
        		if (!"abb".equals(base64) && Base64.isBase64(base64)) {
        			originUrl = new String(Base64.decodeBase64(base64));
        			originUrl = originUrl + "?code=" + code + "&state=" + state;
        			
        			try {
	        			ctx.setRouteHost(new URL(originUrl.toString()));
	                    ctx.setSendZuulResponse(false);
	                    ctx.getResponse().sendRedirect(originUrl.toString());
        			} catch (IOException e) {
        				e.printStackTrace();
        			}
        			
        			return null;
        		}
        	}
            System.out.println("Zuul Gateway Request URI [" + ctx.getRequest().getRequestURI() + "] has code to authorize!");

        	Map<String, String[]> data = request.getParameterMap();
        	Map<String, Object> resp = httpauth.https(authurl + "/" + state + "/login", data);
        	
        	if (resp != null & resp.get("data") != null) {
            	JSONObject userjson = (JSONObject) resp.get("data");
            	StringBuffer url = request.getRequestURL();
            	url.append("?token=" + userjson.getString("access_token"));
            	
                try {
                	ctx.set("authorizing", true);
        			ctx.setRouteHost(new URL(url.toString()));
                    ctx.setSendZuulResponse(false);
                    ctx.getResponse().sendRedirect(url.toString());
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
                return null;
        	} else {
        		// 未取得Token
        	}
        } else if(!StringUtils.isEmpty(token)){
            System.out.println("Zuul Gateway Request URI [" + ctx.getRequest().getRequestURI() + "] has authorized token!");
        	Map<String, String[]> data = request.getParameterMap();

        	data.put("access_token", new String[]{token});
        	Map<String, Object> resp = httpauth.https(authurl + "/islogin", data);
        	ctx.set("authorized", true);

        	if (resp != null && resp.get("data") != null) {
            	System.out.println(resp.toString());
        		JSONObject user = (JSONObject) resp.get("data");
        		
            	ctx.set("authorized_openid", user.getString("openid"));
            	Map<String, List<String>> querys = ctx.getRequestQueryParams();
            	if (querys != null && querys.containsKey("token") && !querys.containsKey("openid")) {
	            	List<String> lopenid = new ArrayList<String>();
	            	lopenid.add(user.getString("openid"));
	            	querys.put("openid", lopenid);
	            	ctx.setRequestQueryParams(querys);
            	}
        	}
        	ctx.set("authorized_userid", resp.get("data"));
        	ctx.set("authorized_user", token);
        }
        
        return null;
    }

}
