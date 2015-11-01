package karl.codes.java;

import org.hamcrest.CoreMatchers;
import org.junit.*;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by karl on 10/31/2015.
 */
public class ListCharSequenceTest {
    public static final String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCharAt() throws Exception {
        CharSequence ab = new ListCharSequence("a","b","cd","efg","hijk");

        assertThat(ab.charAt(0), CoreMatchers.is('a'));
        assertThat(ab.charAt(1), CoreMatchers.is('b'));
        assertThat(ab.charAt(2), CoreMatchers.is('c'));
        assertThat(ab.charAt(3), CoreMatchers.is('d'));
        assertThat(ab.charAt(4), CoreMatchers.is('e'));
        assertThat(ab.charAt(5), CoreMatchers.is('f'));
        assertThat(ab.charAt(6), CoreMatchers.is('g'));
    }

    @Test
    public void testRandomSubstrings() throws Exception {
        Random rnd = new Random();

        for(int round = 0; round < 10; round++) {
            String source = ALPHA;
            ArrayList<String> list = new ArrayList<>();

            while (source.length() > rnd.nextInt(5)) {
                int off = rnd.nextInt(source.length());
                list.add(source.substring(0, off));
                source = source.substring(off);
            }
            list.add(source);

            CharSequence data = new ListCharSequence(list.toArray(new CharSequence[0]));

            for (int i = ALPHA.length() - 1; i >= 0; i--) {
                assertThat(data.charAt(i), CoreMatchers.is(ALPHA.charAt(i)));
            }

            assertThat(data.toString(), CoreMatchers.is(ALPHA));
        }
    }
}