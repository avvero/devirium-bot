package pw.avvero.deviriumbot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class GitWebhookController {

    private final PublicationService publicationService;

    @PostMapping("/git/webhook")
    public void process(@RequestBody GitWebhookRequest request) throws Exception {
        publicationService.publishNote(request.file, request.path, request.content, request.links, request.images);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Object> handleException(Exception e) {
        log.error(e.getLocalizedMessage(), e);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Map response = Map.of(false, "Internal server error");
        return new ResponseEntity<>(response, status);
    }

    public record GitWebhookRequest(String file,
                                    String path,
                                    String content,
                                    Map<String, String> links,
                                    Map<String, String> images) {
    }
}
