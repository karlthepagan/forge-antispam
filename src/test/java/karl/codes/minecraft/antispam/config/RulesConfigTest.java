package karl.codes.minecraft.antispam.config;

import com.fasterxml.jackson.core.JsonParser;
import karl.codes.minecraft.fml.antispam.AntiSpam;
import karl.codes.minecraft.antispam.config.model.SpamRuleNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.net.URL;
import java.util.List;

/**
 * Created by karl on 12/12/2015.
 */
public class RulesConfigTest {
    RulesConfig target;

    @Before
    public void setUp() throws Exception {
        target = new RulesConfig();
    }

    @After
    public void tearDown() throws Exception {
        target = null;
    }

    @Test
    public void testParseJsonToYaml() throws Exception {
        URL rulesResource = AntiSpam.class.getResource("/antispam.json");
        JsonParser parser = target.getJson().getFactory().createParser(rulesResource);

        List<SpamRuleNode> rules = target.readNodes(parser, target.getJson());

        StringWriter wr = new StringWriter();
        target.write(rules,target.getYaml().getFactory().createGenerator(wr),target.yamlOut);
        String value = wr.toString();
        System.out.println(value);
        rules = target.readNodes(target.getYaml().getFactory().createParser(value),target.getYaml());
    }

    @Test
    public void testParseYamlToJson() throws Exception {
        URL rulesResource = AntiSpam.class.getResource("/antispam.yml");
        JsonParser parser = target.getYaml().getFactory().createParser(rulesResource);

        List<SpamRuleNode> rules = target.readNodes(parser, target.getYaml());

        StringWriter wr = new StringWriter();
        target.write(rules,target.getJson().getFactory().createGenerator(wr),target.jsonOut);
        String value = wr.toString();
        System.out.println(value);
        rules = target.readNodes(target.getJson().getFactory().createParser(value),target.getJson());
    }
}