package com.example.account.controller;

import com.example.account.domain.Account;
import com.example.account.dto.AccountDto;
import com.example.account.dto.AccountInfo;
import com.example.account.dto.CreateAccount;
import com.example.account.dto.DeleteAccount;
import com.example.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

// 우리가 하는 방식은 layer 방식
// 외부에서는 컨트롤러로만 접속을 하고 컨트롤러는 서비스로만 접속을 하고 서비스는 리포지토리에 접근을 하는 계층화된 구조임
// 컨트롤러는 서비스만 의존하게 함

@RestController // 이 컨트롤러가 bean으로 등록되게 해주세요
@RequiredArgsConstructor // final의 생성자 생성해주기 위해
public class AccountController {
    private final AccountService accountService; // 의존성 주입받음

    // createAccount API 생성됨
    @PostMapping("/account")
    // @Valid는 CreateAccount.Request의 파라미터 조건이 유효하다면 값을 가져와라
    public CreateAccount.Response createAccount(
            @RequestBody @Valid CreateAccount.Request request
    ){
        return CreateAccount.Response.from(
                accountService.createAccount(
                        request.getUserId(),
                        request.getInitialBalance()
                )
        );
    }

    // 계좌 해지 api 생성
    @DeleteMapping("/account")
    public DeleteAccount.Response deleteAccount(
            @RequestBody @Valid DeleteAccount.Request request
    ){
        return DeleteAccount.Response.from(
                accountService.deleteAccount( // deleteAccount를 accountService에 요청할 것임!
                        request.getUserId(),
                        request.getAccountNumber()
                )
        );
    }

    // 계좌 학인 API 생성
    @GetMapping("/account")
    public List<AccountInfo> getAccountsByUserId(
            @RequestParam("user_id") Long userId // RequestParam으로 아이디를 받고
    ){
        return accountService.getAccountsByUserId(userId) // alter + enter로 메서드 생성
                // 아래는 List<AccountDto>로 응답이 넘어온 걸 List<AccountInfo>로 변환해주기
                .stream().map(accountDto ->
                        AccountInfo.builder()
                        .accountNumber(accountDto.getAccountNumber())
                        .balance(accountDto.getBalance())
                        .build())
                .collect(Collectors.toList());
    }

    @GetMapping("/account/{id}")
    public Account getAccount(@PathVariable Long id) { // 이름이 같아서 PathVariable 생략 가능
        return accountService.getAccount(id);
    }
}
