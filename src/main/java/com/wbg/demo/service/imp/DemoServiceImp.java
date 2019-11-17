package com.wbg.demo.service.imp;

import com.wbg.demo.service.DemoService;
import com.wbg.framework.webmvc.annotation.HandWritingService;

@HandWritingService
public class DemoServiceImp implements DemoService {
    public String get(String name) {
        return "My name is " + name;
    }

    public String sum(int a,int b) {
        return "a + b = " + (a+b);
    }

    public String remove(String id) {
        return "remove id = " + id;
    }
}
