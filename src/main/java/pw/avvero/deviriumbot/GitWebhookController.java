package pw.avvero.deviriumbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static java.lang.String.format;

@RestController
public class GitWebhookController {

    private final TelegramService telegramService;
    private final String deviriumChatId;
    private final String deviriumLink;

    public GitWebhookController(TelegramService telegramService,
                                @Value("${devirium.chatId}") String deviriumChatId,
                                @Value("${devirium.link}") String deviriumLink) {
        this.telegramService = telegramService;
        this.deviriumChatId = deviriumChatId;
        this.deviriumLink = deviriumLink;
    }

    @PostMapping("/git/webhook")
    public void process(@RequestBody GitWebhookRequest request) {
        String content = request.content;
        if (request.links != null) {
            for (Map.Entry<String, String> link : request.links.entrySet()) {
                String url = format("[%s](%s/%s)", link.getKey(), deviriumLink, link.getValue());
                content = content.replace(format("[[%s]]", link.getKey()), url);
            }
        }
        telegramService.sendMessage(deviriumChatId, null, content, "markdown");
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Object> handleException(Exception e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Map response = Map.of(false, "Internal server error");
        return new ResponseEntity<>(response, status);
    }

    public record GitWebhookRequest(String title, String content, Map<String, String> links) {
    }
}
