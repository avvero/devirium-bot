package pw.avvero.deviriumbot;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class TelegramService {
    private final RestTemplate restTemplate;
    private final String url;
    private final String token;
    private final ObjectMapper objectMapper;

    public TelegramService(RestTemplate restTemplate,
                           @Value("${telegram.uri}") String url,
                           @Value("${telegram.token}") String token,
                           ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.url = url;
        this.token = token;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    public SendMessageResult sendMessage(String chatId, String text, String parseMode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        SendMessageRequest request = new SendMessageRequest(chatId, null, text, parseMode);
        HttpEntity<SendMessageRequest> requestEntity = new HttpEntity<>(request, headers);
        try {
            SendMessageResponse response = restTemplate.postForObject(url + "/" + token + "/sendMessage?disable_web_page_preview=true",
                    requestEntity, SendMessageResponse.class);
            return response.result;
        } catch (HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            SendMessageResponse response = objectMapper.readValue(e.getResponseBodyAsString(), SendMessageResponse.class);
            if (!response.ok) {
                throw new TelegramException(response.description);
            }
            throw e;
        }
    }

    public record SendMessageRequest(@JsonProperty("chat_id") String chatId,
                                     @JsonProperty("reply_to_message_id") String replyToMessageId,
                                     String text,
                                     @JsonProperty("parse_mode") String parseMode) {
    }

    public record SendMessageResponse(boolean ok, String description, SendMessageResult result) {
    }

    public record SendMessageResult(@JsonProperty("message_id") String messageId) {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TelegramException extends RuntimeException {

        public TelegramException(String message) {
            super(message);
        }
    }

    @SneakyThrows
    public SendPhotoResult sendPhoto(String chatId, String photo, String caption, String parseMode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new SendPhotoRequest(chatId, photo, caption, parseMode);
        HttpEntity<SendPhotoRequest> requestEntity = new HttpEntity<>(request, headers);
        try {
            SendPhotoResponse response = restTemplate.postForObject(url + "/" + token + "/sendPhoto",
                    requestEntity, SendPhotoResponse.class);
            return response.result;
        } catch (HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            SendMessageResponse response = objectMapper.readValue(e.getResponseBodyAsString(), SendMessageResponse.class);
            if (!response.ok) {
                throw new TelegramException(response.description);
            }
            throw e;
        }
    }

    public record SendPhotoRequest(@JsonProperty("chat_id") String chatId,
                                   String photo,
                                   String caption,
                                   @JsonProperty("parse_mode") String parseMode) {
    }

    public record SendPhotoResponse(boolean ok, String description, SendPhotoResult result) {
    }

    public record SendPhotoResult(@JsonProperty("message_id") String messageId) {
    }
}
