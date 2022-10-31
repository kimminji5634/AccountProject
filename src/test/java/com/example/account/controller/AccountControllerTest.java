package com.example.account.controller;


import com.example.account.domain.Account;
import com.example.account.dto.AccountDto;
import com.example.account.dto.CreateAccount;
import com.example.account.dto.DeleteAccount;
import com.example.account.exception.AccountException;
import com.example.account.type.AccountStatus;
import com.example.account.service.AccountService;
import com.example.account.type.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 진짜 AccountController 와 가짜 AccountService, 가짜 RedisTestService 가 주입됨 => spring Container로 이동
// 주입 가능한 이유는 AccountController 에서 AccountService, RedisTestService 주입받음
// MockMvc 가 요청을 안쪽으로(테스트 컨네이너) 날려서 테스트 할 수 있게 해줌

// 컨트롤러 테스트 진행
@WebMvcTest(AccountController.class) // @WebMvcTest를 사용해서 AccountController 특정 컨트롤러만 격리시켜 단위 테스트
class AccountControllerTest {
    // 컨트롤러에서 의존하고 있는 애들을 @MockBean을 통해 가짜로 빈 등록을 해줌
    @MockBean // mock + bean => AccountController에 주입됨
    private AccountService accountService;

    // @WebMvcTest가 MockMvc를 자동으로 생성해줌
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successCreateAccount() throws Exception {
        //given
        given(accountService.createAccount(anyLong(), anyLong())) // 어느 값이 파라미터로 들어와도
                .willReturn(AccountDto.builder() // 아래를 리턴할 것이다
                        .userid(1L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        //when
        //then
        mockMvc.perform(post("/account")
                .contentType(MediaType.APPLICATION_JSON) // json 타입으로 contentType을 헤더에 넣어줌, content는 바디에 넣어줌
                .content(objectMapper.writeValueAsString( // objectMapper.writeValueAsString : json 문자열로 만들어줌
                        new CreateAccount.Request(3333L, 1111L) // 파라미터값은 아무거나
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andDo(print());
    }

    @Test
    void successGetAccountByUserId() throws Exception {
        //given
        // 복잡해서 아래를 따로 빼서 추가해줄것임, List<AccountDto>는 service 의 리턴 타입
        List<AccountDto> accountDtos =
                Arrays.asList( // 에러가 떠서 왜 뜨지 했는데 Arrays 불러올 때 다른 걸 불러왔었음..
                        // AccountDto를 3개 생성함
                        AccountDto.builder() // accountNumber, balance 응답값임
                                .accountNumber("1234567890")
                                .balance(1000L).build(),
                        AccountDto.builder()
                                .accountNumber("1111111111")
                                .balance(2000L).build(),
                        AccountDto.builder()
                                .accountNumber("2222222222")
                                .balance(3000L).build()
                );
        given(accountService.getAccountsByUserId(anyLong())) // controller 의 리턴 타입
                .willReturn(accountDtos);
        //when
        //then
        mockMvc.perform(get("/account?user_id=1"))
                .andDo(print()) // $는 json에서 root값이고, [0]는 리스트에서 첫번째 값을 말함 => json path 구글 치면 설명 나옴
                .andExpect(jsonPath("$[0].accountNumber"). value("1234567890"))
                .andExpect(jsonPath("$[0].balance"). value(1000))
                .andExpect(jsonPath("$[1].accountNumber"). value("1111111111"))
                .andExpect(jsonPath("$[1].balance"). value(2000))
                .andExpect(jsonPath("$[2].accountNumber"). value("2222222222"))
                .andExpect(jsonPath("$[2].balance"). value(3000));
    }


    @Test
    void successDeleteAccount() throws Exception {
        //given
        given(accountService.deleteAccount(anyLong(), anyString())) // 어느 값이 파라미터로 들어와도
                .willReturn(AccountDto.builder() // 아래를 리턴할 것이다
                        .userid(1L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        //when
        //then
        mockMvc.perform(delete("/account")
                        .contentType(MediaType.APPLICATION_JSON) // json 타입으로 contentType을 헤더에 넣어줌, content는 바디에 넣어줌
                        .content(objectMapper.writeValueAsString( // objectMapper.writeValueAsString : json 문자열로 만들어줌
                                new DeleteAccount.Request(3333L, "1111111111") // 파라미터값은 아무거나
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andDo(print());
    }




    @Test
    void successGetAccount() throws Exception { // 예외처리는 then 부분에서 checkedException 발생해서 붙여줘야 함
        //given => 모킹된 상태
        given(accountService.getAccount(anyLong())) // accountService가 getAccount 호출을 받게 되면
                .willReturn(Account.builder() // accountNumber가 3456이고 accountStatusr가 IN_USE인 account를 리턴함
                        .accountNumber("3456")
                        .accountStatus(AccountStatus.IN_USE)
                        .build());

        //when
        // AccountServiceTest 할때는 when에서 결과 받아서 확인했지만 여기서는 결과가 객체로 만들어지는게 아니라
        // 결과가 http 프로토콜의 응답 형식으로 오기 떄문에 일반적인 리셉션?으로 테스트 할 수 없다

        //then
        mockMvc.perform(get("/account/876")) // 테스트하고자 하는 컨트롤러 안에 있는 url "/account/876" 호출
                .andDo(print()) // get을 했을 때 응답 값을 화면에 자세히 표시해줌
                // $ 바디에 있는, jsonPath는 result로 가져옴
                .andExpect(jsonPath("$.accountNumber").value("3456"))
                .andExpect(jsonPath("$.accountStatus").value("IN_USE"))
                .andExpect(status().isOk());
    }

    // 테스트 코드로 우리가 짠 에러 응답 잘 오나 확인 해봄
    // 컨트롤러 테스트!
    @Test
    void failGetAccount() throws Exception{
        //given
        given(accountService.getAccount(anyLong()))
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        //when
        //then
        mockMvc.perform(get("/account/876"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value("계좌가 없습니다."))
                .andExpect(status().isOk());
    }
}

