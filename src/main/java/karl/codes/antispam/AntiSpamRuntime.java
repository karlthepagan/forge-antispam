package karl.codes.antispam;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

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
public class AntiSpamRuntime<T,N> {

    private final Function<T,N> eventNormalizer;

    private final List<Rule<N>> rules;

    // TODO LRU
    private final ConcurrentMap<Object,Rule<N>> cachedHits = new ConcurrentHashMap<>();

    // TODO - single cache?
    private final Cache<T,Rule<N>> hits = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private final Cache<T,Rule<N>> misses = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public AntiSpamRuntime(Function<T,N> eventNormalizer, List<Rule<N>> rules) {
        this.eventNormalizer = eventNormalizer;
        this.rules = rules;
    }

    public void clear() {
        cachedHits.clear();
    }

    public Rule<N> chatEvent(Object textKey, T event, String last) {
        Rule<N> rule = applyCachedRule(textKey, event);
        if(rule == null) {
            // TODO this is copy-avoidance in the extreme, it is possibly slower because of many small strings, even with the reduce operation
            N text = eventNormalizer.apply(event);

            rule = applyRules(event, last, textKey, text);
        } else {
            textKey = null; // hooray!
        }

        if(textKey != null)
            cachedHits.put(textKey, rule);

        return rule;
    }

    private Rule<N> applyCachedRule(Object textKey, T event) {
        return cachedHits.get(textKey);
    }

    public Rule<N> applyRules(T event, String last, Object textKey, N text) {
        outOfChain:
        for(Rule<N> r : rules) {
            inChain:
            while(r != null) {
                if (r.test(text,last)) {
                    switch (r.onHit()) {
                        case NEXT:
                            // rule branching
                            r = r.onMiss();
                            continue inChain;
                        case PASS:
                            continue outOfChain;
                        default:
                            return r;
                    }
                } else if(r.onHit() == Action.NEXT){
                    continue outOfChain;
                } else if(r.onHit() == Action.PASS) {
                    // rule branching
                    r = r.onMiss();
                    continue inChain;
                }

                // miss
                r = r.onMiss();
            }
        }

        // all missed!
        return (Rule<N>) Rule.OK;
    }

    public Collection<Rule<N>> getCache() {
        return  cachedHits.values();
    }
}
