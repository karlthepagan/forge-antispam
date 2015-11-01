package karl.codes.minecraft.antispam;

import com.google.common.collect.ImmutableList;
import karl.codes.minecraft.ChatEvents;
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
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.text.MessageFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod(
        useMetadata = true,
        modid = AntiSpam.MODID,
        acceptableRemoteVersions = "*",
        acceptedMinecraftVersions = "[1.7.0,)",
        canBeDeactivated = true)
@SideOnly(Side.CLIENT)
public class AntiSpam {
    public static final String MODID = "antispam";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new Command());
    }

    private final List<Rule> rules = ImmutableList.<Rule>builder()
            // factions nametag chat
            .add(new Rule("^<", Action.OK))
            // clearlag
            .add(new Rule("^\\[ClearLag\\]\\s", Action.OK))
            .add(new Rule("^\\[mcMMO\\]", Action.OK)) // TODO rate-limit mcMMO and deny 2nd message which contains url
            // link spam
            .add(new Rule("^(?i).links.\\s", Action.DENY))
            // hbar
            .add(new Rule("(?:[-+~=\\*]\\s?){3}", Action.DENY))
            // voting nag
            .add(new Rule("(?i)voted? .* us", Action.DENY)
                    .then("(?i)you .* votes?", Action.DENY))
            .build();

    @SubscribeEvent
    public void event(ClientChatReceivedEvent event) {
        // TODO this is copy-avoidance in the extreme, it is possibly slower because of many small strings, even with the reduce operation
        CharSequence text = ChatEvents.asCharSequence(event);

        applyRules(event, text);
    }

    public void applyRules(Event event, CharSequence text) {
        for(Rule r : rules) {
            while(r != null) {
                Matcher regex = r.pattern.matcher(text);
                if (regex.find()) {
                    switch (r.onHit) {
                        case OK:
                            return;
                        case DENY:
                            event.setCanceled(true);
                            return;
                        case NEXT:
                            // equivalent to fall thru, easier to debug tho
                            r = r.onMiss;
                            continue;
                    }
                }
                // miss
                r = r.onMiss;
            }
        }

        // all missed!
    }

    enum Action {
        OK,
        DENY,
        NEXT;
    }

    static class Rule {
        public Pattern pattern;
        public Action onHit = Action.DENY;
        public Rule onMiss; // TODO list

        // TODO intellij validator hint?
        public Rule(String pattern, Action onHit) {
            this.pattern = Pattern.compile(pattern);
            this.onHit = onHit;
        }

        public Rule then(String pattern, Action onHit) {
            Rule next = new Rule(pattern, onHit);
            this.onMiss = next;
            return next;
        }
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
            if(strings.length < 1) {
                throw new CommandException("AntiSpam command is too short");
            }

            String arg0 = null,arg1 = null,arg2 = null;

            switch(strings.length) {
                case 3:
                    arg2 = strings[2];
                case 2:
                    arg1 = strings[1];
                case 1:
                    arg1 = strings[0];
                default:
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
                    return null;
            }
        }

        public String defaultArg(String command, String arg1) throws CommandException {
            switch(command) {
                case "show":
                    return "hits";
                default:
                    return null;
            }
        }
    }
}
