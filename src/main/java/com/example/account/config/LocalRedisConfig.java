

package com.example.account.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Configuration // redis 를 빈에 등록함
public class LocalRedisConfig {
    @Value("${spring.redis.port}") // application.yml에 써준 redis host 값을 가져와서
    private int redisPort; // redisPort 에 담아주겠다

    private RedisServer redisServer; // Redis embedded에 있는 redisServer를 프로퍼티로 넣어줌

    // 자동으로 어플리케이션 뜰 때 redis를 start 시켜주고 어플리케이션 꺼질때(빈을 파괴할 때) 자동으로 꺼지도록 설정
    @PostConstruct
    public void startRedis() { // 이 이름은 아무거나 써도 무방함, 의미 없음
        redisServer = new RedisServer(redisPort); // redisPort 값 넣어서 redisServer 생성 => 객체를 생성함
        redisServer.start(); // 실행시킴
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) { // redisServer 생성된 경우에만 stop 가능하도록
            redisServer.stop();
        }
    }
}
