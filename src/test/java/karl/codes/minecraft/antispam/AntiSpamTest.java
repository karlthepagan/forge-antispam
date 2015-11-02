package karl.codes.minecraft.antispam;

import junit.framework.TestCase;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public class AntiSpamTest extends TestCase {

    @Before
    public void setUp() throws Exception {
        target = new AntiSpam();
    }

    @After
    public void tearDown() throws Exception {
        target = null;
    }

    AntiSpam target;

    public void testApplyRules() throws Exception {
        ClientChatReceivedEvent event = AntiSpamFixture.chatEvent("a", "b");

        target.event(event);
        
        Assert.assertThat(event.isCanceled(), CoreMatchers.is(false));
    }
}