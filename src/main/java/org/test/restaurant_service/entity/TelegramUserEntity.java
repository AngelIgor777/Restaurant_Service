package org.test.restaurant_service.entity;

import lombok.*;

import javax.persistence.Table;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_user", schema = "restaurant_service")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TelegramUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "chat_id", unique = true, nullable = false)
    private Long chatId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String firstname;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}