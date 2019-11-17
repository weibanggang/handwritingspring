package com.wbg.framework.webmvc.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HandWritingService {
    String value() default "";
}
