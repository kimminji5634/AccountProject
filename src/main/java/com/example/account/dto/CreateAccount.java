package com.example.account.dto;

import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class CreateAccount {
    @Getter
    @Setter
    @AllArgsConstructor // test에서 new로 생성자 만들 수 있도록 추가함
    public static class Request { // static 임!
        @NotNull // userId는 필수값임
        @Min(1) // 1부터 시작
        private Long userId;

        @NotNull
        @Min(0) // 계좌 해지가 가능하도록 100 -> 0 으로 바꿈
        private Long initialBalance;

        public Request(){}
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long userId;
        private String accountNumber;
        private LocalDateTime registeredAt;

        // AccountDto 에서 CreateAccount.Response 의 값 가져오도록
        public static Response from(AccountDto accountDto) {
            return Response.builder()
                    .userId(accountDto.getUserid())
                    .accountNumber(accountDto.getAccountNumber())
                    .registeredAt(accountDto.getRegisteredAt())
                    .build();
        }
    }
}
