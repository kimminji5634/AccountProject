package com.example.account.controller;

import com.example.account.dto.AccountDto;
import com.example.account.dto.CancelBalance;
import com.example.account.dto.TransactionDto;
import com.example.account.dto.UseBalance;
import com.example.account.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.USE;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {
    @MockBean // TransactionService 를 가짜로 만들어서 주입
    private TransactionService transactionService;

    @Autowired // 주입 시키는 어노테이션
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successUseBalance() throws Exception {
        //given
        given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
                // transactionService.useBalance는 TransactionDto를 리턴함
                .willReturn(TransactionDto.builder()
                        // TransactionDto 에서 필요한 부분만 씀
                        .accountNumber("1000000000")
                        .transactedAt(LocalDateTime.now())
                        .amount(12345L)
                        .transactionId("transactionId")
                        .transactionResultType(S)
                        .build());

        //when
        //then
        mockMvc.perform(post("/transaction/use")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                            new UseBalance.Request(1L, "2000000000", 3000L)
                        ))
                ).andDo(print())
                .andExpect(status().isOk())
                // "$.accountNumber : 최상위 구조에 accountNumber가 들어가 있을 것이다
                // UseBalance.response 에 대해 확인해 보자, transactedAt은 생략하자
                .andExpect(jsonPath("$.accountNumber").value("1000000000"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionId"))
                .andExpect(jsonPath("$.amount").value(12345));
    }

    @Test
    void successCancelBalance() throws Exception { // successUseBalance와 거의 유사함
        //given
        given(transactionService.cancelBalance(anyString(), anyString(), anyLong()))
                // transactionService.useBalance는 TransactionDto를 리턴함
                .willReturn(TransactionDto.builder()
                        // TransactionDto 에서 필요한 부분만 씀
                        .accountNumber("1000000000")
                        .transactedAt(LocalDateTime.now())
                        .amount(54321L)
                        .transactionId("transactionIdForCancel")
                        .transactionResultType(S)
                        .build());

        //when
        //then
        mockMvc.perform(post("/transaction/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CancelBalance.Request("transactionId",
                                        "2000000000", 3000L)
                        ))
                ).andDo(print())
                .andExpect(status().isOk())
                // "$.accountNumber : 최상위 구조에 accountNumber가 들어가 있을 것이다
                // UseBalance.response 에 대해 확인해 보자, transactedAt은 생략하자
                .andExpect(jsonPath("$.accountNumber").value("1000000000"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionIdForCancel"))
                .andExpect(jsonPath("$.amount").value(54321));
    }

    @Test
    void successQueryTransaction() throws Exception {
        //given
        given(transactionService.queryTransaction(anyString())) // controller 의 리턴 타입
                .willReturn(TransactionDto.builder()
                        .accountNumber("1000000000")
                        .transactionType(USE)
                        .transactedAt(LocalDateTime.now())
                        .amount(54321L)
                        .transactionId("transactionIdForCancel")
                        .transactionResultType(S)
                        .build());
        //when
        //then
        //위에 given에서 anyString()이므로 transactionId 아무값이나 12345 와도 됌
        mockMvc.perform(get("/transaction/12345"))
                .andDo(print())
                .andExpect(status().isOk())
                // "$.accountNumber : 최상위 구조에 accountNumber가 들어가 있을 것이다
                // UseBalance.response 에 대해 확인해 보자, transactedAt은 생략하자
                .andExpect(jsonPath("$.accountNumber").value("1000000000"))
                .andExpect(jsonPath("$.transactionType").value("USE"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionIdForCancel"))
                .andExpect(jsonPath("$.amount").value(54321));
    }
}