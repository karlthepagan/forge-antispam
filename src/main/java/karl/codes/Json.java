package karl.codes;

import java.util.regex.Pattern;

/**
 * JSON utilities
 */
public class Json {
    /**
     * A little hack used to treat $ as the escape character in regex's
     * @param jsonPattern a regex pattern which uses $ as escapes
     * @return a compiled pattern
     */
    public static Pattern compileJson(String jsonPattern) {
        try {
            jsonPattern = jsonPattern.replaceAll("\\$(?=.)","\\\\"); // replace all $'s not at the end with \'s
            jsonPattern = jsonPattern.replaceAll("\\\\\\\\","\\\\\\$"); // replace all \\'s with \$'s (literal dollar)
            return Pattern.compile(jsonPattern);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(jsonPattern,e);
        }
    }
}
