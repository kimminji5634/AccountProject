package com.example.account.exception;

import com.example.account.dto.ErrorResponse;
import com.example.account.type.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.example.account.type.ErrorCode.INTERNAL_SERVER_ERROR;
import static com.example.account.type.ErrorCode.INVALID_REQUEST;

@Slf4j
@RestControllerAdvice // 모든컨트롤러에서 발생하는 exception 을 처리해주겠다
public class GlobalExceptionHandler {

    // userId가 잘못들어오면
    @ExceptionHandler(AccountException.class) // AccountException 이 발생하면
    public ErrorResponse handleAccountException(AccountException e) {
        log.error("{} is ocurred.", e.getErrorCode());

        // **두개만 써주면 되므로 build로 작성 안해주고 바로 new 로 해주겠음
        return new ErrorResponse(e.getErrorCode(), e.getErrorMessage());
    }

    // amount를 잘못 입력했다든가 잘못된 요청이 들어왔을 때
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handelMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException is ocurred.", e);

        // **두개만 써주면 되므로 build로 작성 안해주고 바로 new 로 해주겠음
        return new ErrorResponse(INVALID_REQUEST, INVALID_REQUEST.getDescription());
    }

    // db의 유니크 키가 중복될 때 주는 에러 -> 종종 발생함
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ErrorResponse handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("DataIntegrityViolationException is ocurred.", e);

        // **두개만 써주면 되므로 build로 작성 안해주고 바로 new 로 해주겠음
        return new ErrorResponse(INVALID_REQUEST, INVALID_REQUEST.getDescription());
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleException(Exception e) {
        log.error("Exception is ocurred.", e);

        // **두개만 써주면 되므로 build로 작성 안해주고 바로 new 로 해주겠음
        return new ErrorResponse(INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR.getDescription());
    }
}
