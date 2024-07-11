package pw.avvero.deviriumbot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

@Slf4j
@RestController
public class GitWebhookController {

    private final TelegramService telegramService;
    private final String deviriumChatId;
    private final String gardenerChatId;
    private final String deviriumLink;

    public GitWebhookController(TelegramService telegramService,
                                @Value("${devirium.chatId}") String deviriumChatId,
                                @Value("${devirium.gardenerChatId}") String gardenerChatId,
                                @Value("${devirium.link}") String deviriumLink) {
        this.telegramService = telegramService;
        this.deviriumChatId = deviriumChatId;
        this.gardenerChatId = gardenerChatId;
        this.deviriumLink = deviriumLink;
    }

    @PostMapping("/git/webhook")
    public void process(@RequestBody GitWebhookRequest request) {
        if (request.content.contains("#draft")) {
            log.debug("Note {} would be ignored because of #draft tag", request.file);
            return;
        }
        String content = request.content;
        Map<String, String> meta = new HashMap<>();
        //
        Pattern pattern = Pattern.compile("\\[\\[.*\\]\\]");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String id = UUID.randomUUID().toString().replace("-", "");
            meta.put(id, matcher.group());
            content = content.replace(matcher.group(), id);
        }
        // exclude: '>', '`'
        for (char ch : new char[]{'_', '*', '~', '#', '+', '-', '=', '|', '{', '}', '.', '!', '[', ']', '(', ')'}) {
            content = content.replace("" + ch, "\\" + ch);
        }
        //
        for (Map.Entry<String, String> entry : meta.entrySet()) {
            content = content.replace(entry.getKey(), entry.getValue());
        }
        // links
        if (request.links != null) {
            for (Map.Entry<String, String> link : request.links.entrySet()) {
                String url = format("[%s](%s/%s)", link.getKey(), deviriumLink, link.getValue());
                content = content.replace(format("[[%s]]", link.getKey()), url);
            }
        }
        // Check unresolved links
        Matcher matcherUnresolvedLink = Pattern.compile("\\[\\[.*?\\]\\]").matcher(content);
        if (matcherUnresolvedLink.find()) {
            telegramService.sendMessage(gardenerChatId, null, format("Can't process %s: Can't resolve link %s",
                    request.file, matcherUnresolvedLink.group()), null);
            return;
        }
        try {
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
