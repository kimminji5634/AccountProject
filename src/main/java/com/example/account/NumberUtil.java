package com.example.account;

import lombok.experimental.UtilityClass;

// 유틸성 클래스 생성 : 빈으로 등록할 필요가 없고 클래스(객체) 자체는 껍데기고 static 한 메소드를 필요할 때마다 만들어놓고 쓰려고 만듦
@UtilityClass
public class NumberUtil {
    // private NumberUtil(){} 생성자를 private으로 만들어서 다른 곳에서 못 쓰게 막음 => @UtilityClass 가 이를 대신함


    public static Integer sum(Integer a, Integer b) {
        return a + b;
    }

    public static Integer minus(Integer a, Integer b) {
        return a - b;
    }
}
