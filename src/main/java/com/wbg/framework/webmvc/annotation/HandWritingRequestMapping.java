package com.wbg.framework.webmvc.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HandWritingRequestMapping {
    String value() default "";
}
