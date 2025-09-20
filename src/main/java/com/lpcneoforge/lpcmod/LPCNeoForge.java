package com.lpcneoforge.lpcmod;

import com.lpcneoforge.lpcmod.command.LPCCommand;
import com.lpcneoforge.lpcmod.config.LPCConfig;
import com.lpcneoforge.lpcmod.listener.LPCEventListener;
import lombok.Getter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import com.lpcneoforge.lpcmod.command.AchatCommand;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Getter
@Mod(value = LPCNeoForge.MOD_ID, dist = Dist.DEDICATED_SERVER)
public final class LPCNeoForge {

    public static final String MOD_ID = "lpcmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    private final ModContainer modContainer;
    private final IEventBus eventBus;
    private final LPCConfig config;

    private final LPCEventListener eventListener;

    public LPCNeoForge(ModContainer container, IEventBus modEventBus) {
        modContainer = container;
        eventBus = modEventBus;
        config = new LPCConfig();
        if (FMLEnvironment.dist.isClient()) {
            throw new IllegalStateException("LPC-NeoForge: This mod is not designed to run on the client! Disabling...");
        } else {
            eventListener = new LPCEventListener(config);
            initEventListeners(eventBus);
            container.registerConfig(ModConfig.Type.COMMON, config.getSpec());
        }
    }

    private void registerCommands(RegisterCommandsEvent e) {
        LPCCommand achatCommand = new AchatCommand(config, eventListener);
        achatCommand.register(e.getDispatcher());
    }

    private void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        server.getAllLevels().forEach(serverLevel ->
                serverLevel.getGameRules().getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(false, server)
        );
        LOGGER.info("Death messages have been disabled.");
    }

    private void commonSetup(final FMLCommonSetupEvent ignoredEvent) {
        final String displayName = modContainer.getModInfo().getDisplayName();
        final String version = modContainer.getModInfo().getVersion().toString();

        LOGGER.info("   .     __   ___                      ");
        LOGGER.info("   |    |__) |     {} v{}", displayName, version);
        LOGGER.info("   |___ |    |___  Chat Formatter      ");
        LOGGER.info("                                       ");
    }

    private void initEventListeners(IEventBus event) {
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        event.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(eventListener);
    }
}