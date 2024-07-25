package pw.avvero.deviriumbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

@Component
public class TelegramMessageMapper {

    public static final Pattern ZET_LINK = Pattern.compile("\\[\\[.*?\\]\\]");
    public static final Pattern MD_LINK = Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");
    public static final Pattern IMAGE_LINK = Pattern.compile("!\\[.*?\\]\\(.*?\\)\\s*");

    private final String deviriumLink;

    public TelegramMessageMapper(@Value("${devirium.link}") String deviriumLink) {
        this.deviriumLink = deviriumLink;
    }

    public String map(String fileName, String content, Map<String, String> links) throws Exception {
        Map<String, String> meta = new HashMap<>();
        // Remove links to images
        Matcher matcher = IMAGE_LINK.matcher(content);
        content = matcher.replaceAll("");
        // Extract meta
        content = extractMeta(content, meta, ZET_LINK, MD_LINK);
        content = escape(content);
        // Enrich meta
        for (Map.Entry<String, String> entry : meta.entrySet()) {
            content = content.replace(entry.getKey(), entry.getValue());
        }
        // Update links with real ones
        if (links != null) {
            for (Map.Entry<String, String> link : links.entrySet()) {
                String url = format("[%s](%s/%s)", escape(link.getKey()), deviriumLink, link.getValue());
                content = content.replace(format("[[%s]]", link.getKey()), url);
            }
        }
        // Check unresolved links
        Matcher matcherUnresolvedLink = ZET_LINK.matcher(content);
        if (matcherUnresolvedLink.find()) {
            throw new Exception(format("Can't resolve link %s", matcherUnresolvedLink.group()));
        }
        if (fileName != null) {
            return format("*%s*\n\n%s", escape(fileName.replace(".md", "")), content);
        } else {
            return content;
        }
    }

    private String escape(String value) {
        // Escape characters except: '>', '`'
        for (char ch : new char[]{'_', '*', '~', '#', '+', '-', '=', '|', '{', '}', '.', '!', '[', ']', '(', ')'}) {
            value = value.replace("" + ch, "\\" + ch);
        }
        return value;
    }

    private String extractMeta(String content, Map<String, String> meta, Pattern... patterns) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String id = UUID.randomUUID().toString().replace("-", "");
                meta.put(id, matcher.group());
                content = content.replace(matcher.group(), id);
            }
        }
        return content;
    }

    public String getUrlForPhoto(String link) {
        return format("%s/%s", deviriumLink, link);
    }
}
