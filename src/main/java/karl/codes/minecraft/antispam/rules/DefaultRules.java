package karl.codes.minecraft.antispam.rules;

import com.google.common.collect.ImmutableList;
import karl.codes.rules.Action;
import karl.codes.rules.Rule;

import java.util.List;

/**
 * Created by karl on 11/1/15.
 */
public class DefaultRules {
    public static List<Rule<CharSequence>> factionsDefaults() {
        return ImmutableList.<Rule<CharSequence>>builder()
                // factions nametag chat
                .add(new SpamRule("^<", Action.OK).named("chat"))
                // factions
                .add(new SpamRule("____\\.\\[.*\\]\\.____", Action.OK).named("factions"))
                // alternate permissive hbar pattern
//                .add(new SpamRule("(-\\s?){3}",Action.OK).named("factions").ifAfter("factions"))
                // clearlag
                .add(new SpamRule("^\\[ClearLag\\]\\s", Action.OK))
                .add(new SpamRule("^\\[mcMMO\\]", Action.OK).named("mcmmo")
                    .onlyOnce()) // rate-limit mcMMO and deny 2nd message which contains url
                // link spam
                .add(new SpamRule("^(?i).links.\\s", Action.DENY))
                // hbar
                .add(new SpamRule("(?:[-+~=\\*\\\\/]\\s?){3}", Action.DENY)
                    .notAfter("factions")) // permit hbar for factions map!
                // voting nag
                .add(new SpamRule("(?i)vote", Action.NEXT)
                        .then("(?i)voted? .* us", Action.DENY)
                        .then("(?i)you .* votes?", Action.DENY)
                        .then("(?i)has voted @", Action.DENY)
                        .then("^\\[Vote4Cash\\]", Action.DENY)
                        .build())
                .build();
    }
}
