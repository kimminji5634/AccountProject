package com.example.account.domain;

// table과 1대 1로 매칭되는 엔티티의 객체임

import com.example.account.type.AccountStatus;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Transaction extends BaseEntity{ // Transaction 에 들어가는 모든 컬럼 다 써줘야 함 = transaction entity

    // 실제 비즈니스에 쓰일 부분 ///////여기부터
    @Enumerated(EnumType.STRING) // 0123 이런 기본 타입으로 저장하지 않고 문자열로 저장하기 위해 string으로 지정함
    private TransactionType transactionType; // TransactionType 에 대해 enum 클래스 생성

    @Enumerated(EnumType.STRING)
    private TransactionResultType transactionResultType;

    @ManyToOne // 특정 account 한개에 여러 개의 거래가 발생할 수 있도록
    private Account account;

    private Long amount;// 거래금액
    private Long balanceSnapshot;

    private String transactionId; // 거래 고유 id, pk를 그대로 쓰면 보안상, 비즈니스적으로도 좋지 않다
    private LocalDateTime transactedAt; // 거래 시간 스냅샷
    ///////// 여기까지

}
