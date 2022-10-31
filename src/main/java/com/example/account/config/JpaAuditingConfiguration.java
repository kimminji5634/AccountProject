package com.example.account.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration // 자동으로 빈으로 등록
@EnableJpaAuditing // JpaAuditing 켜진 상태로 => db에 데이터 저장하거나 업데이트할때 어노테이션 붙어있는 값 자동 저장
public class JpaAuditingConfiguration {
}


