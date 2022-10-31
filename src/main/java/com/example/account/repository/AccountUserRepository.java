package com.example.account.repository;

import com.example.account.domain.AccountUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository // repository 로 등록해줌
public interface AccountUserRepository extends JpaRepository<AccountUser, Long> {
    // extends JpaRepository<여기서 연결할 테이블, pk 타입>
    // 인터페이스이므로 여기에 구현하지 않음
}




