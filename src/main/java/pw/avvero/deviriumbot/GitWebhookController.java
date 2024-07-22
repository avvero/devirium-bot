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
    private final OpenaiService openaiService;
    private final TelegramMessageMapper mapper;
    private final String deviriumChatId;
    private final String gardenerChatId;
    private final String correctorPrompt;

    public GitWebhookController(TelegramService telegramService,
                                OpenaiService openaiService,
                                TelegramMessageMapper mapper,
                                @Value("${devirium.chatId}") String deviriumChatId,
                                @Value("${devirium.gardenerChatId}") String gardenerChatId,
                                @Value("${corrector.prompt}") String correctorPrompt) {
        this.telegramService = telegramService;
        this.openaiService = openaiService;
        this.mapper = mapper;
        this.deviriumChatId = deviriumChatId;
        this.gardenerChatId = gardenerChatId;
        this.correctorPrompt = correctorPrompt;
    }

    @PostMapping("/git/webhook")
    public void process(@RequestBody GitWebhookRequest request) throws Exception {
        if ("index.md".equals(request.file)) {
            log.debug("Index note would be ignored");
            return;
        }
        if (request.content.contains("#draft") || (request.path != null && request.path.contains("draft"))) {
            log.debug("Note {} would be ignored because of #draft tag", request.file);
            return;
        }
        try {
            String content = mapper.map(request.file, request.content, request.links);
            // gpt-4o resist to follow instruction
            String correctorResult = openaiService.process("gpt-4", correctorPrompt + "\n" + request.content);
            if (!correctorResult.toLowerCase().contains("note is correct")) {
                telegramService.sendMessage(gardenerChatId, null, content, "MarkdownV2");
                telegramService.sendMessage(gardenerChatId, null, format("Can't process %s: Incorrect text, proposal:\n%s",
                        request.file, correctorResult), "markdown");
                return;
            }
            String targetChat = content.contains("#debug") ? gardenerChatId : deviriumChatId;
            telegramService.sendMessage(targetChat, null, content, "MarkdownV2");
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            telegramService.sendMessage(gardenerChatId, null, format("Can't process %s: %s", request.file,
                    e.getMessage()), "markdown");
        }
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Object> handleException(Exception e) {
        log.error(e.getLocalizedMessage(), e);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Map response = Map.of(false, "Internal server error");
        return new ResponseEntity<>(response, status);
    }

    public record GitWebhookRequest(String file, String path, String content, Map<String, String> links) {
    }
}
