package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.repository.AccountRepository;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
// AccountService의 getAccount가 잘 되는지 test 해보겠음
// 이 test의 문제가 있다 테스트 돌기 전에 create으로 만들어야 하고 getAccount id를 직접 증가시켜야 함
// mocking을 통한 격리성을 확보해주는 Mockito를 이용해서 이 테스트가 가진 문제를 해결하겠다

@ExtendWith(MockitoExtension.class) // Mockito 확장 팩을 달아줌 => Mockito로 진행
class AccountServiceTest {

    // AccountService가 AccountRepository, AccountUserRepository를 의존하므로 추가해줌
    @Mock
    private AccountRepository accountRepository; // 가짜로 만듦

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks // 가짜로 만든 accountRepository와 accountUserRepository를
                 // accountService에 inject 시킴
    private AccountService accountService;

    @Test
    void createAccountSuccess() {
        //given
        // AccountUser는 id, name, date만 가짐
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);
        // findById
        given(accountUserRepository.findById(anyLong()))
                //optional타입의 accountUser가 생성되어야 한다. build를 통해 생성해보겠다
                .willReturn(Optional.of(user));
        // findFirstByOrderByIdDesc
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                        // 현재까지 저장된 계좌번호 제일 마지막보다 맨 밑 리턴값은 + 1된 값 나옴
                                .accountNumber("1000000012").build()));
        // accountRepository.save
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000015").build());

        // 원래 로직은 accountNumber + 1한 결과가 리턴되어야 함, 아래를 사용
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        // ctrl + alt + v => 변수 생성
        AccountDto accountDto = accountService.createAccount(1L, 1000L);

        //then
        // accountRepository가 1번 저장을 할 거고 그 떄 captor가 capture할 것임
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserid());
        assertEquals("1000000013", captor.getValue().getAccountNumber());
    }

    @Test
    void createFirstAccount() {
        // findById는 문제가 없고 findFirstByOrderByIdDesc에서 리턴을 할 때
        // optional에 데이터가 없는 경우 => 아무 계좌도 없는 상황일 때

        //given
        // AccountUser는 id, name, date만 가짐
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(15L);
        // findById
        given(accountUserRepository.findById(anyLong()))
                //optional타입의 accountUser가 생성되어야 한다. build를 통해 생성해보겠다
                .willReturn(Optional.of(user));
        // findFirstByOrderByIdDesc
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty()); // 아무 계좌도 없는 상황일 때
        // accountRepository.save => 임의의 값 저장
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000015").build());

        // 원래 로직은 accountNumber + 1한 결과가 리턴되어야 함, 아래를 사용
        // 실제로 저장이 되게 되는 계좌는 captor에 들어감
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        // ctrl + alt + v => 변수 생성
        AccountDto accountDto = accountService.createAccount(1L, 1000L);

        //then
        // accountRepository가 1번 저장을 할 거고 그 떄 captor가 capture할 것임
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(15L, accountDto.getUserid());
        // AccountService에 있는 1000000000 값이 값이 됨
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 생성 실패")
    void createAccount_UserNotFound() {
        // user가 없는 경우

        //given
        // AccountUser는 id, name, date만 가짐
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(15L);
        // findById
        given(accountUserRepository.findById(anyLong()))
                //optional타입의 accountUser가 생성되어야 한다. build를 통해 생성해보겠다
                .willReturn(Optional.empty());
        // AccountService에서 findById가 맨 첫 줄이기 때문에 거기서 에러 나면 아래를 실행 안 함

        // 원래 로직은 accountNumber + 1한 결과가 리턴되어야 함, 아래를 사용
        // 실제로 저장이 되게 되는 계좌는 captor에 들어감
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        // accountService.createAccount 해주면 exception 발생할 것임! -> 우리가 만든 exception뿌리자
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("유저 당 최대 계좌는 10개")
    void createAccount_maxAccountIs10() {
        // 계좌가 10개 이상인 경우 -> 어떻게 테스트 해야 할까?
        // AccountUser를 통해서 accountRepository를 조회해가지고 이 유저가 몇개를 가지고 있나 봐야 함
        // AccountUser안에 manyToOne으로(1대 다 가능) accountUser, accountNumber가 있기 때문에
        // repository에서 accountUser 값을 받아 계좌 갯수 세는 거 가능 => repository에 추가하자

        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(15L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.countByAccountUser(any())) // 아무값을 넣어도
                .willReturn(10); // 10을 리턴할 것이다!

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }


    // 계좌 해지에 대한 service test => 성공 케이스
    @Test
    void deleteAccountSuccess() {
        //given
        AccountUser user = AccountUser.builder() // AccountUser는 id, name, date만 가짐
                .name("Pobi").build();
        user.setId(12L);
        // findById
        given(accountUserRepository.findById(anyLong())) //어느 userId가 들어오든
                .willReturn(Optional.of(user)); // 위의 user를 리턴함
        // findByAccountNumber
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user) // 위에 나왔던 유저와 같은 유저야지면 에러 없음
                        .balance(0L) // balance를 0으로 모킹해야함 => service의 getBalance > 0에서 balance를
                        // 가져오는데 balance는 long타입이기에 값을 가져오므로 0으로 지정해 줘야 함
                        .accountNumber("1000000012").build()));

        // 서비스에서 계좌 해지시 UNREGISTERED로 업데이트 되었는지 확인을 위해 추가
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        // ctrl + alt + v => 변수 생성
        AccountDto accountDto = accountService.deleteAccount(1L, "1234567890");

        //then
        // accountRepository가 1번 저장을 할 거고 그 떄 captor가 capture할 것임
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserid()); // captor 사용 x -> 임의로 user 생성한거여서..?
        assertEquals("1000000012", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());
    }

    // delete에 대해 실패 케이스
    @Test
    @DisplayName("해당 유저 없음 - 계좌 해지 실패")
    void deleteAccount_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong())) // findById로 accountUser 찾는데
                .willReturn(Optional.empty()); // empty가 리턴되도록

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 계좌 해지 실패")
    void deleteAccount_AccountNotFound() {
        //given
        // AccountUser는 id, name, date만 가짐
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);
        // findById
        given(accountUserRepository.findById(anyLong())) //어느 userId가 들어오든
                .willReturn(Optional.of(user)); // 위의 user를 리턴함

        // findByAccountNumber
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름")
    void deleteAccountFailed_userUnMatch() {
        //given
        // AccountUser는 id, name, date만 가짐
        AccountUser pobi = AccountUser.builder()
                .name("Pobi").build();
        pobi.setId(12L);
        AccountUser harry = AccountUser.builder()
                .name("Harry").build();
        harry.setId(13L);
        // findById => userId는 pobi user 이고
        given(accountUserRepository.findById(anyLong())) //어느 userId가 들어오든
                .willReturn(Optional.of(pobi)); // 위의 user를 리턴함
        // findByAccountNumber => 계좌번호의 user는 harry 이다
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(harry) // 위에 나왔던 유저와 같은 유저야지면 에러 없음
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("해지 계좌는 잔액이 없어야 한다")
    void deleteAccountFailed_balanceNotEmpty() {
        //given
        // AccountUser는 id, name, date만 가짐
        AccountUser pobi = AccountUser.builder()
                .name("Pobi").build();
        pobi.setId(12L);
        // findById => userId는 pobi user 이고
        given(accountUserRepository.findById(anyLong())) //어느 userId가 들어오든
                .willReturn(Optional.of(pobi)); // 위의 user를 리턴함
        // findByAccountNumber => 계좌번호의 user는 harry 이다
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi) // 위에 나왔던 유저와 같은 유저야지면 에러 없음
                        .balance(100L)
                        .accountNumber("1000000012").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

    @Test
    @DisplayName("해지 계좌는 해지할 수 없다")
    void deleteAccountFailed_alreadyUnregistered() {
        //given
        // AccountUser는 id, name, date만 가짐
        AccountUser pobi = AccountUser.builder()
                .name("Pobi").build();

        pobi.setId(12L);
        // findById => userId는 pobi user 이고
        given(accountUserRepository.findById(anyLong())) //어느 userId가 들어오든
                .willReturn(Optional.of(pobi)); // 위의 user를 리턴함
        // findByAccountNumber => 계좌번호의 user는 harry 이다
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi) // 위에 나왔던 유저와 같은 유저야지면 에러 없음
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    void successGetAccountsByUserId() {
        //given
        AccountUser pobi = AccountUser.builder()
                        .name("Pobi").build();


        pobi.setId(12L);

        // 가상의 계좌 3개 만들어서 list에 넣어줌
        List<Account> accounts = Arrays.asList(
                Account.builder()
                    .accountUser(pobi) // Account
                    .accountNumber("1111111111")
                    .balance(1000L)
                    .build(),
                Account.builder()
                        .accountUser(pobi)
                        .accountNumber("2222222222")
                        .balance(2000L)
                        .build(),
                Account.builder()
                        .accountUser(pobi)
                        .accountNumber("3333333333")
                        .balance(3000L)
                        .build()
        );
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);

        //when => 이 상황이 주어졌을 때
        // 한줄 다 드래그 후 ctrl + alt + v로 변수 생성
        // ** getAccountsByUserId 호출할 때 AccountDto를 리턴하는데
        // AccountDto에서 .userId를 가져올 때 .getAccountUser()가 null 값이어서 null 에러떴다
        // 위에서 만든 Account 만들 때 accountUser 를 build 해주니 에러가 사라졌다
        List<AccountDto> accountsDtos = accountService.getAccountsByUserId(1L);

        //then => 결과는?
        assertEquals(3, accountsDtos.size());
        assertEquals("1111111111", accounts.get(0).getAccountNumber());
        assertEquals(1000, accounts.get(0).getBalance());
        assertEquals("2222222222", accounts.get(1).getAccountNumber());
        assertEquals(2000, accounts.get(1).getBalance());
        assertEquals("3333333333", accounts.get(2).getAccountNumber());
        assertEquals(3000, accounts.get(2).getBalance());
    }

    @Test
    @DisplayName("사용자 아이디가 없을 때")
    void failedGetAccounts() {
        //given => findById로 user를 조회했는데 empty를 리턴한 경우
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        // userId로 아무 값을 넣어줌
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));

        //then => 아래처럼 뜰 것이다!
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}
