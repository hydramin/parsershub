package com.parsehub.proxy.controller;

import com.parsehub.proxy.service.ProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping("proxy")
public class ProxyController {

    @Autowired
    private ProxyService proxyService;

    @GetMapping("/**")
    public ResponseEntity<String> proxyGetExecutor(HttpServletRequest request) {
        return proxyService.proxyGetService(request);
    }

//    @PostMapping("/**")
//    public String proxyPostExecutor(HttpServletRequest request) throws IOException, ServletException {
//        return proxyService.proxyPostService(request);
//    }

    @PostMapping("/**")
    public ResponseEntity<String> proxyPostExecutor(HttpServletRequest request) throws IOException, ServletException {
        return proxyService.proxyPostService(request);
    }

}
