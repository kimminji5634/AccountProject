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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;

@Slf4j
@Service // 빈으로 등록
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;

    // 요청 파라미터 받아서 TransactionDto로 리턴함
    @Transactional // update와 insert가 동시에 일어나거나 동시에 일어나지 않거나 한다
    public TransactionDto useBalance(Long userId, String accountNumber, Long amount) {
        // 1. userId로 유저 정보 가져오기 => user는 클래스가 AccountUser이므로 4개의 정보를 다 담고 있음
        AccountUser user = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateUseBalanace(user, account, amount); // 정책, 메소드 만들어주는건 alt + enter

        // balance(잔액) - amount => Account 클래스의 useBalance 메서드 실행!!
        // update
        account.useBalance(amount);

        // useBalance 메서드를 실행시 실패할 때 저장되는 곳이 없다
        // validateUseBalanace exception이 발생했을 때 저장해주는 부분이 없으므로 컨트롤러에서 try ~ catch

        // transactionRepository 에 저장해주세요
        // insert
        // saveAndGetTransaction도 아래의 메소드와 거의 동일 f -> s 차이
        return TransactionDto.fromEntity(saveAndGetTransaction(USE, S, account, amount));
    }

    /** 정책(실패 응답이 나오는 예외 케이스)
     * 사용자 없는 경우, 계좌 없는 경우, 사용자 아이디와 계좌 소유주 다른 경우,
     * 계좌가 이미 해지된 경우, 거래금액이 잔액보다 큰 경우,
     * 거래금액이 너무 적거나 큰 경우
     */
    private void validateUseBalanace(AccountUser user, Account account, Long amount) {
        // 사용자 없는 경우, 계좌 없는 경우는 이미 위에서 에러 처리함
        // 거래금액이 너무 적거나 큰 경우는 컨트롤러 requestbody 에서 @Valid로 요청 값 min, max 처리함
        // 사용자 아이디와 계좌 소유주 다른 경우
        if (!Objects.equals(user.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }
        if (account.getAccountStatus() != AccountStatus.IN_USE) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }
        if (account.getBalance() < amount) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }
    }

    // 계좌번호가 없는 경우
    @Transactional
    public void saveFailedUseTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 아래의 return절 반복되므로 메서드로 빼기 => 가독성, ctrl + alt + m
        saveAndGetTransaction(USE, F, account, amount);
    }

    private Transaction saveAndGetTransaction(
            TransactionType transactionType,
            TransactionResultType transactionResultType,
            Account account,
            Long amount) {
        return transactionRepository.save( // Transaction(entity) 에 build 해주세요
                Transaction.builder() // Transaction 보면서 프로퍼티 하나씩 넣으면 된다
                        .transactionType(transactionType)
                        .transactionResultType(transactionResultType) // 실패
                        .account(account)
                        .amount(amount)
                        .balanceSnapshot(account.getBalance())
                        // 고유한 값 만들어야 함 => UUID(UNIVERSIAL UNIQUE IDENTIFIER)를 사용한다
                        // UUID를 랜덤으로 받고 문자열로 바꿔주고 -를 제거해준다
                        .transactionId(UUID.randomUUID().toString().replace("-", ""))
                        .transactedAt(LocalDateTime.now())
                        .build()
        );
    }

    @Transactional
    public TransactionDto cancelBalance(String transactionId,
                                        String accountNumber, Long amount
    ) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 예외 처리, 정책에 대한 로직
        validateCancelBalance(transaction, account, amount); // 자동 메소드 생성은 alt + enter

        // Transaction 테이블이 아닌 Account 테이블에 balance 컬럼 있으므로
        // Account entity 테이블에 금액 사용 취소 로직 추가해줌
        account.cancelBalance(amount);

        // 트랜잭션에 저장하는 메소드인 saveAndGetTransaction 의 내용을 가져와 쓰자
        // transactionType 을 파라미터에 넣어주어 transactionType 값이 달라도 쓸 수 있도록 한다
        // 사용 취소를 저장
        return TransactionDto.fromEntity(saveAndGetTransaction(CANCEL, S, account, amount));
    }

    private void validateCancelBalance(Transaction transaction, Account account, Long amount) {
        // 거래에서 쓸 계좌 id 와 계좌 id가 다를 때
        if(!Objects.equals(transaction.getAccount().getId(), account.getId())){
            throw new AccountException(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH);
        }
        // 거래 금액이 거래 취소 금액과 다를 때
        if (!Objects.equals(transaction.getAmount(), amount)) {
            throw new AccountException(ErrorCode.CANCEL_MUST_FULLY);
        }
        // 1년이 넘은 거래는 사용 취소 불가능
        // isBefore, minusYears 제공해줌
        if (transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))) {
            throw new AccountException(ErrorCode.TOO_OLD_ORDER_TO_CANCEL);
        }
    }

    @Transactional
    public void saveFailedCancelTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 아래의 return절 반복되므로 메서드로 빼기 => 가독성, ctrl + alt + m
        saveAndGetTransaction(CANCEL, F, account, amount);
    }

    public TransactionDto queryTransaction(String transactionId) {
        // 거래가 있었는지
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND));

        return TransactionDto.fromEntity(transaction);
    }
}
