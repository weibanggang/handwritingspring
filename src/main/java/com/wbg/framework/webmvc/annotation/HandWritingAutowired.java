package com.wbg.framework.webmvc.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HandWritingAutowired {
    String value() default "";
}
