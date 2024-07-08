package pw.avvero.deviriumbot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TelegramWebhookController {

    @PostMapping("/telegram/webhook")
    public void process(@RequestBody TelegramWebhookMessage request) {

    }

    public record TelegramWebhookMessage(Message message) {
    }

    public record Message(@JsonProperty("message_id") String id,
                          Chat chat,
                          String text) {
    }

    public record Chat(String id) {
    }

}
