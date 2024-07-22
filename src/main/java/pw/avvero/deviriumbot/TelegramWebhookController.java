package pw.avvero.deviriumbot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TelegramWebhookController {

    private final TelegramService telegramService;
    private final String deviriumChatId;
    private final String gardenerChatId;

    public TelegramWebhookController(TelegramService telegramService,
                                     @Value("${devirium.chatId}") String deviriumChatId,
                                     @Value("${devirium.gardenerChatId}") String gardenerChatId) {
        this.telegramService = telegramService;

        this.deviriumChatId = deviriumChatId;
        this.gardenerChatId = gardenerChatId;
    }

    @PostMapping("/telegram/webhook")
    public void process(@RequestBody TelegramWebhookMessage request) {
        if (request.message.replayToMessage == null) return;
        if (!gardenerChatId.equals(request.message.chat.id)) {
            throw new RuntimeException("Unsupported call from chat " + request.message.chat.id);
        }
        String content = request.message.replayToMessage.text;
        telegramService.sendMessage(deviriumChatId, null, content, "MarkdownV2");
    }

    public record TelegramWebhookMessage(Message message) {
    }

    public record Message(@JsonProperty("message_id") String id,
                          Chat chat,
                          @JsonProperty("reply_to_message") Message replayToMessage,
                          String text) {
    }

    public record Chat(String id) {
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Object> handleException(Exception e) {
        log.error(e.getLocalizedMessage(), e);
        telegramService.sendMessage(gardenerChatId, null, e.getMessage(), "markdown");
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
