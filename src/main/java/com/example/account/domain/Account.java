package com.example.account.domain;

import com.example.account.exception.AccountException;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
// AuditingEntityListener -> config패키지 JpaAuditing에 저장해야 작동됨
@EntityListeners(AuditingEntityListener.class)
public class Account extends BaseEntity{


    @ManyToOne // 계좌 : 유저 = n : 1
    private AccountUser accountUser;
    private String accountNumber;

    @Enumerated(EnumType.STRING) // 0123으로 저장 안되고 AccountStatus 문자 그대로 저장시키기 위해
    private AccountStatus accountStatus;
    private Long balance;

    private LocalDateTime registeredAt;
    private LocalDateTime unRegisteredAt;


    public void useBalance(Long amount) {
        if (amount > balance) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }

        balance -= amount;
    }

    // 금액 사용 취소 요청
    public void cancelBalance(Long amount) {
        if (amount < 0) { // 0보다 작은 수를 입력하면 안된다
            throw new AccountException(ErrorCode.INVALID_REQUEST);
        }

        balance += amount;
    }
}
