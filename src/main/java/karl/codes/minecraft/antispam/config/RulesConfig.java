package karl.codes.minecraft.antispam.config;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableList;
import karl.codes.java.PatternStrategyDeserializer;
import karl.codes.minecraft.antispam.config.model.SpamRuleNode;
import karl.codes.minecraft.antispam.rules.SpamRule;

import java.io.IOException;
import java.util.List;

/**
 * Created by karl on 12/12/2015.
 */
public class RulesConfig {
    private final ObjectMapper mapper;
    private final ObjectReader json;
    private final ObjectReader yaml;

    public RulesConfig() {
        mapper = new ObjectMapper();
        json = mapper.reader(PatternStrategyDeserializer.Strategy.JSON.attributes);
        yaml = new ObjectMapper(new YAMLFactory()).reader(PatternStrategyDeserializer.Strategy.GROOVY.attributes);
    }

    public ObjectReader getJson() {
        return json;
    }

    public ObjectReader getYaml() {
        return yaml;
    }

    void write(List<SpamRuleNode> rules, JsonGenerator output) throws IOException {
        mapper.writeValue(output,rules);
    }

    public ImmutableList<SpamRule> read(JsonParser data) throws IOException {
        return SpamRuleNode.build( readNodes( data, json) );
    }

    public List<SpamRuleNode> readNodes(JsonParser data, ObjectReader reader) throws IOException {
        // TODO extract to reader/writer
        return reader.readValue(data, new TypeReference<List<SpamRuleNode>>() {});
    }

    public ImmutableList<SpamRule> readYaml(JsonParser data) throws IOException {
        return SpamRuleNode.build( readNodes( data, yaml) );
    }
}
