package pw.avvero.deviriumbot;

import org.springframework.test.web.client.ResponseCreator;
import pw.avvero.test.http.RequestCaptor;

public interface OpenaiMock {

    /**
     * <p>This method configures the mock request to the following URL: {@code https://api.openai.com/v1/chat/completions}
     */
    RequestCaptor completions(ResponseCreator responseCreator);
}
