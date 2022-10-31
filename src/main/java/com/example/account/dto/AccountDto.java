package com.example.account.dto;

import com.example.account.domain.Account;
import lombok.*;

import java.time.LocalDateTime;

// Account Entity 클래스와 거의 비슷한데 단순한 버전으로 딱 필요한 것만 넣어놓는 게 기본
// 아래의 정보정도면 controller에서 필요한 모든 정보를 포함한다
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDto {
    private Long userid;
    private String accountNumber;
    private Long balance;

    private LocalDateTime registeredAt;
    private LocalDateTime unRegisteredAt;

    // 특정 Entity에서 특정 dto로 변환해줄 때 아래의 메소드를 만들어 생성자를 쓰지 않고
    // 아래 메소드 생성자를 통해서 생성을 해주면 좀 더 깔끔한 생성을 할 수 있다 => 더 안전함
    public static AccountDto fromEntity(Account account) {
        return AccountDto.builder()
                .userid(account.getAccountUser().getId()) //id가 클래스 한번더 들어가야 있음
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .registeredAt(account.getRegisteredAt())
                .unRegisteredAt(account.getUnRegisteredAt())
                .build();
    }
}
