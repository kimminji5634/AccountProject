package com.example.account.service;

import com.example.account.dto.UseBalance;
import com.example.account.exception.AccountException;
import com.example.account.type.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// LockAopAspect 를 테스트
@ExtendWith(MockitoExtension.class)
class LockAopAspectTest {
    @Mock
    private LockService lockService; // LockAopAspect 가 의존하고 있음

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint; // LockAopAspect 에서 쓰고 있음

    @InjectMocks
    private LockAopAspect lockAopAspect;

    @Test
    void lockAndUnlock() throws Throwable { // lock, unlock 잘 되는지 확인
        //given
        // argument 가 잘 들어가는지 확인하기 위해서는 ArgumentCaptor 를 써야 한다
        ArgumentCaptor<String> lockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unLockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);
        UseBalance.Request request = // request 에 아래를 생성해서 담음
                new UseBalance.Request(123L, "1234", 1000L);

        //when
        lockAopAspect.aroundMethod(proceedingJoinPoint, request);

        //then
        // lock 이 잘 호출되는지 확인
        verify(lockService, times(1))
                .lock(lockArgumentCaptor.capture());
        // unlock 이 잘 호출되는지 확인
        verify(lockService, times(1))
                .unlock(unLockArgumentCaptor.capture());
        assertEquals("1234", lockArgumentCaptor.getValue());
        assertEquals("1234", unLockArgumentCaptor.getValue());
    }


    @Test
    void lockAndUnlock_evenIfThrow() throws Throwable { // lock, unlock 잘 되는지 확인
        //given
        // argument 가 잘 들어가는지 확인하기 위해서는 ArgumentCaptor 를 써야 한다
        ArgumentCaptor<String> lockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unLockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);
        UseBalance.Request request = // request 에 아래를 생성해서 담음
                new UseBalance.Request(123L, "54321", 1000L);

        // AccountException 으로 ACCOUNT_NOT_FOUND 던질 때 그럴때도 unlock이 잘 되는지
        given(proceedingJoinPoint.proceed())
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        //when => ** exception이 발생하더라도 잘 되는걸 확인할 수 있다
        assertThrows(AccountException.class, () ->
        lockAopAspect.aroundMethod(proceedingJoinPoint, request));

        //then
        // lock 이 잘 호출되는지 확인
        verify(lockService, times(1))
                .lock(lockArgumentCaptor.capture());
        // unlock 이 잘 호출되는지 확인
        verify(lockService, times(1))
                .unlock(unLockArgumentCaptor.capture());
        assertEquals("54321", lockArgumentCaptor.getValue());
        assertEquals("54321", unLockArgumentCaptor.getValue());
    }
}