package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.AccountStatus.UNREGISTERED;
import static com.example.account.type.ErrorCode.*;

import java.util.List;
import java.util.stream.Collectors;

@Service // service 타입 bean으로 스프링에 저장해주기 위해 붙임
@RequiredArgsConstructor // final 타입의 생성자를 만들어줌
public class AccountService { // final 로 해주면 생성자가 아니면 값을 못담게 함 = 값 변경 못함
    private final AccountRepository accountRepository; // accountRepository를 활용해 데이터를 저장하도록 함
    private final AccountUserRepository accountUserRepository; // 사용자 조회를 위해 accountRepository 의존

    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {
        // 서비스가 리포지토리갔다가 온 후 컨트롤러에는 AccountDto에 해당하는 정보만 넘겨주도록

        // 사용자 조회
        // findById 메서드로 조회 가능, alt + ctrl + v로 변수 바로 생성
        AccountUser accountUser = getAccountUser(userId);
        // 우리 비즈니스 상황에 맞는 exception들이 없으므로 custom exception 생성하기

        validateCreateAccount(accountUser);

        // 열자리 값으로 이루어진 계좌 번호 생성
        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
                .map(account -> (Integer.parseInt(account.getAccountNumber())) + 1 + "") // 다시 문자열로
                .orElse("1000000000"); // 계좌가 하나도 없었을 경우 , desc 못했을 경우

        // 계좌 저장시킴
        // Account.builder()로 생성한 것을 accountRepository에 저장하고
        // Account Entity를 활용해서 AccountDto.fromEntity에 account로 넣어줌 AccountDto를 생성 후 리턴함
        return AccountDto.fromEntity(
                accountRepository.save(Account.builder() // Account 클래스에 있는 프로퍼티들 넣어줌
                        .accountUser(accountUser) // 위에서 findById로 찾은 값 넣어줌
                        .accountStatus(IN_USE)
                        .accountNumber(newAccountNumber) // 위에서 생성한 계좌번호 넣어줌
                        .balance(initialBalance) // 계좌만들때 파라미터로 받아온 잔액
                        .registeredAt(LocalDateTime.now())
                        .build())
        );
    }

    // 계좌가 10개 이상인 경우 => 이러한 VALIDATION 코드들은 빼놓는것이 코드 전체 이해에 좋다
    private void validateCreateAccount(AccountUser accountUser) {
        if(accountRepository.countByAccountUser(accountUser) >= 10) {
        throw new AccountException(MAX_ACCOUNT_PER_USER_10); // ErrorCode 추가
    }
}

    @Transactional
    public Account getAccount(Long id) { // 값을 받아오기 때문에 return 필요함
        if (id < 0) {
            throw new RuntimeException("Minus");
        }
        return accountRepository.findById(id).get();
    }

    // 아래 메소드는 컨트롤러에서 deleteAccount return에 먼저 작성해주고 alt + enter로 메서드 생성하면 간편함
    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        // 유저 없음, 계좌 번호 없음, 사용자 아이디와 계좌 소유주 다름, 계좌가 이미 해지 상태, 잔액이 있는 경우
        // 위 5가지에 대한 validation을 실행하고 5가지에 다 걸리지 않으면 계좌 해지하고 응답 줄 것임

        // 1. 유저 없으면 에러 띄우기 => userId 없으면 에러
        AccountUser accountUser = getAccountUser(userId);
        // 2. 계좌 번호 없으면 에러 띄우기 => accountNumber 없으면 에러
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        // 아래 먼저 써주고 alt + enter로 메서드 생성
        validateDeleteAccount(accountUser, account);

        // 여기까지 통과하면 계좌를 해지해도 되는 상태이다 => 계좌해지할 떄 상태 업데이트, 해지 시간 부여
        account.setAccountStatus(AccountStatus.UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        // 테스트를 위해 필요한 부분
        // 불필요한 부분이지만 account를 일부러 넣어서 account에 UNREGISTERED 상태값 들어갔는지 test 위해 작성
        accountRepository.save(account);

        return AccountDto.fromEntity(account);
    }

    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        // userId와 account에서 get한 userId가 다르다면 => id로 비교, !=로 했는데 더 안전한거 추천해준거 적용
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }
        // 계좌가 이미 해지된 경우
        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }
        if (account.getBalance() > 0) { // http 계좌 생성시 balance 10000 넣어줘서 해지가 안됨 -> 0을 넣어줌
            throw  new AccountException(BALANCE_NOT_EMPTY);
        }
    }

    @Transactional // transactional이 없으면 정상적인 조회가 안됨
    public List<AccountDto> getAccountsByUserId(Long userId) {
        AccountUser accountUser = getAccountUser(userId);
        // ctrl + alt + v 하면 변수 추천
        // findByAccountUser의 데이터 타입은 List<Account>임 => 계좌가 여러개일 수 있으므로
        List<Account> accounts = accountRepository.findByAccountUser(accountUser);

        // accounts의 데이터타입인 List<Account>를 List<AccountDto> 타입으로 바꿔서 리턴 시켜야 함
        return accounts.stream()
                // fromEntity는 Account를 받아서 AccountDto를 반환해주는 메소드이다
                .map(AccountDto::fromEntity) // 변환됨
                .collect(Collectors.toList()); // 변환된 걸 list로 다시 받아줌
    }

    private AccountUser getAccountUser(Long userId) {
        AccountUser accountUser = accountUserRepository.findById(userId) // userId를 말함
                // ErrorCode 에서 alt + enter 눌르면 에러 코드 간결하게 쓸 수 있음
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
        return accountUser;
    }
}
