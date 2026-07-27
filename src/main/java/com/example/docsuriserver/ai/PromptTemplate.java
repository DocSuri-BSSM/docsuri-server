package com.example.docsuriserver.ai;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * resources/prompts/*.md 를 로드하고 {{VARIABLE_NAME}} 자리표시자를 치환한다.
 * 치환되지 않은 자리표시자가 남아 있으면 누락을 조기에 발견하기 위해 예외를 던진다.
 */
@Component
public class PromptTemplate {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{[A-Z0-9_]+}}");

    public String render(String promptResourceName, Map<String, String> variables) {
        String rendered = load(promptResourceName);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }

        Matcher matcher = PLACEHOLDER.matcher(rendered);
        if (matcher.find()) {
            throw new IllegalStateException(
                    "프롬프트 '" + promptResourceName + "'에 치환되지 않은 자리표시자가 남아 있습니다: " + matcher.group());
        }
        return rendered;
    }

    private String load(String promptResourceName) {
        String path = "/prompts/" + promptResourceName;
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("프롬프트 파일을 찾을 수 없습니다: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("프롬프트 파일 로드 실패: " + path, e);
        }
    }
}
