package com.wut.practicum.util;

import java.util.regex.Pattern;

/**
 * XSS 敏感脚本校验与过滤工具
 */
public class XssUtils {

    private static final Pattern[] SCRIPT_PATTERNS = new Pattern[]{
            Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("src[\r\n]*=[\r\n]*['\"]*(.*?)['\"]*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script(.*?>)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("onerror(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("onclick(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("onmouse(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
    };

    /**
     * 校验文本中是否包含可执行脚本
     */
    public static boolean containsScript(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        for (Pattern pattern : SCRIPT_PATTERNS) {
            if (pattern.matcher(content).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 过滤 HTML 标签转义
     */
    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * 对 ES 搜索高亮片段进行 XSS 安全净化，仅保留受控高亮标签 <em class="highlight"> ... </em>
     */
    public static String sanitizeHighlight(String highlightSnippet) {
        if (highlightSnippet == null) {
            return "";
        }
        // 先替换预定义高亮标记为临时占位符
        String placeholderStart = "___EM_HL_START___";
        String placeholderEnd = "___EM_HL_END___";

        String temp = highlightSnippet
                .replace("<em class=\"highlight\">", placeholderStart)
                .replace("<em>", placeholderStart)
                .replace("</em>", placeholderEnd);

        // 转义其他原生 HTML 字符防范 XSS
        String escaped = escapeHtml(temp);

        // 还原受控高亮标签
        return escaped
                .replace(placeholderStart, "<em class=\"highlight\">")
                .replace(placeholderEnd, "</em>");
    }
}
