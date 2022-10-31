package com.example.account.service;

import com.example.account.exception.AccountException;
import com.example.account.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

// redis 사용
@Slf4j
@Service
@RequiredArgsConstructor // final에 포함된 생성자를 만들어줌
public class LockService {
    private final RedissonClient redissonClient;

    // accountNumber를 락의 키로 삼겠음
    public void lock(String accountNumber) {
        RLock lock = redissonClient.getLock(getLockKey(accountNumber));
        log.debug("Trying lock for accountNumber : {}", accountNumber);

        try { // 최대 1초동안 기다리면서 이 lock을 시도하고 -> 15초동안 아무 작업 안하면 lock이 풀림
            boolean isLock = lock.tryLock(1, 15, TimeUnit.SECONDS);
            if (!isLock) { //false인 경우, 해당 경우 실패한 것임
                log.error("=============Lock aquisition failed==============");
                throw new AccountException(ErrorCode.ACCOUNT_TRANSACTION_LOCK);
            }
            // 락을 못가져와서 생기는 에러가 아닌 다른 에러라면 다른 에러 찍히도록 => 추가
        } catch (AccountException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis lock failed", e);
        }
    }

    public void unlock(String accountNumber) {
        log.debug("Unlock for accountNumber : {}", accountNumber);
        redissonClient.getLock(getLockKey(accountNumber)).unlock();// 락을 가져온 후 unlock 시킴
    }

    private String getLockKey(String accountNumber) {
        return "ACLK:" + accountNumber; // "ACLK:" 아무 의미 없는 문자열임
    }
}
