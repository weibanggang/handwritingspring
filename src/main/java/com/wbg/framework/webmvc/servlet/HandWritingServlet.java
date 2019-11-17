package com.wbg.framework.webmvc.servlet;

import com.wbg.framework.webmvc.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HandWritingServlet extends HttpServlet {
    //配置文件
    private Properties contextConfig = new Properties();
    //存配置文件 scanPackage=com.wbg.demo 下所有的类名
    private List<String> classNames = new ArrayList<String>();
    //IOC容器
    private Map<String, Object> ioc = new HashMap<String, Object>();
    //保存所有的URL和方法的映射关系
    private List<Handler> handlerMapping = new ArrayList<Handler>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            try {
                resp.getWriter().write("500 =========" + e);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * 初始化，加载配置
     * @param config 配置文件
     */
    @Override
    public void init(ServletConfig config) {

        System.out.println("======初始化配置文件======");
        //1、加载配置文件  获取web.xml文件的的param-name
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //2、扫描所有相关联的类 //配置文件的scanPackage = com...
        doScanner(contextConfig.getProperty("scanPackage"));
        //3、初始化所有相关的类，并且将其保存到IOC容器之中
        doInstance();
        //4、执行依赖注入（把加了@Autoidwired注解的字段赋值）
        doAutoWired();

        //-------------------------Spring的核心功能已经完成  IOC  DI注入

        //5、构造HandlerMapping,将URL和Method进行关联
        initHandlerMapping();

        System.out.println("=========启动完毕============");

    }

    /**
     * 构造HandlerMapping,将URL和Method进行关联
     */
    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {

            Class<?> clazz = entry.getValue().getClass();
            //如果该类上没有Controller  下一个
            if (!clazz.isAnnotationPresent(HandWritingController.class)) {
                continue;
            }

            String baseUrl = "";
            //如果该类上有RequestMapping注解
            if (clazz.isAnnotationPresent(HandWritingRequestMapping.class)) {
                HandWritingRequestMapping requestMapping = clazz.getAnnotation(HandWritingRequestMapping.class);
                //获取RequestMapping注解的值
                baseUrl = requestMapping.value();
            }
            //获取该类的所有方法
            Method[] methods = clazz.getMethods();

            for (Method method : methods) {
                //如果该方法上面没有RequestMapping 下一个
                if (!method.isAnnotationPresent(HandWritingRequestMapping.class)) {
                    continue;
                }

                HandWritingRequestMapping requestMapping = method.getAnnotation(HandWritingRequestMapping.class);
                //获取RequestMapping注解的值
                String regex = requestMapping.value();
                //将 类上、方法上 的RequestMapping 注解相加  得到一个url
                regex = (baseUrl + regex).replaceAll("/+", "/");
                Pattern pattern = Pattern.compile(regex);
                handlerMapping.add(new Handler(pattern, entry.getValue(), method));
                System.out.println("handlerMapping：" + regex);
            }

        }

    }

    /**
     * 执行依赖注入 将加了AutoWired注解的字段进行赋值
     * 注入的意思就是把所有IOC容器中加了@Autowired注解的字段全部赋值
     */
    private void doAutoWired() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //获取该类的声明字段 包过私有的字段
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            //循环加入IOC容器
            for (Field field : fields) {
                //如果字段不加Autowired注解  下一个
                if (!field.isAnnotationPresent(HandWritingAutowired.class)) {
                    continue;
                }
                //获取这个字段上的Autowired注解
                HandWritingAutowired autowired = field.getAnnotation(HandWritingAutowired.class);
                //获取注解上的value
                String beanName = autowired.value().trim();
                //如果为空
                if ("".equals(beanName)) {
                    //获取这个字段的名字
                    beanName = field.getType().getName();
                }
                //如果这个字段是私有的字段，强制访问
                field.setAccessible(true);
                try {
                    //赋值
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 初始化所有相关的类
     * 加了注解的才初始化
     */
    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                //只有加了注解的  才初始化
                if (clazz.isAnnotationPresent(HandWritingController.class)) {
                    Object instance = clazz.newInstance();
                    String beanName = lowerFirstClass(clazz.getSimpleName());
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(HandWritingService.class)) {
                    HandWritingService service = clazz.getAnnotation(HandWritingService.class);
                    //2、自定义命名，优先使用自定义命名
                    String beanName = service.value();
                    //1、默认类名首字母小写
                    if ("".equals(beanName.trim())) {
                        beanName = lowerFirstClass(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);

                    // 3、自动类型匹配（例如将实现类赋值给接口）
                    Class<?>[] instances = clazz.getInterfaces();
                    for (Class<?> i : instances) {
                        ioc.put(i.getName(), instance);
                    }
                } else {
                    continue;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 将字符串的首字母变成小写  利用ASCII转换
     * @param simpleName
     * @return
     */
    private String lowerFirstClass(String simpleName) {
        char[] c = simpleName.toCharArray();
        c[0] += 32;
        return String.valueOf(c);
    }

    protected void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Handler handler = getHandler(req);
        //如果为空  没有这个请求路径
        if (handler == null) {
            resp.getWriter().write("=========handWritingSpring=============404==========");
            return;
        }
        try {
            //获取方法的参数列表
            Class<?>[] paramTypes = handler.method.getParameterTypes();
            //保存所有需要自动赋值的参数值
            Object[] paramValues = new Object[paramTypes.length];
            //获取方法上的参数
            Map<String, String[]> params = req.getParameterMap();
            for (Map.Entry<String, String[]> param : params.entrySet()) {
                //转换为String 去掉数组[]两边 去掉字符的空白
                String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll("\\s", "");
                //如果找到匹配的对象，则开始填充参数值
                if (!handler.paramIndexMapping.containsKey(param.getKey())) {
                    continue;
                }
                int index = handler.paramIndexMapping.get(param.getKey());
                paramValues[index] = convert(paramTypes[index], value);
            }
            int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = req;
            int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = resp;
            handler.method.invoke(handler.controller, paramValues);
        } catch (Exception e) {
            throw e;
        }
    }

    private Handler getHandler(HttpServletRequest req) throws Exception {
        if (handlerMapping.isEmpty()) {
            return null;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        for (Handler handler : handlerMapping) {
            try {
                Matcher matcher = handler.pattern.matcher(url);
                //如果没有匹配上继续下一个匹配
                if (!matcher.matches()) {
                    continue;
                }
                return handler;
            } catch (Exception e) {
                throw e;
            }

        }
        return null;
    }

    /**
     * 转换类型Integer
     * @param type
     * @param value
     * @return
     */
    private Object convert(Class<?> type, String value) {
        if (Integer.class == type) {
            return Integer.valueOf(value);
        }
        return value;
    }

    /**
     * 扫描basePackage路径下所以的类
     * @param basePackage
     */
    private void doScanner(String basePackage) {
        //获取路径
        URL url = this.getClass().getClassLoader().getResource("/" + basePackage.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            //如果是一个文件夹
            if (file.isDirectory()) {
                //递归
                doScanner(basePackage + "." + file.getName());
            } else {
                //获取类名
                String className = basePackage + "." + file.getName().replace(".class", "");
                //将所有项目所有类存起来
                classNames.add(className);
            }
        }
    }

    /**
     * 加载配置文件
     * @param contextConfigLocation
     */
    private void doLoadConfig(String contextConfigLocation) {

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Handler记录Controller中的RequestMapping和Method对应的关系
     */
    private class Handler {
        protected Object controller; //保存方法对应的实例
        protected Method method; //保存映射的方法
        protected Pattern pattern;  //RequestMapping存的URL
        protected Map<String, Integer> paramIndexMapping; //参数顺序

        /**
         * 基本参数
         * @param pattern
         * @param controller
         * @param method
         */
        protected Handler(Pattern pattern, Object controller, Method method) {
            this.pattern = pattern;
            this.controller = controller;
            this.method = method;
            paramIndexMapping = new HashMap<String, Integer>();
            putParamIndexMapping(method);

        }

        /**
         * 保存RequestParam注解的参数值
         * @param method 类的方法
         */
        private void putParamIndexMapping(Method method) {
            //获取方法中加了注解的参数
            Annotation[][] pa = method.getParameterAnnotations();
            for (int i = 0; i < pa.length; i++) {
                for (Annotation annotation : pa[i]) {
                    //如果使用到了HandWritingRequestParam注解
                    if (annotation instanceof HandWritingRequestParam) {
                        //获取该注解的值
                        String paramName = ((HandWritingRequestParam) annotation).value();
                        //如果这个值不为空
                        if (!"".equals(paramName.trim())) {
                            //保存参数
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }
            //提取方法中的request和response参数
            Class<?>[] paramsTypes = method.getParameterTypes();
            for (int i = 0; i < paramsTypes.length; i++) {
                Class<?> type = paramsTypes[i];
                if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                    paramIndexMapping.put(type.getName(), i);
                }
            }

        }

    }
}
