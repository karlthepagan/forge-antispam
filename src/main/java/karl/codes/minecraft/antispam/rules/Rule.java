package karl.codes.minecraft.antispam.rules;

import karl.codes.minecraft.antispam.AntiSpam;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Created by karl on 11/1/15.
*/
public class Rule {
    public static final Rule OK = new Rule("", Action.OK);
    private static AtomicInteger IDS = new AtomicInteger(1);

    private final Pattern pattern;
    public Action onHit = Action.DENY;
    public Rule onMiss; // TODO list

    public Rule root = this;

    private static ConcurrentMap<String,String> EXAMPLES = new ConcurrentHashMap<>();

    private final int id = IDS.getAndIncrement();
    public String stringify = null;

    // TODO intellij validator hint?
    public Rule(String regex, Action onHit) {
        this.pattern = Pattern.compile(regex);
        this.onHit = onHit;
    }

    public Rule then(String regex, Action onHit) {
        Rule next = new Rule(regex, onHit);
        this.onMiss = next;
        next.root = this.root;

        return next;
    }

    public Rule build() {
        return this.root;
    }

    public boolean test(CharSequence input) {
        Matcher m = pattern.matcher(input);

        if(!m.find()) return false;

        if(stringify == null) {
            String example = m.group();
            if(example.length() > 7)
                example = example.substring(0,4) + "...";
            if(EXAMPLES.putIfAbsent(example, example) == null) {
                String s = id + " - " + example;
                if(root != this)
                    s = root.id + "->" + s;
                if(onMiss != null)
                    s = s + " -> " + onMiss.id;

                stringify = s;
            }
        }

        return true;
    }

    public String toString() {
        if(stringify != null) return stringify;
        return String.valueOf(id);
    }
}
