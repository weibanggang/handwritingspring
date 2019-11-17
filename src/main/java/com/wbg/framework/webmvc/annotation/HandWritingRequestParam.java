package com.wbg.framework.webmvc.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HandWritingRequestParam {
    String value() default "";
}
