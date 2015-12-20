package karl.codes.minecraft.fml.antispam;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import karl.codes.minecraft.forge.client.event.EventFixture;
import karl.codes.rules.RuleKernel;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;

import java.io.File;

/**
 * Created by karl on 12/19/15.
 */
public class AntiSpamServerSide extends AntiSpamSideBase<ClientChatReceivedEvent> {
    @SubscribeEvent
    public void event(ClientChatReceivedEvent event) {
        spiEvent(event,EventFixture.CLIENT_CHAT_PREPEND_MUTATOR,EventFixture.CANCEL_EVENT);
    }

    @SubscribeEvent
    public void onLoad(WorldEvent.Load event) {
        runtime.clear();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(new Command());
    }

    @Override
    public void resetRuntime(File configFile) {
        runtime = new RuleKernel<>(
                EventFixture.CLIENT_CHAT_AS_CHARSEQUENCE,
                loadRules(configFile)
        );
    }

    @Override
    protected Object messageKey(ClientChatReceivedEvent event, String last) {
        return EventFixture.messageKey(event,last);
    }

    @Override
    protected boolean canCommandSenderUseCommand(ICommandSender iCommandSender) {
        return iCommandSender instanceof EntityPlayerSP;
    }
}
