package karl.codes.antispam;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import karl.codes.minecraft.antispam.rules.Action;
import karl.codes.minecraft.antispam.rules.DefaultRules;
import karl.codes.minecraft.antispam.rules.Rule;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * A trivial rules engine. Logical branching is achieved using a list of rule chains, with
 * dependent rules down each chain. Misses progress down the list and down each chain unless
 * specified otherwise: "PASS" to skip a chain or "NEXT" to progress down the chain only on a match.
 *
 * The rule names are used to hold state. The name of the previous Rule end-state (OK or DENY) is
 * the only memory provided by this engine. Users should utilize DFA techniques to achieve
 * complex features.
 */
public class AntiSpamRuntime<T> {

    private final Function<T,CharSequence> asCharacters;

    private final List<Rule> rules = DefaultRules.factionsDefaults();

    // TODO LRU
    private final ConcurrentMap<Object,Rule> cachedHits = new ConcurrentHashMap<>();

    // TODO - single cache?
    private final Cache<T,Rule> hits = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private final Cache<T,Rule> misses = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public AntiSpamRuntime(Function<T,CharSequence> asCharacters) {
        this.asCharacters = asCharacters;
    }

    public void clear() {
        cachedHits.clear();
    }

    public Rule chatEvent(Object textKey, T event, String last) {
        Rule rule = applyCachedRule(textKey, event);
        if(rule == null) {
            // TODO this is copy-avoidance in the extreme, it is possibly slower because of many small strings, even with the reduce operation
            CharSequence text = asCharacters.apply(event);

            rule = applyRules(event, last, textKey, text);
        } else {
            textKey = null; // hooray!
        }

        if(textKey != null)
            cachedHits.put(textKey, rule);

        return rule;
    }

    private Rule applyCachedRule(Object textKey, T event) {
        return cachedHits.get(textKey);
    }

    public Rule applyRules(T event, String last, Object textKey, CharSequence text) {
        outOfChain:
        for(Rule r : rules) {
            inChain:
            while(r != null) {
                if (r.test(text,last)) {
                    switch (r.onHit) {
                        case NEXT:
                            // rule branching
                            r = r.onMiss;
                            continue inChain;
                        case PASS:
                            continue outOfChain;
                        default:
                            return r;
                    }
                } else if(r.onHit == Action.NEXT){
                    continue outOfChain;
                } else if(r.onHit == Action.PASS) {
                    // rule branching
                    r = r.onMiss;
                    continue inChain;
                }

                // miss
                r = r.onMiss;
            }
        }

        // all missed!
        return Rule.OK;
    }

    public Collection<Rule> getCache() {
        return  cachedHits.values();
    }
}
