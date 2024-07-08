package pw.avvero.deviriumbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class GitWebhookController {

    private final TelegramService telegramService;
    private final String deviriumChatId;

    public GitWebhookController(TelegramService telegramService,
                                @Value("${devirium.chatId}") String deviriumChatId) {
        this.telegramService = telegramService;
        this.deviriumChatId = deviriumChatId;
    }

    @PostMapping("/git/webhook")
    public void process(@RequestBody GitWebhookRequest request) {
        telegramService.sendMessage(deviriumChatId, null, request.content);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Object> handleException(Exception e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Map response = Map.of(false, "Internal server error");
        return new ResponseEntity<>(response, status);
    }

    public record GitWebhookRequest(String title, String content) {
    }
}
