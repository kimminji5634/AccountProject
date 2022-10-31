package com.example.account.dto;

import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

public class DeleteAccount {
    @Getter
    @Setter
    @AllArgsConstructor // test에서 new로 생성자 만들 수 있도록 추가함
    public static class Request { // static 임!
        @NotNull // userId는 필수값임
        @Min(1) // 1부터 시작
        private Long userId;

        @NotBlank // notnull보다 더 강력
        @Size(min = 10, max = 10) // size는 문자열 길이
        private String accountNumber;

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
        private LocalDateTime unRegisteredAt;

        // AccountDto 에서 CreateAccount.Response 의 값 가져오도록
        public static Response from(AccountDto accountDto) { // AccountDto는 계좌에 대한 모든 프로퍼티 있음
            return Response.builder()
                    .userId(accountDto.getUserid())
                    .accountNumber(accountDto.getAccountNumber())
                    .unRegisteredAt(accountDto.getUnRegisteredAt())
                    .build();
        }
    }
}
