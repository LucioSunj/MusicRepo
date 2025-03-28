package org.example.cpt202music.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


// 针对方法的注解
// 使用这个注解的目的是为了进行用户权限的校验，如果不想要有角色，那么就不用这个注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 必须具有某个角色
     * @return
     */
    String mustRole() default "";
}
