package karl.codes.minecraft.antispam.rules;

import karl.codes.antispam.Action;
import karl.codes.antispam.IRule;
import karl.codes.minecraft.antispam.AntiSpam;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Created by karl on 11/1/15.
*/
public class SpamRule implements IRule<CharSequence> {
    public static final SpamRule OK = new SpamRule("", Action.OK);

    private Pattern pattern;
    private String name = null;
    private Action onHit = Action.DENY;
    private SpamRule onMiss; // TODO list
    private SpamRule root = this; // used for builder notation :/
    /**
     * name required for last hit
     */
    public Pattern lastPattern = null;

    private static ConcurrentMap<String,String> EXAMPLES = new ConcurrentHashMap<>();

    private final int id = AntiSpam.nextRuleId();

    private String stringify = null;

    // TODO intellij validator hint?
    public SpamRule(String regex, Action onHit) {
        this(Pattern.compile(regex),onHit);
    }

    protected SpamRule(Pattern regex, Action onHit) {
        this.pattern = regex;
        this.onHit = onHit;
    }

    public SpamRule named(String name) {
        this.name = name;

        return this;
    }

    // TODO no chain in builder
    public SpamRule notAfter(String lastHit) {
        // capture a hit, process chain only if pattern matches
        SpamRule chain = new SpamRule(this.pattern,Action.NEXT)
                // terminal state for matching last hit
                .then(".",this.onHit.getOpposite()).named(lastHit).ifAfter(lastHit)
                // original terminal state
                .then(this);

        this.pattern = Pattern.compile(".");

        return chain.build();
    }

    public SpamRule ifAfter(String lastHit) {
        if(lastHit.startsWith("/") && lastHit.endsWith("/")) {
            this.lastPattern = Pattern.compile(lastHit.substring(1,lastHit.length()-1));
        } else {
            // TODO bounds?
            this.lastPattern = Pattern.compile(lastHit, Pattern.LITERAL);
        }

        return this;
    }

    public SpamRule then(SpamRule next) {
        this.onMiss = next;
        next.root = this.root;

        return next;
    }

    public SpamRule then(String regex, Action onHit) {
        return then(new SpamRule(regex, onHit));
    }

    public SpamRule build() {
        return this.root;
    }

    public boolean test(CharSequence input, String last) {
        if(lastPattern != null) {
            if(last == null) return false;

            Matcher m = lastPattern.matcher(last);

            if(!m.find()) return false;
        }

        Matcher m = pattern.matcher(input);

        if(!m.find()) return false;

        makeString(m,last);

        return true;
    }

    @Override
    public Action onHit() {
        return onHit;
    }

    @Override
    public IRule<CharSequence> onMiss() {
        return onMiss;
    }

    @Override
    public String name() {
        return name;
    }

    private String makeString(Matcher m, String last) {
        if(stringify == null) {
            if(name != null) {
                return stringify = id + " - " + name;
            } else {
                String example = m.group();
                if (example.length() > 7)
                    example = example.substring(0, 4) + "...";
                if (EXAMPLES.putIfAbsent(example, example) == null) {
                    String s = id + " - " + example;
                    if (root != this)
                        s = root.id + "->" + s;
                    if (onMiss != null)
                        s = s + " -> " + onMiss.id;

                    return stringify = s;
                }
            }
        }

        return stringify;
    }

    public String toString() {
        if(stringify != null) return stringify;
        if(name != null) return makeString(null,null);
        return String.valueOf(id);
    }

    // TODO no chain in builder
    public SpamRule onlyOnce() {
        SpamRule limiter = new SpamRule(this.pattern,this.onHit.getOpposite())
                .ifAfter(this.name);
        limiter.onMiss = this;

        return limiter;
    }
}
