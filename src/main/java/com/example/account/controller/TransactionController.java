package com.example.account.controller;


import com.example.account.aop.AccountLock;
import com.example.account.dto.CancelBalance;
import com.example.account.dto.QueryTransactionResponse;
import com.example.account.dto.TransactionDto;
import com.example.account.dto.UseBalance;
import com.example.account.exception.AccountException;
import com.example.account.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 잔액 관련 컨트롤러
 * 1. 잔액 사용
 * 2. 잔액 사용 취소
 * 3. 거래 확인
 */

@Slf4j
@RestController // spring 빈으로 자동 등록됨
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transacionService;

    @PostMapping("/transaction/use")
    @AccountLock
    public UseBalance.Response useBalance(
            @Valid @RequestBody UseBalance.Request request
    ) throws InterruptedException {
        // transactionService.useBalance의 validateUseBalanace exception이 발생했을 때
        // 저장해주는 부분이 없으므로 => 예외처리 해주는 부분이 없으므로 => try ~ catch로 예외처리 해주겠음
        try {
            Thread.sleep(3000L); // 3초후 응답을 보여줌
            return UseBalance.Response.from(
                    transacionService.useBalance(request.getUserId(),
                            request.getAccountNumber(), request.getAmount())
            );
        } catch (AccountException e) {
            log.error("Failed to use balance. ");
            transacionService.saveFailedUseTransaction( // 실패건 저장 해야함
                    request.getAccountNumber(),
                    request.getAmount()
            );

            throw e;
        }
    }

    @PostMapping("/transaction/cancel")
    @AccountLock
    public CancelBalance.Response cancelBalance(
            @Valid @RequestBody CancelBalance.Request request
    ) {
        // transactionService.useBalance의 validateUseBalanace exception이 발생했을 때
        // 저장해주는 부분이 없으므로 => 예외처리 해주는 부분이 없으므로 => try ~ catch로 예외처리 해주겠음
        // 메소드 생성 => alt + 마우스 갖다대기
        try {
            return CancelBalance.Response.from(
                    transacionService.cancelBalance(request.getTransactionId(),
                            request.getAccountNumber(), request.getAmount())
            );
        } catch (AccountException e) {
            log.error("Failed to use balance. ");

            transacionService.saveFailedCancelTransaction( // 실패건 저장 해야함
                    request.getAccountNumber(),
                    request.getAmount()
            );

            throw e;
        }
    }

    @GetMapping("/transaction/{transactionId}")
    public QueryTransactionResponse queryTransaction(
            @PathVariable String transactionId) {
        return QueryTransactionResponse.from(
                transacionService.queryTransaction(transactionId));
    }
}
