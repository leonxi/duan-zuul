package com.xiaoji.duan.zuulserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.netflix.zuul.filters.RefreshableRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomRouteLocator extends SimpleRouteLocator implements RefreshableRouteLocator {

    public final static Logger logger = LoggerFactory.getLogger(CustomRouteLocator.class);

    private JdbcTemplate jdbcTemplate;

    private ZuulProperties properties;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public CustomRouteLocator(String servletPath, ZuulProperties properties) {
        super(servletPath, properties);
        this.properties = properties;
        logger.info("servletpath:{}",servletPath);
    }

    @Override
    public void refresh() {
        doRefresh();
    }

    @Override
    protected Map<String,ZuulProperties.ZuulRoute> locateRoutes(){
        LinkedHashMap<String,ZuulProperties.ZuulRoute> routesMap = new LinkedHashMap<>();
        // 从application.properties中加载路由信息
        routesMap.putAll(super.locateRoutes());
        // TODO 从db中加载路由信息
        routesMap.putAll(locateRoutesFromBD());
        // 优化配置
        LinkedHashMap<String,ZuulProperties.ZuulRoute> values = new LinkedHashMap<>();
        for(Map.Entry<String,ZuulProperties.ZuulRoute> enty : routesMap.entrySet()){
            String path = enty.getKey();
            if(!path.startsWith("/")){
                path = "/" + path;
            }
            if(StringUtils.hasText(this.properties.getPrefix())){
                path = this.properties.getPrefix() + path;
                if(!path.startsWith("/")){
                    path = "/" + path;
                }
            }
            values.put(path,enty.getValue());
        }
        return values;
    }

    private Map<String,ZuulProperties.ZuulRoute> locateRoutesFromBD(){
        Map<String,ZuulProperties.ZuulRoute> routes = new LinkedHashMap<>();
        // TODO SQL
        List<ZuulRouteVO> results = jdbcTemplate.query("select * from gateway_api_define where enabled = true ",new BeanPropertyRowMapper<>(ZuulRouteVO.class));
        for(ZuulRouteVO result : results){
            if(StringUtils.isEmpty(result.getPath())){
                continue;
            }
            if(StringUtils.isEmpty(result.getServiceid()) && StringUtils.isEmpty(result.getUrl())){
                continue;
            }
            ZuulProperties.ZuulRoute zuulRoute = new ZuulProperties.ZuulRoute();
            try {
                BeanUtils.copyProperties(result,zuulRoute);
            } catch (Exception e){
                logger.error("=============load zuul route info from db with error==============",e);
            }
            routes.put(zuulRoute.getPath(),zuulRoute);
        }
        return routes;
    }

    public static class ZuulRouteVO{
        private String id;
        private String path;
        private String serviceid;
        private String url;
        private boolean stripPrefix = true;
        private Boolean retryable;
        private Boolean enabled;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getServiceid() {
            return serviceid;
        }

        public void setServiceid(String serviceid) {
            this.serviceid = serviceid;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean isStripPrefix() {
            return stripPrefix;
        }

        public void setStripPrefix(boolean stripPrefix) {
            this.stripPrefix = stripPrefix;
        }

        public Boolean getRetryable() {
            return retryable;
        }

        public void setRetryable(Boolean retryable) {
            this.retryable = retryable;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}
