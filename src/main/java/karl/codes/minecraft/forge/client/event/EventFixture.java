package karl.codes.minecraft.forge.client.event;

import com.google.common.base.Function;
import cpw.mods.fml.common.eventhandler.Event;
import karl.codes.minecraft.srg.ChatEvents;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Created by karl on 12/18/2015.
 */
public class EventFixture {
    public static final Function<ClientChatReceivedEvent, CharSequence> CLIENT_CHAT_AS_CHARSEQUENCE = new Function<ClientChatReceivedEvent, CharSequence>() {
        @Nullable
        @Override
        public CharSequence apply(ClientChatReceivedEvent input) {
            // TODO this is copy-avoidance in the extreme, it is possibly slower because of many small strings, even with the reduce operation
            return ChatEvents.asCharSequence(input.message);
        }
    };

    public static final Consumer<Event> CANCEL_EVENT = new Consumer<Event>() {
        @Override
        public void accept(Event event) {
            event.setCanceled(true);
        }
    };

    public static Consumer<Pair<String, ClientChatReceivedEvent>> CLIENT_CHAT_PREPEND_MUTATOR = new Consumer<Pair<String, ClientChatReceivedEvent>>() {
        @Override
        public void accept(Pair<String, ClientChatReceivedEvent> messageEventPair) {
            ClientChatReceivedEvent event = messageEventPair.getRight();
            IChatComponent old = event.message;
            event.message = new ChatComponentText(messageEventPair.getLeft());
            event.message.appendSibling(old);
        }
    };

    public static Object messageKey(ClientChatReceivedEvent event, String last) {
        return ChatEvents.textKey(event.message, last);
    }
}
