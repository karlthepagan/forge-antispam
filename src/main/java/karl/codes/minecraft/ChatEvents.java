package karl.codes.minecraft;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import karl.codes.java.ListCharSequence;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import scala.Char;

import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by karl on 11/1/15.
 */
public class ChatEvents {
    public static final HashFunction STRING_HASH = Hashing.goodFastHash(32);

    private static Function<IChatComponent, CharSequence> TEXT_REDUCE_NOBAKE = new Function<IChatComponent, CharSequence>() {
        @Nullable
        @Override
        public CharSequence apply(@Nullable IChatComponent input) {
            return reduce(false, input);
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

    public static CharSequence asCharSequence(IChatComponent input) {
        return reduce(true, input);
    }

    public static int hashCode(IChatComponent input) {
        Charset charset = Charsets.UTF_8;
        Hasher h = STRING_HASH.newHasher();
        // depth-first iteration, using stack
        hash(input, charset, h);
        return h.hash().asInt();
    }

    public static void hash(IChatComponent input, Charset charset, Hasher h) {
        List<IChatComponent> siblings = input.getSiblings();
        if(siblings.size() == 0) {
            // TODO this will stringify sometimes?
            h.putString(input.getUnformattedText(),charset);
        } else {
            for(int i = siblings.size() - 1; i >= 0; i--) {
                hash(siblings.get(i),charset,h);
            }
        }
    }

    public static class TextKey {
        // TODO weak reference
        public IChatComponent chat;
        private int hash = Integer.MAX_VALUE;

        @Override
        public String toString() {
            if(chat == null) return ""; // TODO cache toString for debugging?

            // copy once, usually... TODO stream into StringBuilder
            return reduce(false, chat).toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TextKey textKey = (TextKey) o;

            // not worth checking contents
            return hashCode() == textKey.hashCode();
        }

        @Override
        public int hashCode() {
            if(hash == Integer.MAX_VALUE) {
                hash = ChatEvents.hashCode(chat);
                chat = null;
            }

            return hash;
        }
    }

    public static Object textKey(IChatComponent input) {
        TextKey key = new TextKey();
        key.chat = input;
        return key;
    }
}
