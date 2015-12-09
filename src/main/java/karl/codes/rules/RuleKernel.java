package karl.codes.rules;

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
public class RuleKernel<T,N> {

    private final Function<T,N> eventNormalizer;

    private final List<Rule<N>> rules;

    // TODO LRU
    private final ConcurrentMap<Object,Rule<N>> cachedHits = new ConcurrentHashMap<>();

    private final Cache<Rule<N>,T> hits = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private final Cache<Rule<N>,T> misses = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public RuleKernel(Function<T,N> eventNormalizer,
                      List<Rule<N>> rules) {
        this.eventNormalizer = eventNormalizer;
        this.rules = rules;
    }

    public void clear() {
        cachedHits.clear();
    }

    public Rule<N> apply(Object hashKey, T event, String contextName) {
        Rule<N> rule = applyCached(hashKey, event);
        if(rule == null) {
            N normalized = eventNormalizer.apply(event);

            rule = applyNormalized(hashKey, event, normalized, contextName);
        } else {
            hashKey = null; // hooray!
        }

        if(hashKey != null)
            cachedHits.put(hashKey, rule);

        return rule;
    }

    private Rule<N> applyCached(Object hashKey, T event) {
        return cachedHits.get(hashKey);
    }

    /**
     * Full rule logic
     * @param hashKey hash key used for cached lookups
     * @param event canonical event
     * @param normalized normalized event
     * @param contextName name of the logical context for this rule (last hit, current motivation, etc)
     * @return matching rule
     */
    public Rule<N> applyNormalized(Object hashKey, T event, N normalized, String contextName) {
        outOfChain:
        for(Rule<N> r : rules) {
            inChain:
            while(r != null) {
                if (r.test(normalized,contextName)) {
                    switch (r.onHit()) {
                        case NEXT:
                            // rule branching
                            r = r.onMiss();
                            continue inChain;
                        case PASS:
                            continue outOfChain;
                        default:
                            hits.put(r,event);
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
        misses.put(Rule.MISS, event);
        return (Rule<N>) Rule.MISS;
    }

    public Collection<Rule<N>> getCache() {
        return  cachedHits.values();
    }
}
