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

    def "Note goes to telegram channel"() {
        setup:
        def telegramRequestCaptor = restExpectation.telegram.sendMessage(withSuccess("{}"))
        when:
        mockMvc.perform(post("/git/webhook")
                .contentType(APPLICATION_JSON_VALUE)
                .content("""{
                  "file": "Note 1.md",
                  "content": "Note text\\n#teg1 #teg2"
                }""".toString())
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
        then:
        telegramRequestCaptor.times == 1
        assertEquals("""{
            "chat_id": "200000",
            "text": "*Note 1*\\n\\nNote text\\n\\\\#teg1 \\\\#teg2",
            "parse_mode" : "MarkdownV2"
        }""", telegramRequestCaptor.bodyString, false)
    }

    def "Draft note is skipped"() {
        setup:
        def telegramRequestCaptor = restExpectation.telegram.sendMessage(withSuccess("{}"))
        when:
        mockMvc.perform(post("/git/webhook")
                .contentType(APPLICATION_JSON_VALUE)
                .content("""{
                  "file": "Note 1.md",
                  "content": "Note text\\n#teg1 #teg2 #draft"
                }""".toString())
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
        then:
        telegramRequestCaptor.times == 0
    }

    def "Note with resolved links goes to telegram channel"() {
        setup:
        def telegramRequestCaptor = restExpectation.telegram.sendMessage(withSuccess("{}"))
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
        telegramRequestCaptor.times == 1
        assertEquals("""{
            "chat_id": "200000",
            "text": "*Note 1*\\n\\nNote text\\n[Note 2](https://devirium.com/2021/2021-11/Note-2)\\n[Note\\\\-3](https://devirium.com/2021/2021-11/Note-3)\\n\\\\#teg1 \\\\#teg2",
            "parse_mode" : "MarkdownV2"
        }""", telegramRequestCaptor.bodyString, false)
    }

    def "Note with unresolved links wouldn't be sent to telegram channel"() {
        setup:
        def telegramRequestCaptor = restExpectation.telegram.sendMessage(withSuccess("{}"))
        when:
        mockMvc.perform(post("/git/webhook")
                .contentType(APPLICATION_JSON_VALUE)
                .content("""{
                  "file": "Note 1.md",
                  "content": "Note text\\n[[Note 2]]\\n#teg1 #teg2",
                  "links": {
                    "unexpected": "2021/2021-11/Note-2"
                  }
                }""".toString())
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
        then:
        telegramRequestCaptor.times == 1
        assertEquals("""{
            "text": "Can't process Note 1.md: Can't resolve link [[Note 2]]",
            "chat_id": "300000"
        }""", telegramRequestCaptor.bodyString, false)
    }

    @Unroll
    def "Note with special symbols goes to telegram channel"() {
        setup:
        def telegramRequestCaptor = restExpectation.telegram.sendMessage(withSuccess("{}"))
        when:
        mockMvc.perform(post("/git/webhook")
                .contentType(APPLICATION_JSON_VALUE)
                .content("""{
                  "file": null,
                  "content": "$content"
                }""".toString())
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
        then:
        telegramRequestCaptor.times == 1
        telegramRequestCaptor.body.text == expected
        where:
        content                                                     | expected
        "Note"                                                      | "Note"
        "https://devirium/2021/2021-11/trick-of-abusive-assurances" | "https://devirium/2021/2021\\-11/trick\\-of\\-abusive\\-assurances"
        "Note _"                                                    | "Note \\_"
        ">Text"                                                     | ">Text"
        "[inline URL](http://www.example.com/)"                     | "[inline URL](http://www.example.com/)"
        "One (Two) Three"                                           | "One \\(Two\\) Three"
        "One [Two] Three"                                           | "One \\[Two\\] Three"
        "sdf`d"                                                     | "sdf\\`d"
        "sdf`d`"                                                    | "sdf`d`"
        "sdf```d```"                                                | "sdf```d```"
    }

    def "Message goes to admin if exception is occurred with send message method"() {
        setup:
        def telegramRequestCaptor = restExpectation.telegram.sendMessage(
                chain(
                        withBadRequest("""{
                            "ok": false,
                            "error_code": 400,
                            "description": "Bad Request: can't parse entities: Can't find end of Code entity at byte offset 3"
                        }"""),
                        withSuccess('{}')
                ))
        when:
        mockMvc.perform(post("/git/webhook")
                .contentType(APPLICATION_JSON_VALUE)
                .content("""{
                  "file": "Note 1.md",
                  "content": "Note"
                }""".toString())
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
        then:
        telegramRequestCaptor.times == 2
        assertEquals("""{
            "text": "Can't process Note 1.md: Bad Request: can't parse entities: Can't find end of Code entity at byte offset 3",
            "chat_id": "300000"
        }""", telegramRequestCaptor.bodyString, false)
    }
}