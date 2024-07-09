package pw.avvero.deviriumbot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static java.lang.String.format;

@Slf4j
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
        if (request.content.contains("#draft")) {
            log.debug("Note {} would be ignored because of #draft tag", request.title);
            return;
        }
        String content = request.content;
        if (request.links != null) {
            for (Map.Entry<String, String> link : request.links.entrySet()) {
                String url = format("[%s](%s/%s)", link.getKey(), deviriumLink, link.getValue());
                content = content.replace(format("[[%s]]", link.getKey()), url);
            }
        }
        for (char ch : new char[]{'_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!'}) {
            content = content.replace("" + ch, "\\" + ch);
        }
        telegramService.sendMessage(deviriumChatId, null, content, "MarkdownV2");
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
