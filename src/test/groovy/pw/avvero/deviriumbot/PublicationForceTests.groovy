package pw.avvero.deviriumbot

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.client.RestTemplate
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static pw.avvero.deviriumbot.CustomMockRestResponseCreators.*

@SpringBootTest
@ActiveProfiles(profiles = "test")
@ContextConfiguration
@AutoConfigureMockMvc
class PublicationForceTests extends Specification {

    @Autowired
    RestTemplate restTemplate
    @Autowired
    MockMvc mockMvc
    @Shared
    RestExpectation restExpectation

    def setup() {
        restExpectation = new RestExpectation(restTemplate)
    }

    def cleanup() {
        restExpectation.cleanup()
    }

    def "Note goes through openai corrector unsuccessfully and goes to admin channel"() {
        setup:
        def telegramRequestCaptor = restExpectation.telegram.sendMessage(chain(
                withSuccess('{"ok": true, "result": {"message_id": 100}}'),
                withSuccess('{"ok": true, "result": {"message_id": 101}}'),
                withSuccess('{"ok": true, "result": {"message_id": 102}}'),
        ))
        def openaiRequest = restExpectation.openai.completions(withSuccess('{"choices": [{"message": {"content": "Some note"}}]}'))
        when:
        mockMvc.perform(post("/git/webhook")
                .contentType(APPLICATION_JSON_VALUE)
                .content("""{
                  "file": "Note 1.md",
                  "content": "Note text\\n[[Note 2]]\\n[[Note-3]]\\n#teg1 #teg2",
                  "links": {
                    "Note 2": "2021/2021-11/Note-2",
                    "Note-3": "2021/2021-11/Note-3"
                  }
                }""".toString())
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
        then:
        openaiRequest.times == 1
        assertEquals("""{
          "model": "gpt-4",
          "messages": [{
            "role": "user", 
            "content": "Please check the note below, if nothing to fix just return 'Note is correct'. Note:\\nNote text\\n[[Note 2]]\\n[[Note-3]]\\n#teg1 #teg2"
          }]
        }""", openaiRequest.bodyString, false)
        openaiRequest.headers["Authorization"].last == "Bearer abc123"
        and:
        telegramRequestCaptor.times == 2
//        assertEquals("""{ TODO need request chain
//            "chat_id": "300000",
//            "text": "*Note 1*\\n\\nNote text\\n[Note 2](https://devirium.com/2021/2021-11/Note-2)\\n[Note\\\\-3](https://devirium.com/2021/2021-11/Note-3)\\n\\\\#teg1 \\\\#teg2",
//            "parse_mode" : "MarkdownV2"
//        }""", telegramRequestCaptor.bodyString, false)
        assertEquals("""{
            "text": "Can't process Note 1.md: Incorrect text, proposal:\\nSome note",
            "chat_id": "300000"
        }""", telegramRequestCaptor.bodyString, false)
        when: "Force publication request"
        mockMvc.perform(post("/telegram/webhook")
                .contentType(APPLICATION_JSON_VALUE)
                .content("""{
                    "message": {
                        "chat": {
                            "id": 300000
                        },
                        "reply_to_message": {
                            "message_id": 100,
                            "text": "Note"
                        },
                        "text": "publish"
                    }
                }""".toString())
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
        then:
        openaiRequest.times == 1
        telegramRequestCaptor.times == 3
        assertEquals("""{
            "chat_id": "200000",
            "text": "*Note 1*\\n\\nNote text\\n[Note 2](https://devirium.com/2021/2021-11/Note-2)\\n[Note\\\\-3](https://devirium.com/2021/2021-11/Note-3)\\n\\\\#teg1 \\\\#teg2",
            "parse_mode" : "MarkdownV2"
        }""", telegramRequestCaptor.bodyString, false)
    }
}