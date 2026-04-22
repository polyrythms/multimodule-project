package ru.polyrythms.telegrambot.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import ru.polyrythms.telegrambot.application.port.input.*;
import ru.polyrythms.telegrambot.application.port.output.*;
import ru.polyrythms.telegrambot.application.service.*;
import ru.polyrythms.telegrambot.infrastructure.adapter.output.telegram.TelegramBotClient;

@Configuration
@EnableAsync
public class BeanConfig {

    @Bean
    public AdminManagementUseCase adminManagementUseCase(AdminRepository adminRepository) {
        return new AdminManagementService(adminRepository);
    }

    @Bean
    public GroupManagementUseCase groupManagementUseCase(
            TelegramGroupRepository groupRepository,
            AdminRepository adminRepository) {
        return new GroupManagementService(groupRepository, adminRepository);
    }

    @Bean
    public CommandHandlingUseCase commandHandlingUseCase(
            AdminManagementUseCase adminManagementUseCase,
            GroupManagementUseCase groupManagementUseCase,
            MessageSender messageSender,
            WeatherAdminUseCase weatherAdminUseCase,
            WeatherUserUseCase weatherUserUseCase,
            TelegramBotClient telegramBotClient) {
        return new CommandHandlingService(
                adminManagementUseCase,
                groupManagementUseCase,
                messageSender,
                weatherAdminUseCase,
                weatherUserUseCase,
                telegramBotClient);
    }

    @Bean
    public DecryptionResultHandlingUseCase decryptionResultHandlingUseCase(
            MessageSender messageSender) {
        return new DecryptionResultHandlingService(messageSender);
    }
}