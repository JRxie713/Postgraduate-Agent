package com.demo.scaffoldbackend.service;

import com.demo.scaffoldbackend.vo.QuestionOptionVO;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 练习题选项解析与客观题判题工具
 *
 * <p>兼容三类 option_info 存储格式：JSON 数组、换行分隔、竖线分隔。</p>
 */
@Component
public class PracticeQuestionSupport {

    private final ObjectMapper objectMapper;

    private static final Pattern OPTION_PREFIX =
            Pattern.compile("^\\s*[(（]?([A-Za-z])[)）.、:：]\\s*(.+)$", Pattern.DOTALL);

    public PracticeQuestionSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 解析选项信息为统一结构 */
    public List<QuestionOptionVO> parseOptions(String optionInfo) {
        List<QuestionOptionVO> options = new ArrayList<>();
        if (optionInfo == null || optionInfo.isBlank()) {
            return options;
        }
        String raw = optionInfo.trim();
        if (raw.startsWith("[") || raw.startsWith("{")) {
            try {
                JsonNode root = objectMapper.readTree(raw);
                JsonNode array = root.isArray() ? root : root.get("options");
                if (array != null && array.isArray()) {
                    int index = 0;
                    for (JsonNode node : array) {
                        index++;
                        if (node.isString()) {
                            options.add(splitPrefixed(node.asText(), String.valueOf((char) ('A' + index - 1))));
                        } else {
                            String key = firstText(node, "key", "label", "value", "option");
                            String text = firstText(node, "text", "content", "title", "value");
                            if (key == null || key.isBlank()) {
                                key = String.valueOf((char) ('A' + index - 1));
                            }
                            if (text != null && !text.isBlank()) {
                                QuestionOptionVO vo = new QuestionOptionVO();
                                vo.setKey(key.trim().toUpperCase(Locale.ROOT));
                                vo.setText(text.trim());
                                options.add(vo);
                            }
                        }
                    }
                    if (!options.isEmpty()) {
                        return options;
                    }
                }
            } catch (Exception ignore) {
                // 非法 JSON 时回退到文本分隔解析
            }
        }
        String[] parts = raw.split("\\r?\\n|\\||;|；");
        int index = 0;
        for (String part : parts) {
            String text = part.trim();
            if (text.isEmpty()) {
                continue;
            }
            options.add(splitPrefixed(text, String.valueOf((char) ('A' + index))));
            index++;
        }
        return options;
    }

    /** 判断客观题作答是否正确 */
    public boolean judge(String userAnswer, String refAnswer, List<QuestionOptionVO> options) {
        String user = normalize(userAnswer);
        String ref = normalize(refAnswer);
        if (user.isEmpty() || ref.isEmpty()) {
            return false;
        }
        if (user.equals(ref)) {
            return true;
        }
        String userKeys = letterKeys(user);
        String refKeys = letterKeys(ref);
        if (!refKeys.isEmpty() && userKeys.equals(refKeys)) {
            return true;
        }
        // 允许用户直接提交选项文本
        if (!userKeys.isEmpty() && options != null) {
            for (QuestionOptionVO option : options) {
                if (normalize(option.getText()).equals(user)) {
                    return refKeys.contains(normalize(option.getKey()));
                }
            }
        }
        return false;
    }

    private QuestionOptionVO splitPrefixed(String text, String defaultKey) {
        QuestionOptionVO vo = new QuestionOptionVO();
        Matcher matcher = OPTION_PREFIX.matcher(text);
        if (matcher.find()) {
            vo.setKey(matcher.group(1).toUpperCase(Locale.ROOT));
            vo.setText(matcher.group(2).trim());
        } else {
            vo.setKey(defaultKey);
            vo.setText(text);
        }
        return vo;
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && value.isValueNode()) {
                return value.asText();
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    /** 仅把 A-D 这类纯选项字母视为选项键，避免把英文单词误判为键 */
    private String letterKeys(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (!value.matches("[A-H]+")) {
            return "";
        }
        char[] chars = value.toCharArray();
        java.util.Arrays.sort(chars);
        return new String(chars);
    }
}
