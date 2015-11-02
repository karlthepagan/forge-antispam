package karl.codes.minecraft.antispam.rules;

import com.google.common.collect.ImmutableList;
import karl.codes.minecraft.antispam.rules.Action;
import karl.codes.minecraft.antispam.rules.Rule;

import java.util.List;

/**
 * Created by karl on 11/1/15.
 */
public class DefaultRules {
    public static List<Rule> factionsDefaults() {
        return ImmutableList.<Rule>builder()
                // factions nametag chat
                .add(new Rule("^<", Action.OK))
                // clearlag
                .add(new Rule("^\\[ClearLag\\]\\s", Action.OK))
                .add(new Rule("^\\[mcMMO\\]", Action.OK)) // TODO rate-limit mcMMO and deny 2nd message which contains url
                // link spam
                .add(new Rule("^(?i).links.\\s", Action.DENY))
                // hbar
//                .add(new Rule("(?:[-+~=\\*]\\){3}", Action.DENY))
                        // TODO permit hbar for factions map!
                // voting nag
                .add(new Rule("(?i)vote", Action.NEXT)
                        .then("(?i)voted? .* us", Action.DENY)
                        .then("(?i)you .* votes?", Action.DENY)
                        .then("(?i)has voted @", Action.DENY)
                        .then("^\\[Vote4Cash\\]", Action.DENY)
                        .build())
                .build();
    }
}
