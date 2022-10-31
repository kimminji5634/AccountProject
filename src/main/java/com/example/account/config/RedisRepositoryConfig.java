package com.example.account.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 별도로 떠있는 redis를 RedisRepositoryConfig에서 redissonClient가 해당 redis로 접속하도록 접근할 수 있는 client 생성
// RedisTestService에서는 redissonClient를 활용해서 스핀락 획득
@Configuration
public class RedisRepositoryConfig {
    @Value("${spring.redis.host}") // spring.redis.host는 application.yml에 있는 값인 redis host값을 가져와서
    private String redisHost; // 이 변수에 넣어주겠다

    @Value("${spring.redis.port}")
    private int redisPort;


    @Bean // 함수이름이 자동으로 빈으로 등록되게 함
    public RedissonClient redissonClient() {
        Config config = new Config(); // Config 생성
        config.useSingleServer().setAddress("redis://" + redisHost + ":" + redisPort);
        return Redisson.create(config); // redisson에 대한 접근을 허용하는 config 값을 가지는 Redisson 생성!
        // 다른 서비스나 컨트롤러에서 redissonClient 주입받게 되면 여기서 생성된 redissonClient가 거기에 불려가서 쓰이게 됨
    }
}
