package ru.polyrythms.telegrambot.infrastructure.adapter.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.polyrythms.telegrambot.application.port.output.AdminRepository;
import ru.polyrythms.telegrambot.domain.model.AdminUser;
import ru.polyrythms.telegrambot.domain.model.AdminRole;
import ru.polyrythms.telegrambot.infrastructure.adapter.mapper.AdminEntityMapper;
import ru.polyrythms.telegrambot.infrastructure.entity.AdminUserEntity;
import ru.polyrythms.telegrambot.infrastructure.repository.AdminUserJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AdminRepositoryImpl implements AdminRepository {

    private final AdminUserJpaRepository jpaRepository;
    private final AdminEntityMapper mapper;

    @Override
    public Optional<AdminUser> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return jpaRepository.existsByUserId(userId);
    }

    @Override
    public AdminUser save(AdminUser adminUser) {
        AdminUserEntity entity = mapper.toEntity(adminUser);
        AdminUserEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(AdminUser adminUser) {
        AdminUserEntity entity = mapper.toEntity(adminUser);
        jpaRepository.delete(entity);
    }

    @Override
    public List<AdminUser> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminUser> findByRole(AdminRole role) {
        return jpaRepository.findByRoleIn(List.of(role)).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByRole(AdminRole role) {
        return jpaRepository.countByRole(role);
    }
}