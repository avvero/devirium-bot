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

@SpringBootTest
@ActiveProfiles(profiles = "test")
@ContextConfiguration
@AutoConfigureMockMvc
class ApplicationTests extends Specification {

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

    def "User Message Processing"() {
        setup:
        def telegramRequestCaptor = restExpectation.telegram.sendMessage(CustomMockRestResponseCreators.withSuccess("{}"))
        when:
        mockMvc.perform(post("/git/webhook")
                .contentType(APPLICATION_JSON_VALUE)
                .content("""{
                  "file": "Заметка 1.md",
                  "content": "# Заметка 1\\n\\nТекст заметки\\n#teg1 #teg2"
                }""".toString())
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
        then:
        telegramRequestCaptor.times == 1
        assertEquals("""{
            "chat_id": "200000",
            "text": "\\\\# Заметка 1\\n\\nТекст заметки\\n\\\\#teg1 \\\\#teg2",
            "parse_mode" : "MarkdownV2"
        }""", telegramRequestCaptor.bodyString, false)
    }

    def "Ignore drafts"() {
        setup:
        def telegramRequestCaptor = restExpectation.telegram.sendMessage(CustomMockRestResponseCreators.withSuccess("{}"))
        when:
        mockMvc.perform(post("/git/webhook")
                .contentType(APPLICATION_JSON_VALUE)
                .content("""{
                  "file": "Заметка 1.md",
                  "content": "# Заметка 1\\n\\nТекст заметки\\n#teg1 #teg2 #draft"
                }""".toString())
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
        then:
        telegramRequestCaptor.times == 0
    }

    def "User Message Processing with links"() {
        setup:
        def telegramRequestCaptor = restExpectation.telegram.sendMessage(CustomMockRestResponseCreators.withSuccess("{}"))
        when:
        mockMvc.perform(post("/git/webhook")
                .contentType(APPLICATION_JSON_VALUE)
                .content("""{
                  "file": "Заметка 1.md",
                  "content": "# Заметка 1\\n\\nТекст заметки\\n[[Заметка 2]]\\n#teg1 #teg2",
                  "links": {
                    "Заметка 2": "2021/2021-11/Заметка-2"
                  }
                }""".toString())
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
        then:
        telegramRequestCaptor.times == 1
        assertEquals("""{
            "chat_id": "200000",
            "text": "\\\\# Заметка 1\\n\\nТекст заметки\\n[Заметка 2](https://devirium\\\\.com/2021/2021\\\\-11/Заметка\\\\-2)\\n\\\\#teg1 \\\\#teg2",
            "parse_mode" : "MarkdownV2"
        }""", telegramRequestCaptor.bodyString, false)
    }

    @Unroll
    def "User Message Processing with escaped character"() {
        setup:
        def telegramRequestCaptor = restExpectation.telegram.sendMessage(CustomMockRestResponseCreators.withSuccess("{}"))
        when:
        mockMvc.perform(post("/git/webhook")
                .contentType(APPLICATION_JSON_VALUE)
                .content("""{
                  "file": "Заметка 1.md",
                  "content": "$content"
                }""".toString())
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
        then:
        telegramRequestCaptor.times == 1
        telegramRequestCaptor.body.text == expected
        where:
        content                                                     | expected
        "Заметка"                                                   | "Заметка"
        "https://devirium/2021/2021-11/trick-of-abusive-assurances" | "https://devirium/2021/2021\\-11/trick\\-of\\-abusive\\-assurances"
        "Заметка _"                                                 | "Заметка \\_"
        ">Цитата"                                                   | ">Цитата"
    }
}