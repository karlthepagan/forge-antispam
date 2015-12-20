package karl.codes.minecraft.fml.antispam;

import cpw.mods.fml.common.SidedProxy;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

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

    @SidedProxy(
            clientSide = "karl.codes.minecraft.fml.antispam.AntiSpamClientSide",
            serverSide = "karl.codes.minecraft.fml.antispam.AntiSpamServerSide"
    )
    AntiSpamSideBase sideProxy;

    public static AtomicInteger IDS = new AtomicInteger(1);

    public AntiSpam() {
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        sideProxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(sideProxy);

        sideProxy.init(event);
    }

    // TODO move to rules creation context?
    public static int nextRuleId() {
        return IDS.getAndIncrement();
    }
}
