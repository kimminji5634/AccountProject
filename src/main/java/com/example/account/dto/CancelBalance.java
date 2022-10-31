package com.example.account.dto;

import com.example.account.aop.AccountLockIdInterface;
import com.example.account.type.TransactionResultType;
import lombok.*;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

public class CancelBalance {

    /** 요청
     * "transactionId" : "asdfasdfsadf" => 난수,
     * "accountNumber" : "1000000000",
     * "amount" : 1000 => 최소, 최대값 필요함 왜? 너무 적거나 많이 요청하면 거래 취소
     */

    @Getter
    @Setter
    @AllArgsConstructor // test에서 new로 생성자 만들 수 있도록 추가함
    public static class Request implements AccountLockIdInterface { // static 임!
        @NotBlank
        private String transactionId;

        @NotBlank
        @Size(min = 10, max = 10)
        private String accountNumber;

        @NotNull
        @Min(10)
        @Max(1000_000_000)
        private Long amount;

        public Request() {}
    }

    /** 응답
     * "accountNumber" : "1234567890",
     * "transactionResult" : "S",
     * "transactionId" : "c2033bb6d82a4250aecf8e27c49b63"
     * "amount" : 1000 => 최소, 최대값 필요함 왜? 너무 적거나 많이 요청하면 거래 취소
     * "transactedAt" : "2022-06-01T23:26:14.671859"
     */

    // 응답에 대한 게 UseBalance 와 동일하더라도 따로 쓰는 게 더 좋다
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private String accountNumber;
        private TransactionResultType transactionResult; // TransactionResultType 클래스 enum 생성
        private String transactionId;
        private Long amount;
        private LocalDateTime transactedAt;

        public static Response from(TransactionDto transactionDto) {
            return Response.builder()
                    .accountNumber(transactionDto.getAccountNumber())
                    .transactionResult(transactionDto.getTransactionResultType())
                    .transactionId(transactionDto.getTransactionId())
                    .amount(transactionDto.getAmount())
                    .transactedAt(transactionDto.getTransactedAt())
                    .build();
        }
    }
}
