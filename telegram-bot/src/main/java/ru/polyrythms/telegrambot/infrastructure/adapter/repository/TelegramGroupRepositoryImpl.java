package ru.polyrythms.telegrambot.infrastructure.adapter.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.polyrythms.telegrambot.application.port.output.TelegramGroupRepository;
import ru.polyrythms.telegrambot.domain.model.TelegramGroup;
import ru.polyrythms.telegrambot.infrastructure.adapter.mapper.TelegramGroupEntityMapper;
import ru.polyrythms.telegrambot.infrastructure.entity.TelegramGroupEntity;
import ru.polyrythms.telegrambot.infrastructure.repository.TelegramGroupJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TelegramGroupRepositoryImpl implements TelegramGroupRepository {

    private final TelegramGroupJpaRepository jpaRepository;
    private final TelegramGroupEntityMapper mapper;

    @Override
    public Optional<TelegramGroup> findByChatId(Long chatId) {
        return jpaRepository.findByChatId(chatId)
                .map(mapper::toDomain);
    }

    @Override
    public List<TelegramGroup> findAllActive() {
        return jpaRepository.findByIsActiveTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TelegramGroup> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TelegramGroup> findByAddedBy(Long addedBy) {
        return jpaRepository.findByAddedBy(addedBy).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByChatId(Long chatId) {
        return jpaRepository.existsByChatId(chatId);
    }

    @Override
    public TelegramGroup save(TelegramGroup group) {
        TelegramGroupEntity entity = mapper.toEntity(group);
        TelegramGroupEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(TelegramGroup group) {
        TelegramGroupEntity entity = mapper.toEntity(group);
        jpaRepository.delete(entity);
    }

    @Override
    public long countActive() {
        return jpaRepository.countByIsActiveTrue();
    }

    @Override
    public long countByAddedBy(Long userId) {
        return jpaRepository.countByAddedBy(userId);
    }
}