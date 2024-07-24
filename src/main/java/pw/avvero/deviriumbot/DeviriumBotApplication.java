package pw.avvero.deviriumbot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@SpringBootApplication
public class DeviriumBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeviriumBotApplication.class, args);
    }

    @Component
    public class StartupEventListener implements ApplicationListener<ContextRefreshedEvent> {

        private final TelegramService telegramService;
        private final String deviriumChatId;
        private final String gardenerChatId;

        public StartupEventListener(TelegramService telegramService,
                                         @Value("${devirium.chatId}") String deviriumChatId,
                                         @Value("${devirium.gardenerChatId}") String gardenerChatId) {
            this.telegramService = telegramService;
            this.deviriumChatId = deviriumChatId;
            this.gardenerChatId = gardenerChatId;
        }

        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            try {
                telegramService.sendMessage(gardenerChatId, "Bot is ready to work", "markdown");
            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }
    }
}
