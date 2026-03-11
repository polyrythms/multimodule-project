package ru.polyrythms.telegrambot.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.polyrythms.telegrambot.infrastructure.entity.AdminUserEntity;
import ru.polyrythms.telegrambot.domain.model.AdminRole;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminUserJpaRepository extends JpaRepository<AdminUserEntity, Long> {

    Optional<AdminUserEntity> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<AdminUserEntity> findByRoleIn(List<AdminRole> roles);

    @Query("SELECT COUNT(a) FROM AdminUserEntity a WHERE a.role = :role")
    long countByRole(AdminRole role);
}