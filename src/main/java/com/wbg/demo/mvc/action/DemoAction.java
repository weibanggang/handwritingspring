package com.wbg.demo.mvc.action;

import com.wbg.demo.service.DemoService;
import com.wbg.framework.webmvc.annotation.HandWritingAutowired;
import com.wbg.framework.webmvc.annotation.HandWritingController;
import com.wbg.framework.webmvc.annotation.HandWritingRequestMapping;
import com.wbg.framework.webmvc.annotation.HandWritingRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@HandWritingController
@HandWritingRequestMapping("/demo")
public class DemoAction {
    @HandWritingAutowired
    private DemoService demoService;

    @HandWritingRequestMapping("/query.json")
    public void query(HttpServletRequest req, HttpServletResponse resp, @HandWritingRequestParam("name")String name){
        String result =  demoService.get(name);
        try{
            resp.getWriter().write(result);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    @HandWritingRequestMapping("/sum.json")
    public void sum(HttpServletRequest req, HttpServletResponse resp, @HandWritingRequestParam("a") Integer a, @HandWritingRequestParam("b") Integer b){
        String result =  demoService.sum(a,b);
        try{
            resp.getWriter().write(result);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    @HandWritingRequestMapping("/remove.json")
    public void remove(HttpServletRequest req, HttpServletResponse resp, @HandWritingRequestParam("id") String id){
        String result =  demoService.remove(id);
        try{
            resp.getWriter().write(result);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
