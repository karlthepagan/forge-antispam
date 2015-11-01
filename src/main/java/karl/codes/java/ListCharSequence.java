package karl.codes.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is a experimental copy-avoidance for string fragments.
 */
public class ListCharSequence implements CharSequence {
//    private final int[] log2;
    private transient int lastIndex;
    private transient int lastElement;
    private final int[] limit;
    private final CharSequence[] elements;

    public static final int REDUCE_THRESHOLD = 24;

    protected static CharSequence[] reduce(boolean bake, CharSequence ... items) {
        if(!bake) {
            return items;
        } else {
            return reduce(true, Arrays.asList(items));
        }
    }

    protected static CharSequence[] reduce(boolean bake, List<CharSequence> items) {
        if(!bake) {
            return items.toArray(new CharSequence[items.size()]);
        }

        ArrayList<CharSequence> output = new ArrayList<>(items.size());

        StringBuilder builder = new StringBuilder(REDUCE_THRESHOLD);
        for(CharSequence element : items) {
            if(element.length() + builder.length() < REDUCE_THRESHOLD) {
                builder.append(element);
            } else {
                if(builder.length() > 0) {
                    output.add(builder.toString());
                    builder.setLength(0); // ugh, zeroes data slowly
                }
                output.add(element);
            }
        }
        if(builder.length() > 0) {
            output.add(builder.toString());
        }

        return output.toArray(new CharSequence[output.size()]);
    }

    public ListCharSequence(List<CharSequence> items) {
        this(true, items);
    }

    public ListCharSequence(CharSequence ... items) {
        this(true, items);
    }

    public ListCharSequence(boolean bake, List<CharSequence> items) {
        this(null, reduce(bake, items));
    }

    public ListCharSequence(boolean bake, CharSequence ... items) {
        this(null, reduce(bake, items));
    }

    protected ListCharSequence(Object inner, CharSequence ... items) {
        // clz partition is a mess
//        int[] clzArray = new int[32];
//        int clzIndex = 0;

        elements = items;
        limit = new int[elements.length];
        int last = 0;
//        int lastclz = Integer.numberOfLeadingZeros(1);
        for(int i = 0; i < elements.length; i++) {
            limit[i] = last = last + elements[i].length();
//            int iclz = Integer.numberOfLeadingZeros(last);
//            if(iclz < lastclz && i > 0) {
//                lastclz = iclz;
//                clzArray[clzIndex++] = i-1;
//            }
        }
        // clz 32 -> 0
        // clz 31 -> ? etc
//        log2 = Arrays.copyOf(clzArray,clzIndex);
    }

    /**
     * right now using clz to fake a BSP. Buggy code.
     */
    private int estimateElement(int index, int min) {
        if(index == lastIndex) return lastElement;

        return min;
//        int zlc = 31 - Integer.numberOfLeadingZeros(index);
//        /*
//        if(zlc < 0) {
//            return min;
//        }
//        */
//
//        return log2[zlc];
    }

    @Override
    public int length() {
        return limit[limit.length-1];
    }

    @Override
    public char charAt(int index) {
        if(index < limit[0]) {
            return elements[lastElement = 0].charAt(lastIndex = index);
        }

        int start = estimateElement(index, 1);

        for(int i = start; i < limit.length; i++) {
            if(index < limit[i])
                return elements[lastElement = i].charAt((lastIndex = index) - limit[i-1]);
        }

        throw new IndexOutOfBoundsException(String.valueOf(index));
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        // if we're slicing, just copy it!
        return toString().substring(start, end);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(length());
        // TODO folding append
        for(CharSequence e : elements) {
            sb.append(e);
        }
        // TODO memoize? externally?
        return sb.toString();
    }
}
