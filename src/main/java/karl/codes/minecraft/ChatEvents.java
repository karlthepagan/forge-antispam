package karl.codes.minecraft;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import karl.codes.java.ListCharSequence;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by karl on 11/1/15.
 */
public class ChatEvents {
    private static Function<IChatComponent, CharSequence> TEXT_REDUCE_NOBAKE = new Function<IChatComponent, CharSequence>() {
        @Nullable
        @Override
        public CharSequence apply(@Nullable IChatComponent input) {
            return reduce(false, input);
        }
    };

    private static Function<IChatComponent, CharSequence> TEXT_REDUCE = new Function<IChatComponent, CharSequence>() {
        @Nullable
        @Override
        public CharSequence apply(IChatComponent input) {
            return reduce(true, input);
        }
    };

    private static CharSequence reduce(boolean bake, IChatComponent input) {
        List<IChatComponent> siblings = input.getSiblings();
        if(siblings.size() == 0) {
            return input.getUnformattedTextForChat();
        } else {
            return new ListCharSequence(bake, Lists.transform(siblings, TEXT_REDUCE_NOBAKE));
        }
    }

    public static CharSequence asCharSequence(ClientChatReceivedEvent event) {
        return new ListCharSequence(Lists.transform(event.message.getSiblings(), ChatEvents.TEXT_REDUCE));
    }
}
