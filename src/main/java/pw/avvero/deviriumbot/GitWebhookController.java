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
    private final TelegramMessageMapper mapper;
    private final String deviriumChatId;
    private final String gardenerChatId;

    public GitWebhookController(TelegramService telegramService,
                                TelegramMessageMapper mapper,
                                @Value("${devirium.chatId}") String deviriumChatId,
                                @Value("${devirium.gardenerChatId}") String gardenerChatId) {
        this.telegramService = telegramService;
        this.mapper = mapper;
        this.deviriumChatId = deviriumChatId;
        this.gardenerChatId = gardenerChatId;
    }

    @PostMapping("/git/webhook")
    public void process(@RequestBody GitWebhookRequest request) {
        if (request.content.contains("#draft")) {
            log.debug("Note {} would be ignored because of #draft tag", request.file);
            return;
        }
        try {
            String content = mapper.map(request.content, request.links);
            telegramService.sendMessage(deviriumChatId, null, content, "MarkdownV2");
        } catch (Exception e) {
            telegramService.sendMessage(gardenerChatId, null, format("Can't process %s: %s", request.file, e.getMessage()), null);
        }
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Object> handleException(Exception e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Map response = Map.of(false, "Internal server error");
        return new ResponseEntity<>(response, status);
    }

    public record GitWebhookRequest(String file, String content, Map<String, String> links) {
    }
}
