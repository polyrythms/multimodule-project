package ru.polyrythms.telegrambot.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_cities")
@Getter
@Setter
@NoArgsConstructor
public class GroupCityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long groupChatId;
    private Long cityId;
    private LocalDateTime assignedAt;
}