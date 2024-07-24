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
    private final PublicationService publicationService;
    private final String gardenerChatId;

    public TelegramWebhookController(TelegramService telegramService,
                                     PublicationService publicationService,
                                     @Value("${devirium.gardenerChatId}") String gardenerChatId) {
        this.telegramService = telegramService;
        this.publicationService = publicationService;
        this.gardenerChatId = gardenerChatId;
    }

    @PostMapping("/telegram/webhook")
    public void process(@RequestBody TelegramWebhookMessage request) {
        if (request.message.replayToMessage == null) return;
        if (!gardenerChatId.equals(request.message.chat.id)) {
            telegramService.sendMessage(gardenerChatId, "Unsupported call from chat " + request.message.chat.id, "markdown");
            return;
        }
        publicationService.publishAfterReview(request.message.replayToMessage.id);
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
        telegramService.sendMessage(gardenerChatId, e.getMessage(), "markdown");
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
