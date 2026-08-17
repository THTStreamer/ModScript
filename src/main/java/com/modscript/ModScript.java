package com.modscript;

import com.mojang.logging.LogUtils;
import com.modscript.command.ModCreatorCommands;
import com.modscript.gui.KeyBindings;
import com.modscript.project.ProjectManager;
import com.modscript.registry.ModScriptRegistry;
import com.modscript.script.EventManager;
import com.modscript.script.PermissionSystem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

@Mod(ModScript.MODID)
public class ModScript {
    public static final String MODID = "modscript";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ModScript(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerKeyMappings);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(KeyBindings.class);
        EventManager.init();
        LOGGER.info("ModScript loaded");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("ModScript common setup");
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        KeyBindings.register(event);
    }

    @SubscribeEvent
    public void onCommandsRegister(RegisterCommandsEvent event) {
        ModCreatorCommands.register(event.getDispatcher());
        LOGGER.info("ModScript commands registered");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ProjectManager.init(event.getServer().getServerDirectory());
        ModScriptRegistry.init(event.getServer());
        try {
            var worldPath = event.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            PermissionSystem.loadPermissions(worldPath);
            LOGGER.info("ModScript permissions loaded");
        } catch (Exception e) {
            LOGGER.warn("Failed to load permissions: {}", e.getMessage());
        }
        LOGGER.info("ModScript initialized");
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            PermissionSystem.syncToClient(player);
        }
        var items = ModScriptRegistry.loadAllItems();
        if (!items.isEmpty()) {
            LOGGER.info("Loaded {} ModScript items", items.size());
        }
    }
}
