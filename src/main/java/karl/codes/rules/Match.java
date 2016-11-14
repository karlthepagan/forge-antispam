package karl.codes.rules;

import java.util.regex.Matcher;

/**
 * Created by karl on 9/3/16.
 */
public interface Match {
    Match HIT = () -> true;
    Match MISS = () -> false;

    boolean isHit();
    default Matcher matcher() {
        return null;
    }
}
