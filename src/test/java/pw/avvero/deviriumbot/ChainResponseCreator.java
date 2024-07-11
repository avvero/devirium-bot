package pw.avvero.deviriumbot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.ResponseCreator;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

@Slf4j
public class ChainResponseCreator implements ResponseCreator {

    private final Queue<ResponseCreator> queue;

    public ChainResponseCreator(Collection<ResponseCreator> collection) {
        this.queue = new ArrayDeque<>(collection);
    }

    @Override
    public synchronized ClientHttpResponse createResponse(ClientHttpRequest request) throws IOException {
        ResponseCreator responseCreator = queue.poll();
        if (queue.isEmpty()) {
            queue.add(responseCreator);
        }
        log.trace("Response creator for {} is {}", request.getURI(), responseCreator);
        ClientHttpResponse result = responseCreator.createResponse(request);
        log.trace("Response for {} is {}", request.getURI(), result);
        return result;
    }
}

