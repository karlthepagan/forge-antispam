package karl.codes.minecraft.antispam.config;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableList;
import karl.codes.java.PatternStrategyDeserializer;
import karl.codes.minecraft.antispam.config.model.SpamRuleNode;
import karl.codes.minecraft.antispam.rules.SpamRule;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by karl on 12/12/2015.
 */
public class RulesConfig {
    private static final Pattern JSON_PATTERN = Pattern.compile("^\\s*[\\[\\{]");

    private final ObjectMapper mapper;
    private final ObjectReader json;
    final ObjectWriter jsonOut;

    private final ObjectMapper yamlMapper;
    private final ObjectReader yaml;
    final ObjectWriter yamlOut;

    public RulesConfig() {
        mapper = new ObjectMapper();
        json = mapper.reader(PatternStrategyDeserializer.Strategy.JSON.attributes);
        jsonOut = mapper.writer(PatternStrategyDeserializer.Strategy.JSON.attributes);

        yamlMapper = new ObjectMapper(new YAMLFactory());
        yaml = yamlMapper.reader(PatternStrategyDeserializer.Strategy.GROOVY.attributes);
        yamlOut = yamlMapper.writer(PatternStrategyDeserializer.Strategy.GROOVY.attributes);
    }

    public ObjectReader getJson() {
        return json;
    }

    public ObjectReader getYaml() {
        return yaml;
    }

    public boolean isJson(URL resource) throws IOException {
        if(resource.getFile().endsWith(".yml"))
            return false;

        if(resource.getFile().endsWith(".json"))
            return true;

        // TODO remote resources will be read twice! (oops)
        Reader detectionStream = new InputStreamReader(resource.openStream());
        CharBuffer buffer = CharBuffer.allocate(512);
        detectionStream.read(buffer);
        detectionStream.close();
        buffer.flip();
        return JSON_PATTERN.matcher(buffer).find();
    }

    public ObjectReader createReader(URL resource) throws IOException {
        return isJson(resource) ? getJson() : getYaml();
    }

    public ImmutableList<SpamRule> read(URL resource) throws IOException {
        ObjectReader readerConfig = createReader(resource);

        return read(resource, readerConfig);
    }

    public ImmutableList<SpamRule> read(URL resource, ObjectReader readerConfig) throws IOException {
        JsonParser parser = readerConfig.getFactory().createParser(resource);

        List<SpamRuleNode> nodes = readNodes(parser, readerConfig);
//        write(nodes, ?.createGenerator(System.out) );

        return SpamRuleNode.build(nodes);
    }

    List<SpamRuleNode> readNodes(JsonParser parser, ObjectReader readerConfig) throws IOException {
        return readerConfig.readValue(parser, new TypeReference<List<SpamRuleNode>>() {});
    }

    void write(List<SpamRuleNode> nodes, JsonGenerator generator, ObjectWriter writerConfig) throws IOException {
        writerConfig.writeValue(generator, nodes);
    }

    // utility methods
    private static URL asURL(File file) {
        try {
            return file.getAbsoluteFile().toURI().toURL();
        } catch (MalformedURLException e) {
            throw new Error(e); // "Can't happen"
        }
    }

    public static List<URL> ruleLocations(File suggestedCfg, Class<?> context) {
        ArrayList<URL> candidates = new ArrayList<>();
        File suggestedYaml = new File(suggestedCfg.getParentFile(),suggestedCfg.getName().replace(".cfg",".yml"));
        File suggestedJson = new File(suggestedCfg.getParentFile(),suggestedCfg.getName().replace(".cfg",".json"));

        for (File file : new File[]{suggestedYaml, suggestedJson, suggestedCfg}) {
            if (file.exists() && file.canRead())
                candidates.add( asURL( file ) );
        }

        for (String res : new String[]{"antispam.yml","antispam.json","/antispam.yml","/antispam.json"}) {
            URL url = context.getResource(res);
            if(url != null)
                candidates.add( url );
        }

        return candidates;
    }
}
