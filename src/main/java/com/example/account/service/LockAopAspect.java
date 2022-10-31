package com.example.account.service;

import com.example.account.aop.AccountLockIdInterface;
import com.example.account.dto.UseBalance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LockAopAspect {
    private final LockService lockService;

    // 아래의 어노테이션이 달린 메소드가 실행될 때 전 후에 lock을 취득했다가 lock을 해제하는 방식
    @Around("@annotation(com.example.account.aop.AccountLock) && args(request)")
    // accountNumber 가져와야함 : && args(request) : @AccountLock 넣어준 파라미터 Request 를 가져다 쓸 수 있다
    public Object aroundMethod(
            ProceedingJoinPoint pjp, // 조인포인트
            AccountLockIdInterface request // useBalance, cancelBalance 상관없이 우리가 의도한 타입으로 가져옴
    ) throws Throwable {
        // lock 취득 시도
        lockService.lock(request.getAccountNumber());
        try {
            return pjp.proceed(); // 로직 동작시킴
        } finally {
            // 동작이 정상적으로 진행되든 안되든 lock을 해제
            lockService.unlock(request.getAccountNumber());
        }
    }
}
