package com.xiaoji.duan.zuulserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xiaoji.duan.zuulserver.service.RefreshRouteService;

import java.util.Map;

@RestController
public class RefreshController {
    @Autowired
    RefreshRouteService refreshRouteService;

    @Autowired
    ZuulHandlerMapping zuulHandlerMapping;

    @GetMapping("/refreshRoute")
    public String refresh(){
        refreshRouteService.refreshRoute();
        return "refresh success";
    }

    @GetMapping("/watchRoute")
    public Object watchNowRoute(){
        Map<String,Object> handlerMap = zuulHandlerMapping.getHandlerMap();
        return handlerMap;
    }
}
