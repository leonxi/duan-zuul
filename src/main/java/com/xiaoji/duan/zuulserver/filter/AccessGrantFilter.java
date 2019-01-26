package com.xiaoji.duan.zuulserver.filter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;

import com.alibaba.fastjson.JSONObject;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.xiaoji.duan.zuulserver.service.HttpService;

public class AccessGrantFilter extends ZuulFilter {

    @Autowired
    private HttpService httpgrant;
    
	@Override
	public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        String upgradeHeader = ctx.getRequest().getHeader("Upgrade");
		if (null == upgradeHeader) {
			upgradeHeader = ctx.getRequest().getHeader("upgrade");
		}
		return !ctx.getBoolean("authorizing") 
				&& !(null != upgradeHeader && "websocket".equalsIgnoreCase(upgradeHeader))
				&& !ctx.getRequest().getRequestURI().startsWith("/abb/")
        		&& !ctx.getRequest().getRequestURI().endsWith("/shortapplication.json")
//				&& !ctx.getRequest().getRequestURI().endsWith(".php")
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

    @Value("${zuul.authorize.url}")
    private URL authurl;
    @Value("${zuul.authorize.path}")
    private String authpath;

    @Value("${zuul.grant.url}")
    private URL granturl;
    @Value("${zuul.grant.path}")
    private String grantpath;

	@Override
	public Object run() throws ZuulException {
		
        RequestContext ctx = RequestContext.getCurrentContext();
        String host = ctx.getRequest().getServerName();
        String subdomain = host.substring(0, host.indexOf('.'));
        String requestURI = ctx.getRequest().getRequestURI();
        String prefix = "/";

        if (requestURI.indexOf('/', 1) > 3)
        	prefix = requestURI.substring(1, requestURI.indexOf('/', 1));

        Map<String, String[]> data = new HashMap<String, String[]>();

        boolean authorized = ctx.getBoolean("authorized");
        
        if (authorized) {
	        JSONObject user = (JSONObject) ctx.get("authorized_userid");
	
	        data.put("openid", new String[]{user.getString("openid")});
	        data.put("name", new String[]{user.getString("nickname")});
	        data.put("uri", new String[]{requestURI});
        } else {
	        data.put("uri", new String[]{requestURI});
        }
        
        Map<String, Object> resp = null;
        if (!"/".equals(prefix))
        	resp = httpgrant.https(granturl + "/" + subdomain + "/" + prefix + "/accessable", data);

        if (resp != null && !ctx.getRequest().getRequestURI().startsWith("/abd/" + subdomain + "/")) {
        	if (authorized)
        		System.out.println("Zuul Gateway Request URI [" + ctx.getRequest().getRequestURI() + "] has grant info! " + ctx.get("authorized_userid") + " <=> " + resp);
        	else
        		System.out.println("Zuul Gateway Request URI [" + ctx.getRequest().getRequestURI() + "] has grant info! " + resp);
        	boolean isLogin = Boolean.valueOf(((Map<String, Object>) resp.get("data")).get("tologin").toString());
        	boolean isAccessable = Boolean.valueOf(((Map<String, Object>) resp.get("data")).get("accessable").toString());
        	boolean isGroupUserApply = Boolean.valueOf(((Map<String, Object>) resp.get("data")).get("groupuser_apply").toString());
        	boolean isGroupSAUserApply = Boolean.valueOf(((Map<String, Object>) resp.get("data")).get("groupsauser_apply").toString());
        	String verifyType = "WEIXIN";
        	if (isLogin) {
        		verifyType = ((Map<String, Object>) resp.get("data")).get("verifyType").toString();
        	}
        	
        	String redirect = ctx.getRequest().getScheme() + "://" + host + "/abd";

        	if (!isAccessable && isGroupUserApply) {
        		String token = (String) ctx.get("authorized_user");
        		URL redirectURL = null;
				try {
					redirectURL = new URL(redirect + "/" + subdomain + "/apply?token=" + token);
				} catch (MalformedURLException e1) {
					e1.printStackTrace();
				}
    			ctx.setRouteHost(redirectURL);
                ctx.setSendZuulResponse(false);
                try {
    				ctx.getResponse().sendRedirect(redirect + "/" + subdomain + "/apply?token=" + token);
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
        		return null;
        	}

        	if (!isAccessable && isGroupSAUserApply) {
        		String token = (String) ctx.get("authorized_user");
        		URL redirectURL = null;
				try {
					redirectURL = new URL(redirect + "/" + subdomain + "/" + prefix + "/apply?token=" + token);
				} catch (MalformedURLException e1) {
					e1.printStackTrace();
				}
    			ctx.setRouteHost(redirectURL);
                ctx.setSendZuulResponse(false);
                try {
    				ctx.getResponse().sendRedirect(redirect + "/" + subdomain + "/" + prefix + "/apply?token=" + token);
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
        		return null;
        	}

        	if (!isLogin && !isAccessable && !isGroupUserApply && !isGroupSAUserApply) {
        		ctx.setSendZuulResponse(false);
        		ctx.setResponseStatusCode(401);
        		return null;
        	}
        	
        	if (isLogin) {
        		  // 过滤该请求
            	data = ctx.getRequest().getParameterMap();
            	if ("www".equals(subdomain)) {
            		data.put("redirect_url", new String[]{ctx.getRequest().getRequestURL().toString()});
            	} else {
            		String redir = "https://www.guobaa.com/abb/" + Base64.encodeBase64URLSafeString(ctx.getRequest().getRequestURL().toString().getBytes());
            		data.put("redirect_url", new String[]{redir});
            	}
            	data.put("verifyType", new String[]{verifyType});
            	resp = httpgrant.https(authurl + "/login", data);

            	URL url = null;
    			String path = ((Map<String, String>) resp.get("data")).get("login_url");
    			try {
    				url = new URL(path);
    			} catch (MalformedURLException e) {
    				e.printStackTrace();
    			}

    			ctx.setRouteHost(url);
                ctx.setSendZuulResponse(false);
                try {
    				ctx.getResponse().sendRedirect(path);
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
                return null;
            
        	}
        } else {
            System.out.println("Zuul Gateway Request URI [" + ctx.getRequest().getRequestURI() + "] has not grant info!");
//            if ("/".equals(prefix)) {
//                System.out.println(ctx.getRouteHost().toString());
//            	URL welcomepage = ctx.getRouteHost();
//				try {
//					welcomepage = new URL(ctx.getRouteHost().toString() + "index");
//				} catch (MalformedURLException e) {
//					e.printStackTrace();
//				}
//            	ctx.setRouteHost(welcomepage);
//            	ctx.set(key, value);
//            }
        }
        
		return null;
	}

	@Override
	public String filterType() {
		return FilterConstants.PRE_TYPE;
	}

	@Override
	public int filterOrder() {
		return 2;
	}

}
