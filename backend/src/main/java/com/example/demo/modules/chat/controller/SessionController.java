package com.example.demo.modules.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RestController
@RequestMapping("/api")
public class SessionController {
	
    @GetMapping("/create")
    public ResponseEntity<String> cn1(HttpSession session){
    	session.setAttribute("data","Session Value1" );
    	System.out.println("create session data is Session Value1");
    	return ResponseEntity.ok("Session Value1");
    }
    @GetMapping("/get")
    public ResponseEntity<String> cn2(HttpSession session){
    	Object obj=session.getAttribute("data");
    	System.out.println("get session data is "+obj);
    	String msg=""+obj;
    	return ResponseEntity.ok(msg);
    }
}
