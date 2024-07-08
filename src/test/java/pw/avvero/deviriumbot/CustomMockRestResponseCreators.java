package pw.avvero.deviriumbot;

import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringSubstitutor;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.intellij.lang.annotations.Language;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.ResourceAccessException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@RequiredArgsConstructor
public class CustomMockRestResponseCreators extends MockRestResponseCreators {

    public static ResponseCreator withSuccess(@Language("JSON") String body) {
        return MockRestResponseCreators.withSuccess(body, APPLICATION_JSON);
    }

    public static ResponseCreator withResourceAccessException() {
        return (request) -> {
            throw new ResourceAccessException("Error");
        };
    }

    public static String fromFile(String testResourceFile) throws IOException {
        return fromFile(testResourceFile, Map.of());
    }

    public static String fromFile(String file, Map<String, Object> values) throws IOException {
        String text = IOGroovyMethods.getText(ResourceGroovyMethods.newReader(new File("src/test/resources/" + file)));
        return new StringSubstitutor(values).replace(text);
    }
}
