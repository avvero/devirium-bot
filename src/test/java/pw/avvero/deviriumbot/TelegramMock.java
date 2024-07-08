package pw.avvero.deviriumbot;

import org.springframework.test.web.client.ResponseCreator;
import pw.avvero.test.http.RequestCaptor;

public interface TelegramMock {
    RequestCaptor sendMessage(ResponseCreator responseCreator);
}
