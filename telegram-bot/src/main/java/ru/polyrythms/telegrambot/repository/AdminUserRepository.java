package ru.polyrythms.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.polyrythms.telegrambot.entity.AdminUser;
import ru.polyrythms.telegrambot.entity.AdminRole;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<AdminUser> findByRoleIn(List<AdminRole> roles);

    @Query("SELECT COUNT(a) FROM AdminUser a WHERE a.role = :role")
    long countByRole(AdminRole role);
}