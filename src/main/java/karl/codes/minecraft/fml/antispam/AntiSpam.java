package karl.codes.minecraft.fml.antispam;

import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.ImmutableList;
import karl.codes.minecraft.antispam.config.RulesConfig;
import karl.codes.minecraft.antispam.rules.SpamRule;
import karl.codes.minecraft.forge.client.event.EventFixture;
import karl.codes.rules.Rule;
import karl.codes.minecraft.srg.ChatEvents;
import karl.codes.minecraft.antispam.rules.DefaultRules;
import karl.codes.rules.RuleKernel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Mod(
        useMetadata = true,
        modid = AntiSpam.MODID,
        acceptableRemoteVersions = "*",
        acceptedMinecraftVersions = "[1.7.0,)",
        canBeDeactivated = true)
@SideOnly(Side.CLIENT)
public class AntiSpam {
    public static final String MODID = "antispam";

    private Logger log;
    private File configFile;

    private volatile RuleKernel<ClientChatReceivedEvent, CharSequence> runtime;

    public static AtomicInteger IDS = new AtomicInteger(1);

    private boolean passive = false;

    private String lastHitName = null;

    // TODO proxy
    private GuiIngame gui;

    public AntiSpam() {
    }

    public ImmutableList<SpamRule> loadRules(File suggestedConfig) {
        List<URL> candidates = RulesConfig.ruleLocations(suggestedConfig, AntiSpam.class);

        RulesConfig config = new RulesConfig();

        ImmutableList<SpamRule> rules = DefaultRules.factionsDefaults();
        URL resLoaded = null;
        for(URL res : candidates) {
            try {
                ObjectReader reader = config.createReader(res);
                rules = config.read(res,reader);
                resLoaded = res;
                break;
            } catch(Exception e) {
                log.error("Failed to parse rules uri={}",res,e);
            }
        }

        if(resLoaded != null) {
            log.info("loaded rules uri={}",resLoaded);
        }

        return rules;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        log = event.getModLog();
        configFile = event.getSuggestedConfigurationFile();

        resetRuntime();
    }

    private void resetRuntime() {
        runtime = new RuleKernel<>(
                EventFixture.CLIENT_CHAT_AS_CHARSEQUENCE,
                loadRules(configFile)
        );
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new Command());
        gui = Minecraft.getMinecraft().ingameGUI; // TODO proxy
    }

    @SubscribeEvent
    public void onLoad(WorldEvent.Load event) {
        runtime.clear();
    }

    @SubscribeEvent
    public void event(ClientChatReceivedEvent event) {
        spiEvent(event,EventFixture.CLIENT_CHAT_PREPEND_MUTATOR);
    }

    // TODO move to core
    public <T extends ClientChatReceivedEvent> void spiEvent(T event, Consumer<Pair<String,T>> eventMutator) {
        String last = lastHitName;
        Object textKey = EventFixture.messageKey(event, last); // TODO static call -> function

        Rule<CharSequence> rule = runtime.apply(textKey, event, last);

        switch (rule.onHit()) {
            case DENY:
                if(getPassive()) {
                    if(rule.name()!= null) {
                        eventMutator.accept(ImmutablePair.of("BLOCKED(" + rule.name() + "): ",event));
                    } else {
                        eventMutator.accept(ImmutablePair.of("BLOCKED: ",event));
                    }
                } else {
                    event.setCanceled(true);
                }
                break;

            default:
                if (getPassive() && rule.name() != null) {
                    eventMutator.accept(ImmutablePair.of("(" + rule.name() + "): ",event));
                }
                break;
        }
        lastHitName = rule.name();
    }

    public boolean getPassive() {
        return passive;
    }

    public void setPassive(boolean passive) {
        this.passive = passive;
    }

    public static int nextRuleId() {
        return IDS.getAndIncrement();
    }

    // TODO apply SPI design
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
                    "cache (show|clear)" + // show cache and example hits
                    // finds last matching denied events, fails if none found
                    // new rule is added before the final position in chain (or inserts a new chain if it is the head)
                    "permit [string|/regex/]\n" +
                    // finds last matching allowed events, fails if none found
                    // new rule is added as a new chain or immediately before the proceeding OK rule
                    "deny [string|/regex/]\n" +
                    "commit (all|# ... # #)" + // commit the recorded candidate rules
                    "abort" + // abort auto-commit (10 seconds after a unique rule is staged it will auto-commit)
                    "hold (10)" + // set auto-commit hold for a specified time
                    "reload"; // reload config from disk
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
                            for (Rule<CharSequence> r : runtime.getCache()) {
                                iCommandSender.addChatMessage(new ChatComponentText(r.toString()));
                            }
                            return;
                        case "clear":
                            runtime.clear();
                            return;
                    }
                case "debug":
                    setPassive(!getPassive());
                    return;
                case "reload":
                    resetRuntime();
                    return;
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
