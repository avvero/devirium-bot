package pw.avvero.deviriumbot;

import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;
import pw.avvero.test.http.RequestCaptor;

import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

public class RestExpectation {

    private final MockRestServiceServer mockServer;
    public final OpenaiMock openai;
    public final TelegramMock telegram;

    public RestExpectation(RestTemplate restTemplate) {
        mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        this.openai = new OpenaiMock() {
            @Override
            public RequestCaptor completions(ResponseCreator responseCreator) {
                return map(mockServer, "https://api.openai.test/v1/chat/completions", responseCreator);
            }
        };
        this.telegram = new TelegramMock() {
            @Override
            public RequestCaptor sendMessage(ResponseCreator responseCreator) {
                return map(mockServer, "https://api.telegram.test/bot123abc123/sendMessage?disable_web_page_preview=true", responseCreator);
            }
            @Override
            public RequestCaptor sendPhoto(ResponseCreator responseCreator) {
                return map(mockServer, "https://api.telegram.test/bot123abc123/sendPhoto", responseCreator);
            }
        };
    }

    protected RequestCaptor map(MockRestServiceServer mockServer, String uri, ResponseCreator responseCreator) {
        RequestCaptor requestCaptor = new RequestCaptor();
        mockServer.expect(manyTimes(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andExpect(requestCaptor)
                .andRespond(responseCreator);
        return requestCaptor;
    }

    public void cleanup() {
        mockServer.reset();
    }
}
