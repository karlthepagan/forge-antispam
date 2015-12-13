package karl.codes.minecraft.antispam.config.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import karl.codes.java.PatternStrategyDeserializer;
import karl.codes.minecraft.antispam.rules.SpamRule;
import karl.codes.rules.Action;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Model for spam rules using the JSON/YAML style.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class SpamRuleNode {

    @JsonProperty
    @JsonDeserialize(using = PatternStrategyDeserializer.class)
    private Pattern pattern = null;
    @JsonProperty
    private Action action = Action.DENY;
    @JsonProperty
    private String name = null;
    @JsonProperty
    private boolean onlyOnce = false;
    @JsonProperty
    private String notAfter = null;
    private List<SpamRuleNode> next = null;

    @JsonSetter
    public void setNext(List<SpamRuleNode> next) {
        action = Action.NEXT;
        this.next = next;
    }

    @JsonGetter
    public List<SpamRuleNode> getNext() {
        return next;
    }

    public SpamRule build() {
        SpamRule rule = new SpamRule(pattern, action);
        if(name != null)
            rule.named(name);
        if(onlyOnce)
            rule = rule.onlyOnce();
        if(notAfter != null)
            rule = rule.notAfter(notAfter);
        if(next != null) {
            for(SpamRuleNode node : next) {
                rule = rule.then(node.build());
            }
        }

        return rule.build();
    }

    public static final ImmutableList<SpamRule> build(List<SpamRuleNode> nodes) {
        ImmutableList.Builder rules = ImmutableList.builder();

        for(SpamRuleNode node : nodes) {
            rules.add(node.build());
        }

        return rules.build();
    }
}
