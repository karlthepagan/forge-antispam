package karl.codes.minecraft.antispam;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by karl on 11/1/15.
 */
public class AntiSpamFixture {
    public static ClientChatReceivedEvent chatEvent(String ... segments) {
        IChatComponent message = new ChatComponentText("");
        for(String s : segments) {
            message.appendText(s);
        }
        return new Chat(message);
    }

    public static List<ClientChatReceivedEvent> chatEvents(Iterator<String> text, int size) {
        ArrayList<ClientChatReceivedEvent> output = new ArrayList<>(size);

        while(text.hasNext()) {
            output.add(new Chat(new ChatComponentText(text.next())));
        }

        return output;
    }

    public static class Chat extends ClientChatReceivedEvent {
        public Chat(IChatComponent message) {
            // TODO mc 1.7.10 vs 1.8.8
            super(message);
        }

        public boolean isCancelable() {
            return true;
        }
    }
}
