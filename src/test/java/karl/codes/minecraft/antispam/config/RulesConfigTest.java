package karl.codes.minecraft.antispam.config;

import karl.codes.minecraft.antispam.AntiSpam;
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
        List<SpamRuleNode> rules = target.readNodes(
                target.getJson().getFactory().createParser(rulesResource),
                target.getJson());

        StringWriter wr = new StringWriter();
        target.write(rules,target.getYaml().getFactory().createGenerator(wr));
        String value = wr.toString();
        rules = target.readNodes(target.getYaml().getFactory().createParser(value),target.getYaml());
    }
}