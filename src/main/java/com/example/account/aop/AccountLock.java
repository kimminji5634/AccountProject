package com.example.account.aop;

import java.lang.annotation.*;

// 기본적인 어노테이션
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
// 락을 생성한 것임
public @interface AccountLock {
    long tryLockTime() default 5000L; // 어노테이션에서 지정해준 값으로 해당 시간동안 기다려보겠다
}
