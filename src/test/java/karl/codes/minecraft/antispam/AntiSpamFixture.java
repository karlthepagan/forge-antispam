package karl.codes.minecraft.antispam;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

/**
 * Created by karl on 11/1/15.
 */
public class AntiSpamFixture {
    public static ClientChatReceivedEvent chatEvent(String ... segments) {
        IChatComponent message = new ChatComponentText("");
        for(String s : segments) {
            message.appendText(s);
        }
        return new ClientChatReceivedEvent((byte)0,message);
    }
}
