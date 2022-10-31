package com.example.account.repository;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository // bean 으로 등록하기 위해 붙임
// extends 기능 확장 : 스프링에서 jpa 를 훨씬 쓰기 쉽게 만들어주는 기능임
// JpaRepository interface 를 상속받음
public interface TransactionRepository
        extends JpaRepository<Transaction, Long> { // <Entity, Entity의 pk 타입>

    Optional<Transaction> findByTransactionId(String transactionId);
}
