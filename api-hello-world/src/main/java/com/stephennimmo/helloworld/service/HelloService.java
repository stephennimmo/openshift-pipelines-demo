package com.stephennimmo.helloworld.service;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HelloService {

    public String hello() {
        return "Hello, World!";
    }

}
