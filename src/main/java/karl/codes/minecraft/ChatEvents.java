package karl.codes.minecraft;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import karl.codes.java.ListCharSequence;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

/**
 * Minecraft chat text processing
 *
 * TODO reobf support
 */
public class ChatEvents {
    public static final HashFunction STRING_HASH = Hashing.goodFastHash(32);

    public static final Function<Object, CharSequence> CLIENT_CHAT_AS_CHARSEQUENCE = new Function<Object, CharSequence>() {
        @Override
        public CharSequence apply(Object input) {
            // TODO this is copy-avoidance in the extreme, it is possibly slower because of many small strings, even with the reduce operation
//            return ChatEvents.asCharSequence(input.message);
            return null;
        }
    };

    private static Function<Object, CharSequence> TEXT_REDUCE_NOBAKE = new Function<Object, CharSequence>() {
        @Override
        public CharSequence apply(Object input) {
            return reduce(false, input);
        }
    };

    private static CharSequence reduce(boolean bake, Object input) {
        return null;
//        List<IChatComponent> siblings = input.getSiblings();
//        if(siblings.size() == 0) {
//            return input.getUnformattedTextForChat();
//        } else {
//            return new ListCharSequence(bake, Lists.transform(siblings, TEXT_REDUCE_NOBAKE));
//        }
    }

    public static CharSequence asCharSequence(Object input) {
        return reduce(true, input);
    }

    public static String toString(Object input) {
        return reduce(false, input).toString();
    }

    public static int hashCode(Object input) {
        return hashCode(input,0);
    }

    private static int hashCode(Object input, int seed) {
        Charset charset = Charsets.UTF_8;
        Hasher h = STRING_HASH.newHasher();
        h.putInt(seed);
        // depth-first iteration, using stack
        hash(input, charset, h);
        return h.hash().asInt();
    }

    public static void hash(Object input, Charset charset, Hasher h) {
//        List<IChatComponent> siblings = input.getSiblings();
//        if(siblings.size() == 0) {
//            // TODO this will stringify sometimes?
//            h.putString(input.getUnformattedText(),charset);
//        } else {
//            for(int i = siblings.size() - 1; i >= 0; i--) {
//                hash(siblings.get(i),charset,h);
//            }
//        }
    }

    public static class TextKey {
        // TODO weak reference
        private Object chat;
        private int seed = 0;
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
                hash = ChatEvents.hashCode(chat,seed);
                chat = null;
            }

            return hash;
        }
    }

    public static Object textKey(Object input) {
        TextKey key = new TextKey();
        key.chat = input;
        return key;
    }

    public static Object textKey(Object input, Object attachment) {
        TextKey key = new TextKey();
        key.chat = input;
        key.seed = Objects.hashCode(attachment);
        return key;
    }
}
