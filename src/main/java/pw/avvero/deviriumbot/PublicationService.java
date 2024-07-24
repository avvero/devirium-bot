package pw.avvero.deviriumbot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

@Slf4j
@Component
public class PublicationService {

    private final TelegramService telegramService;
    private final OpenaiService openaiService;
    private final TelegramMessageMapper mapper;
    private final String deviriumChatId;
    private final String gardenerChatId;
    private final String correctorPrompt;
    private final Map<String, String> notesOnReview = new ConcurrentHashMap<>();

    public PublicationService(TelegramService telegramService,
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

    public void publishNote(String name, String path, String content, Map<String, String> links) {
        if ("index.md".equals(name)) {
            log.debug("Index note would be ignored");
            return;
        }
        if (content.contains("#draft") || (path != null && path.contains("draft"))) {
            log.debug("Note {} would be ignored because of it's a draft", name);
            return;
        }
        try {
            String telegramMessageBody = mapper.map(name, content, links);
            // gpt-4o resist to follow instruction
            String correctorResult = openaiService.process("gpt-4", correctorPrompt + "\n" + content);
            if (!correctorResult.toLowerCase().contains("note is correct")) {
                var messageToReview = telegramService.sendMessage(gardenerChatId, telegramMessageBody, "MarkdownV2");
                notesOnReview.put(messageToReview.messageId(), telegramMessageBody);
                telegramService.sendMessage(gardenerChatId, format("Can't process %s: Incorrect text, proposal:\n%s",
                        name, correctorResult), "markdown");
                return;
            }
            String targetChat = telegramMessageBody.contains("#debug") ? gardenerChatId : deviriumChatId;
            telegramService.sendMessage(targetChat, telegramMessageBody, "MarkdownV2");
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            telegramService.sendMessage(gardenerChatId, format("Can't process %s: %s", name, e.getMessage()), "markdown");
        }
    }

    public void publishAfterReview(String messageId) {
        String telegramMessageBody = notesOnReview.get(messageId);
        if (telegramMessageBody == null) {
            telegramService.sendMessage(gardenerChatId, format("Can't find message to publish after review: %s", messageId), "markdown");
            return;
        }
        try {
            telegramService.sendMessage(deviriumChatId, telegramMessageBody, "MarkdownV2");
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            telegramService.sendMessage(gardenerChatId, format("Can't process note after review: %s", e.getMessage()), "markdown");
        }
    }
}
