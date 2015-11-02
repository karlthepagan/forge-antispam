package karl.codes.minecraft.antispam;

import karl.codes.minecraft.ChatEvents;
import karl.codes.minecraft.antispam.rules.DefaultRules;
import karl.codes.minecraft.antispam.rules.Rule;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Mod(
        useMetadata = true,
        modid = AntiSpam.MODID,
        acceptableRemoteVersions = "*",
        acceptedMinecraftVersions = "[1.7.0,)",
        canBeDeactivated = true)
@SideOnly(Side.CLIENT)
public class AntiSpam {
    public static final String MODID = "antispam";

    public static AtomicInteger IDS = new AtomicInteger(1);

    private final List<Rule> rules = DefaultRules.factionsDefaults();

    // TODO LRU
    private final ConcurrentMap<Object,Rule> cachedHits = new ConcurrentHashMap<>();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new Command());
    }

    @SubscribeEvent
    public void event(ClientChatReceivedEvent event) {
        Object textKey = ChatEvents.textKey(event.message);
        Rule rule = applyCachedRule(textKey, event);
        if(rule == null) {
            // TODO this is copy-avoidance in the extreme, it is possibly slower because of many small strings, even with the reduce operation
            CharSequence text = ChatEvents.asCharSequence(event.message);

            rule = applyRules(event, textKey, text);
        } else {
            textKey = null; // hooray!
        }

        if(textKey != null)
            cachedHits.put(textKey,rule);

        switch (rule.onHit) {
            case DENY:
                event.setCanceled(true);
            default:
                // do nothing
        }
    }

    private Rule applyCachedRule(Object textKey, ClientChatReceivedEvent event) {
        return cachedHits.get(textKey);
    }

    public Rule applyRules(Event event, Object textKey, CharSequence text) {
        for(Rule r : rules) {
            while(r != null) {
                if (r.test(text)) {
                    switch (r.onHit) {
                        case NEXT:
                            // equivalent to fall thru, easier to debug tho
                            r = r.onMiss;
                            continue;
                        default:
                            return r;
                    }
                }
                // miss
                r = r.onMiss;
            }
        }

        // all missed!
        return Rule.OK;
    }

    public static int nextRuleId() {
        return IDS.getAndIncrement();
    }

    class Command extends CommandBase {
        @Override
        public String getCommandName() {
            return "spam";
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender iCommandSender) {
            return iCommandSender instanceof EntityPlayerSP;
        }

        @Override
        public String getCommandUsage(ICommandSender iCommandSender) {
            return "[action] [args]\n" +
                    // TODO look at apache WAF module? command line api?
                    "(show) (10) (hits)\n" + // show last hits (denied events)
                    "cache (clear)" + // show cache and example hits
                    // finds last matching denied events, fails if none found
                    // new rule is added before the final position in chain (or inserts a new chain if it is the head)
                    "permit [string|/regex/]\n" +
                    // finds last matching allowed events, fails if none found
                    // new rule is added as a new chain or immediately before the proceeding OK rule
                    "deny [string|/regex/]\n" +
                    "commit (all|# ... # #)" + // commit the recorded candidate rules
                    "abort"; // abort auto-commit (10 seconds after a unique rule is staged it will auto-commit)
        }

        @Override
        public void processCommand(ICommandSender iCommandSender, String[] strings) throws CommandException {
            String arg0 = null,arg1 = null,arg2 = null;

            switch(strings.length) {
                default:
                case 3:
                    arg2 = strings[2];
                case 2:
                    arg1 = strings[1];
                case 1:
                    arg0 = strings[0];
                case 0:
                    if(arg0 == null) {
                        arg0 = "show";
                    }

                    if(arg1 == null) {
                        arg1 = defaultArg(arg0);
                    }

                    if(arg2 == null) {
                        arg2 = defaultArg(arg0,arg1);
                    }
            }

            // doesn't actually do anything yet, derps
            iCommandSender.addChatMessage(new ChatComponentText(MessageFormat.format("command={0} arg1={1} arg2={2}",arg0,arg1,arg2)));

            switch(arg0) {
                case "cache":
                    switch(arg1) {
                        case "":
                            for (Rule r : cachedHits.values()) {
                                iCommandSender.addChatMessage(new ChatComponentText(r.toString()));
                            }
                            return;
                        case "clear":
                            cachedHits.clear();
                            return;
                    }
                default:
                    iCommandSender.addChatMessage(new ChatComponentText("not yet implemented"));
            }
        }

        public String defaultArg(String command) throws CommandException {
            switch (command) {
                case "show":
                    return "10";

                case "commit":
                    return "all";

                case "permit":
                case "deny":
                    throw new CommandException(command + " requires 1 arg");

                default:
                    return "";
            }
        }

        public String defaultArg(String command, String arg1) throws CommandException {
            switch(command) {
                case "show":
                    return "hits";
                default:
                    return "";
            }
        }
    }
}
