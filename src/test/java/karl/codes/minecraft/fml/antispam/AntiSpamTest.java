package karl.codes.minecraft.fml.antispam;

import karl.codes.minecraft.antispam.AntiSpamFixture;
import karl.codes.minecraft.srg.ChatEvents;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

public class AntiSpamTest {

    @Before
    public void setUp() throws Exception {
        target = new AntiSpam();
        // TODO need mock events?
        // TODO look at fml.common.Loader - could we have a mock context? e.g. https://docs.spring.io/spring/docs/current/spring-framework-reference/html/integration-testing.html
        target.preInit(new FMLPreInitializationEvent(null,null) {
            public Logger getModLog() {
                return LogManager.getLogger("antispam");
            }
            public File getSuggestedConfigurationFile() {
                return new File("config/antispam.cfg");
            }
        });
        target.setPassive(false);
    }

    @After
    public void tearDown() throws Exception {
        target = null;
    }

    AntiSpam target;

    @Test
    public void testApplyRules() throws Exception {
        ClientChatReceivedEvent event = AntiSpamFixture.chatEvent("a", "b");

        target.event(event);
        
        Assert.assertThat(event.isCanceled(), CoreMatchers.is(false));
    }

    public void runSpecification(LinkedHashMap<String,Boolean> spec) {

        List<ClientChatReceivedEvent> inputs = AntiSpamFixture.chatEvents(
                spec.keySet().iterator(), spec.size());

        Iterator<Boolean> outputIsCanceled = spec.values().iterator();

        for(ClientChatReceivedEvent input : inputs) {
            target.event(input);

            Assert.assertThat(ChatEvents.toString(input.message), input.isCanceled(), CoreMatchers.is(outputIsCanceled.next()));
        }

    }

    @Test
    public void testMcMMO() {
        LinkedHashMap<String,Boolean> events = new LinkedHashMap<>();

        events.put("[mcMMO] hello, i've got a version", false);
        events.put("[mcMMO] OMG LIKE ME HERE MY URL", true);

        runSpecification(events);
    }

    @Test
    public void testHbarSpam() {
        LinkedHashMap<String,Boolean> events = new LinkedHashMap<>();

        events.put("random control text, will be allowed", false);
        events.put("------------", true);
        events.put("Vote for us!", true);
        events.put("------------", true);

        runSpecification(events);
    }

    @Test
    public void testRegex() {
        String s = "derp";

        Pattern pat = Pattern.compile("^(?!" + Pattern.quote(s) + ").");

        Assert.assertThat(pat.matcher(s).find(),CoreMatchers.is(false));
        Assert.assertThat(pat.matcher("hello").find(),CoreMatchers.is(true));

    }

    @Test
    public void testFactionsMap() {
        LinkedHashMap<String,Boolean> events = new LinkedHashMap<>();

        events.put("_________.[ Factions! derp a derp ]._________", false);
        events.put("\\N/ - - - - - - - -", false);
        events.put("W+E - - - - - - - -", false);
        events.put("/S\\  - - - - - - - -", false);

        runSpecification(events);
    }
}