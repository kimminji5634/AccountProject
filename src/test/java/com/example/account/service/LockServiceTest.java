package com.example.account.service;

import com.example.account.exception.AccountException;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LockServiceTest {
    @Mock
    private RedissonClient redissonClient;

    // LockService 에 보면 RLock 을 어떻게 해주냐에 따라 로직이 변화하기 때문에
    // RLock 을 모킹해가지고 RLock 을 우리가 원하는대로 바꿔주도록 하겠음
    @Mock
    private RLock rLock; // LockService 에서는 lock 에 해당

    @InjectMocks // 위의 Mock 을 lockService 에 담는다
    private LockService lockService;

    @Test
    void successGetLock() throws InterruptedException {
        //given
        given(redissonClient.getLock(anyString()))
                .willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any()))
                .willReturn(true);

        //when
        //then
        // 예외가 없을 때!!!
        assertDoesNotThrow(() -> lockService.lock("123"));
    }

    @Test
    void failGetLock() throws InterruptedException {
        //given
        given(redissonClient.getLock(anyString()))
                .willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any()))
                .willReturn(false);

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> lockService.lock("123"));

        //then
        assertEquals(ErrorCode.ACCOUNT_TRANSACTION_LOCK, exception.getErrorCode());
    }
}