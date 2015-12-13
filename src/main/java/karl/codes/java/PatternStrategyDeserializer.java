package karl.codes.java;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import karl.codes.Json;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Created by karl on 12/12/2015.
 */
public class PatternStrategyDeserializer extends JsonDeserializer<Pattern> {
    public enum Strategy {
        JSON,
        GROOVY,
        RAW;

        public final ContextAttributes attributes = buildAttributes();

        private ContextAttributes buildAttributes() {
            return ContextAttributes.getEmpty().withSharedAttribute(
                    PatternStrategyDeserializer.class.getSimpleName(),
                    this);
        }
    }

    @Override
    public Pattern deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if(value == null) return null;

        return compile(value,ctxt.getConfig().getAttributes());
    }

    private Pattern compile(String value, ContextAttributes attributes) {
        switch((Strategy)attributes.getAttribute(PatternStrategyDeserializer.class.getSimpleName())) {
            case JSON:
                return Json.compileJson(value);

            default:
            case GROOVY:
                if('/' == value.charAt(0) && '/' == value.charAt(value.length()-1))
                    return Pattern.compile(value.substring(1,value.length()-1));

                // fall through to RAW
            case RAW:
                return Pattern.compile(value);
        }
    }
}
