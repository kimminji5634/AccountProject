package com.example.account.repository;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
@Repository // bean 으로 등록하기 위해 붙임
// extends 기능 확장 : 스프링에서 jpa 를 훨씬 쓰기 쉽게 만들어주는 기능임
public interface AccountRepository extends JpaRepository<Account, Long> { // <Entity, Entity의 pk 타입>
    Optional<Account> findFirstByOrderByIdDesc();
    // 값이 있을 수도 있고 없을 수도 있기 때문에 Optional을 붙여줌 => 테이블에 data가 아직 없을 때!
    // 형식에 맞춰 쓰면 자동으로 쿼리 생성해줌

    // 단순히 카운트만 해주기 때문에 Integer 사용
    Integer countByAccountUser(AccountUser accountUser);

    Optional<Account> findByAccountNumber(String accountNumber);

    // JPA에서 지원하는 기능 :  Account에 AccountUser가 포함되어 있기 때문에
    // 아래의 메서드가 인터페이스 내에서 자동으로 생성됨!!!
    List<Account> findByAccountUser(AccountUser accountUser);
}
