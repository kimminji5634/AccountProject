package com.example.account.dto;

import com.example.account.type.ErrorCode;
import lombok.*;

// 아래 5개의 어노테이션 넣어주면 웬만해서는 문제없이 사용할 수 있음
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {
    private ErrorCode errorCode;
    private String errorMessage;
}
