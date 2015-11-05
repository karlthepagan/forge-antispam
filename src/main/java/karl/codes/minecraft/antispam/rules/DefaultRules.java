package karl.codes.minecraft.antispam.rules;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Created by karl on 11/1/15.
 */
public class DefaultRules {
    public static List<Rule> factionsDefaults() {
        return ImmutableList.<Rule>builder()
                // factions nametag chat
                .add(new Rule("^<", Action.OK).named("chat"))
                // factions
                .add(new Rule("____\\.\\[.*\\]\\.____", Action.OK).named("factions"))
                // alternate permissive hbar pattern
//                .add(new Rule("(-\\s?){3}",Action.OK).named("factions").ifAfter("factions"))
                // clearlag
                .add(new Rule("^\\[ClearLag\\]\\s", Action.OK))
                .add(new Rule("^\\[mcMMO\\]", Action.OK).named("mcmmo")
                    .onlyOnce()) // rate-limit mcMMO and deny 2nd message which contains url
                // link spam
                .add(new Rule("^(?i).links.\\s", Action.DENY))
                // hbar
                .add(new Rule("(?:[-+~=\\*\\\\/]\\s?){3}", Action.DENY)
                    .notAfter("factions")) // permit hbar for factions map!
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
