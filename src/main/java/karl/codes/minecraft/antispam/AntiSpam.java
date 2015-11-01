package karl.codes.minecraft.antispam;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import karl.codes.java.ListCharSequence;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
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
import scala.Char;

import javax.annotation.Nullable;
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

    private static Function<IChatComponent, CharSequence> TEXT_REDUCE_NOBAKE = new Function<IChatComponent, CharSequence>() {
        @Nullable
        @Override
        public CharSequence apply(@Nullable IChatComponent input) {
            return reduce(false, input);
        }
    };

    private static Function<IChatComponent, CharSequence> TEXT_REDUCE = new Function<IChatComponent, CharSequence>() {
        @Nullable
        @Override
        public CharSequence apply(IChatComponent input) {
            return reduce(true, input);
        }
    };

    private static CharSequence reduce(boolean bake, IChatComponent input) {
        List<IChatComponent> siblings = input.getSiblings();
        if(siblings.size() == 0) {
            return input.getUnformattedTextForChat();
        } else {
            return new ListCharSequence(bake, Lists.transform(siblings,TEXT_REDUCE_NOBAKE));
        }
    }

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
        Pattern p = null;

        List<IChatComponent> siblings = event.message.getSiblings();
        // TODO this is copy-avoidance in the extreme, it is possibly slower because of many small strings, even with the reduce operation
        CharSequence text = new ListCharSequence(Lists.transform(siblings,TEXT_REDUCE));

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
                            // fall thru!
                    }
                }
                // miss or falling thru!
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
        public Rule onMiss;

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
            return "[record id] [action] [args]\n" +
                    "0|hit0|last|lastHit (show)\n" +
                    "1|hit1 ok|permit (regex)\n" + // fails if regex doesn't match, if regex matches a new rule is added before the final position in chain (or inserts a new chain if it is the head)
                    "miss0 deny (regex)\n" + // fails if regex doesn't match, if regex matches a new rule is added as a new chain
                    "";
        }

        @Override
        public void processCommand(ICommandSender iCommandSender, String[] strings) throws CommandException {
            if(strings.length < 1) {
                throw new CommandException("AntiSpam command is too short");
            }

            String recordId = null,subcommand = null,pattern = null;

            switch(strings.length) {
                case 0:
                    throw new CommandException("AntiSpam command is too short");
                case 1:
                    subcommand = "show";
                case 2:
                    pattern = "";
                default:
                    if(pattern == null) pattern = strings[2];
                    if(subcommand == null) subcommand = strings[1];
                    recordId = strings[0];
            }

            // doesn't actually do anything yet, derps
            iCommandSender.addChatMessage(new ChatComponentText(MessageFormat.format("record={0} command={1} pattern={2}",recordId,subcommand,pattern)));
        }
    }
}
