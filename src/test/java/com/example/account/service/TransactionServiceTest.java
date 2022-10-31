package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.account.type.AccountStatus.*;
import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class) // Mockito 확장 팩을 달아줌 => Mockito로 진행
class TransactionServiceTest {
    public static final long USE_AMOUNT = 200L;
    public static final long CANCEL_AMOUNT = 200L;
    @Mock // 가짜로 만듦
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks // 위의 Mock 들을 TransactionService에 주입시킴
    private TransactionService transactionService;

    @Test
    void successUseBalance() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);

        Account account = Account.builder()
                .accountUser(user) // userId와 계좌 소유주 일치해야 함
                // TransactionService validateUseBalance에서 IN_USE가 아니면 에러이므로 설정해줘야함
                .accountStatus(IN_USE)
                .balance(10000L) // 만원 넣어줄 것임
                .accountNumber("1000000012").build();

        given(accountUserRepository.findById(anyLong())) // AccountUser entity와 관련
                .willReturn(Optional.of(user)); // 위의 변수 user 를 말함

        given(accountRepository.findByAccountNumber(anyString())) // Account entity와 관련
                .willReturn(Optional.of(account)); // 위의 변수 account 를 말함

        // TransactionDto fromEntity 메서드에서
        // .accountNumber(transaction.getAccount().getAccountNumber() => NullPointerException 발생
        // Transaction을 request함 => Transaction을 리턴해주는 service의 saveGetTransaction 메소드 가기
        // 그 메소드의 리턴값을 아래에서 값을 넘겨줘야 함
        given(transactionRepository.save(any())) // service 의 saveAndGetTransaction 메서드
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());

        // when 에서 넣어준 amount가 잘 동작하는지 보기 위해 captor를 사용한다
        // then 에서 확인할 때 transaction 에 있는 프로퍼티 확인 가능!
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        // when의 amount는 영향을 안 주는게 useBalance 실행시 account.useBalance(amount)를
        // 실행하지만 return 값인 saveAndGetTransaction 에서 save한 값을 가져오기 때문에!
        TransactionDto transactionDto = transactionService.useBalance(1L,
                "1000000000", USE_AMOUNT); // balance - amount 로직 있음
        //then
        // TransactionDto에서 확인해줄 수 있는 것들 확인하기, trasactionId는 난수이므로 확인불가
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        // given 에 넣어준 최초 잔액이 10000원임
        assertEquals(9800L, captor.getValue().getBalanceSnapshot());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapshot()); // balance : 잔액
        assertEquals(1000L, transactionDto.getAmount()); // amount : 사용금액

    }

    /** 정책(실패 응답이 나오는 예외 케이스)
     * 사용자 없는 경우, 계좌 없는 경우, 사용자 아이디와 계좌 소유주 다른 경우,
     * 계좌가 이미 해지된 경우, 거래금액이 잔액보다 큰 경우,
     * 거래금액이 너무 적거나 큰 경우
     */

    @Test
    @DisplayName("해당 유저 없음 - 잔액 사용 실패")
    void useBalance_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong())) // findById로 accountUser 찾는데
                .willReturn(Optional.empty()); // empty가 리턴되도록

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000" , 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 실패")
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
                () -> transactionService.useBalance(1L, "1000000000" , 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 잔액 사용 실패")
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
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("해지 계좌는 사용할 수 없다")
    void deleteAccountFailed_alreadyUnregistered() {
        //given
        AccountUser pobi = AccountUser.builder()

                .name("Pobi").build();
        pobi.setId(12L);
        given(accountUserRepository.findById(anyLong())) //어느 userId가 들어오든
                .willReturn(Optional.of(pobi)); // 위의 user를 리턴함
        // findByAccountNumber => 계좌번호의 user는 harry 이다
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi) // 위에 나왔던 유저와 같은 유저야지면 에러 없음
                        .accountStatus(AccountStatus.UNREGISTERED) // 이미 해지된 상태!!!!
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액이 잔액보다 큰 경우")
    void exceedAmount_UseBalance() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);

        Account account = Account.builder()
                .accountUser(user) // userId와 계좌 소유주 일치해야 함
                // TransactionService validateUseBalance에서 IN_USE가 아니면 에러이므로 설정해줘야함
                .accountStatus(IN_USE)
                .balance(100L) // 처음 있는 잔액
                .accountNumber("1000000012").build();

        given(accountUserRepository.findById(anyLong())) // AccountUser entity와 관련
                .willReturn(Optional.of(user)); // 위의 변수 user 를 말함

        given(accountRepository.findByAccountNumber(anyString())) // Account entity와 관련
                .willReturn(Optional.of(account)); // 위의 변수 account 를 말함

        // 이 아래까지 돌지 않으므로 모킹 필요 없음 => given 에서 build 해주는 것

        //when
        //then
        // amount(사용할금액) 이 1000원일 때
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
    }

    // saveFailedUseTransaction 테스트, 실제로 뭐가 저장되는지 보는게 중요하기에 successtest 에서 가져옴
    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    void saveFailedUseTransaction() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);

        Account account = Account.builder()
                .accountUser(user) // userId와 계좌 소유주 일치해야 함
                // TransactionService validateUseBalance에서 IN_USE가 아니면 에러이므로 설정해줘야함
                .accountStatus(IN_USE)
                .balance(10000L) // 만원 넣어줄 것임
                .accountNumber("1000000012").build();

        given(accountRepository.findByAccountNumber(anyString())) // Account entity와 관련
                .willReturn(Optional.of(account)); // 위의 변수 account 를 말함

        // saveAndGetTransaction 메소드 사용하는데 리턴 값을 잘 모킹해놨다
        given(transactionRepository.save(any())) // service 의 saveAndGetTransaction 메서드
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());

        // when 에서 넣어준 amount가 잘 동작하는지 보기 위해 captor를 사용한다
        // then 에서 확인할 때 transaction 에 있는 프로퍼티 확인 가능!
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        // 테스트 해야할 대상 메소드를 적어줌!!!!, 이 메소드의 리턴타입 없고 void 이기 때문에 이렇게만 한다
        transactionService.saveFailedUseTransaction(
                "1000000000", USE_AMOUNT); // balance - amount 로직 있음

        //then
        // 아래는 transactionRepository.save를 리턴한다 => build 정보를 한 번 캡처함
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        // 거래가 실패되었으므로 남은 잔액은 초기 잔액과 같아야 한다 => 변동이 없어야 한다
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        // 실패했으므로 F가 되어야 한다
        assertEquals(F, captor.getValue().getTransactionResultType());
    }

    @Test
    void successCancelBalance() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);

        Account account = Account.builder()
                .accountUser(user) // userId와 계좌 소유주 일치해야 함
                // TransactionService validateUseBalance에서 IN_USE가 아니면 에러이므로 설정해줘야함
                .accountStatus(IN_USE)
                .balance(10000L) // 만원 넣어줄 것임
                .accountNumber("1000000012").build();

        Transaction transaction = Transaction.builder() // 원래 거래를 찾아줌
                .account(account)
                .transactionType(USE) // USE일 때 amount와 아래의 CANCEL일 때 amount가 같아야 함
                .transactionResultType(S) // Success 여야 함
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString())) // Account entity와 관련
                .willReturn(Optional.of(account)); // 위의 변수 account 를 말함

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder() // 원래 거래를 찾아줌
                        .account(account)
                        .transactionType(CANCEL) // CANCEL 여야 함 => 사용 금액 취소
                        .transactionResultType(S) // Success 여야 함
                        .transactionId("transactionIdForCancel")
                        .transactedAt(LocalDateTime.now())
                        .amount(CANCEL_AMOUNT)
                        .balanceSnapshot(10000L)
                        .build());

        // when 에서 넣어준 amount가 잘 동작하는지 보기 위해 captor를 사용한다
        // then 에서 확인할 때 transaction 에 있는 프로퍼티 확인 가능!
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        // when의 amount는 영향을 안 주는게 useBalance 실행시 account.useBalance(amount)를
        // 실행하지만 return 값인 saveAndGetTransaction 에서 save한 값을 가져오기 때문에!
        TransactionDto transactionDto = transactionService.cancelBalance("transactionId",
                "1000000000", CANCEL_AMOUNT); // balance - amount 로직 있음
        //then
        // TransactionDto에서 확인해줄 수 있는 것들 확인하기, trasactionId는 난수이므로 확인불가
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        // given 에 넣어준 최초 잔액(account)이 10000원임
        assertEquals(10000L + CANCEL_AMOUNT, captor.getValue().getBalanceSnapshot());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(10000L, transactionDto.getBalanceSnapshot()); // balance : 잔액
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount()); // amount : 사용금액

    }





    // 예외에 대한 test => TransactionService 의 cancelBalance 메소드를 보며 작업할 것
    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 취소 실패")
    void cancelTransaction_AccountNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                // 여기서 에러가 발생할 것이기 때문에 모킹 단순화 시켰음
                .willReturn(Optional.of(Transaction.builder().build()));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000" , 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("원 사용 거래 없음 - 잔액 사용 취소 실패")
    void cancelTransaction_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                // 여기서 에러가 발생할 것이기 때문에 모킹 단순화 시켰음
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000" , 1000L));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    // validation - 1
    @Test
    @DisplayName("거래와 계좌가 매칭실패 - 잔액 사용 취소 실패")
    void cancelTransaction_TransactionAccountUnMatch() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);

        // NullPointerError 떴다 => validateCancelBalance에서 account id를 가져온다
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        user.setId(1L);

        Account accountNotUse = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000013").build();
        user.setId(2L);

        Transaction transaction = Transaction.builder() // 원래 거래를 찾아줌
                .account(account) // account
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));  // account!!!!!!!

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotUse)); // accountNotUse!!!!!!

        //when
        // CANCEL_MUST_FULLY 에러가 났는데 Transaction amount와 취소시킬 when절의 amount일치 시켜야 함
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1000_000_000" ,CANCEL_AMOUNT));

        //then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    // validation - 2
    @Test
    @DisplayName("거래금액과 취소금액이 다름 - 잔액 사용 취소 실패")
    void cancelTransaction_CancelMustFully() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);

        // NullPointerError 떴다 => validateCancelBalance에서 account id를 가져온다
        Account account = Account.builder()
                // 따라서 id를 추가해줌
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        user.setId(12L);

        Transaction transaction = Transaction.builder() // 원래 거래를 찾아줌
                .account(account) // account
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT + 1000L)
                .balanceSnapshot(9000L)
                .build();


        // 거래 계좌와 계좌가 일치함
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        // 금액이 다름 !! transaction 은 CANCEL_AMOUNT + 1000 임
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1000000000" , CANCEL_AMOUNT));

        //then
        assertEquals(ErrorCode.CANCEL_MUST_FULLY, exception.getErrorCode());
    }

    // validation - 3
    @Test
    @DisplayName("취소는 1년까지만 가능 - 잔액 사용 취소 실패")
    void cancelTransaction_TooOldOrder() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);

        // NullPointerError 떴다 => validateCancelBalance에서 account id를 가져온다
        Account account = Account.builder()
                // 따라서 id를 추가해줌
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        user.setId(12L);

        Transaction transaction = Transaction.builder() // 원래 거래를 찾아줌
                .account(account) // account
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1)) // 1년 딱 돼도 실패임
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();


        // 거래 계좌와 계좌가 일치함
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        // 금액도 일치함
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1000000000" , CANCEL_AMOUNT));

        //then
        assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCEL, exception.getErrorCode());
    }


    // 성공 케이스
    @Test
    void successQueryTransaction() {
        //given
        // 트랜잭션이 있으려면 Account 있어야 하고 Account 있으려면 AccountUser가 있어야 한다
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);


        // NullPointerError 떴다 => validateCancelBalance에서 account id를 가져온다
        Account account = Account.builder()
                // 따라서 id를 추가해줌
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        user.setId(12L);

        Transaction transaction = Transaction.builder() // 원래 거래를 찾아줌
                .account(account) // account
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1)) // 1년 딱 돼도 실패임
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        //when
        TransactionDto transactionDto = transactionService.queryTransaction("trxId");

        //then
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());
    }

    // 예외 케이스
    @Test
    @DisplayName("원거래 없음 - 거래 조회 실패")
    void queryTransaction_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                // 여기서 에러가 발생할 것이기 때문에 모킹 단순화 시켰음
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }
}