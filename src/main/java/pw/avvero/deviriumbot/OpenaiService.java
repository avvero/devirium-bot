package pw.avvero.deviriumbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class OpenaiService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String url;
    private final String token;
    public OpenaiService(RestTemplate restTemplate,
                         ObjectMapper objectMapper,
                         @Value("${openai.uri}") String url,
                         @Value("${openai.token}") String token) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.url = url;
        this.token = token;
    }

    @SneakyThrows
    public String process(String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        CompletionRequest request = new CompletionRequest("gpt-4o", List.of(new CompletionMessage("user", content)));
        HttpEntity<CompletionRequest> requestEntity = new HttpEntity<>(request, headers);
        try {
            CompletionResponse response = restTemplate.postForObject(url + "/v1/chat/completions", requestEntity,
                    CompletionResponse.class);
            return response.choices.getLast().message.content;
        } catch (HttpClientErrorException e) {
            CompletionResponse response = objectMapper.readValue(e.getResponseBodyAsString(), CompletionResponse.class);
            if (response != null && response.error != null) {
                throw new OpenaiException(response.error.code, response.error.message);
            }
            throw e;
        }
    }

    public record CompletionRequest(String model, List<CompletionMessage> messages) {}
    public record CompletionMessage(String role, String content){}
    public record CompletionResponse(List<CompletionChoice> choices, CompletionError error) {}
    public record CompletionChoice(CompletionChoiceMessage message){}
    public record CompletionChoiceMessage(String content){}
    public record CompletionError(String code, String message){}

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class OpenaiException extends RuntimeException {
        private String code;

        public OpenaiException(String code, String message) {
            super(message);
            this.code = code;
        }
    }
}
