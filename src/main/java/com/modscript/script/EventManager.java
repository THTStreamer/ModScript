package com.modscript.script;

import com.modscript.registry.ModScriptRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventManager {
    private static final List<EventScript> registeredScripts = new ArrayList<>();
    private static final Map<String, List<List<ASTNode>>> eventHandlers = new ConcurrentHashMap<>();

    public static void init() {
        NeoForge.EVENT_BUS.register(new EventManager());
    }

    public static void registerScript(String eventName, String scriptContent) {
        registeredScripts.add(new EventScript(eventName, scriptContent));
    }

    public static void registerEvent(String eventName, List<ASTNode> actions) {
        eventHandlers.computeIfAbsent(eventName.toLowerCase(), k -> new ArrayList<>()).add(actions);
    }

    @SubscribeEvent
    public void onPlayerAttack(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        ServerPlayer player = (ServerPlayer) event.getEntity();
        executeEvent("attack", player);
        executeEvent("attacks", player);
        for (EventScript script : registeredScripts) {
            if (script.eventName().contains("attacks") || script.eventName().contains("attack")) {
                executeScript(script, player);
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getPlayer();
        if (player instanceof ServerPlayer serverPlayer) {
            executeEvent("break", serverPlayer);
            executeEvent("breaks", serverPlayer);
            for (EventScript script : registeredScripts) {
                if (script.eventName().contains("breaks") || script.eventName().contains("break")) {
                    executeScript(script, serverPlayer);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        ServerPlayer player = (ServerPlayer) event.getEntity();
        executeEvent("join", player);
        executeEvent("joins", player);
        for (EventScript script : registeredScripts) {
            if (script.eventName().contains("joins") || script.eventName().contains("join")) {
                executeScript(script, player);
            }
        }
    }

    private void executeEvent(String eventName, ServerPlayer player) {
        List<List<ASTNode>> handlers = eventHandlers.get(eventName);
        if (handlers == null) return;
        for (var actions : handlers) {
            try {
                ScriptSandbox.startExecution(Thread.currentThread());
                for (var action : actions) {
                    if (action instanceof ASTNode.ActionNode actionNode) {
                        executeAction(actionNode, player);
                    }
                }
            } catch (Exception e) {
                // Silent fail for event handlers
            } finally {
                ScriptSandbox.endExecution(Thread.currentThread());
            }
        }
    }

    private void executeAction(ASTNode.ActionNode action, ServerPlayer player) throws Exception {
        switch (action.getType()) {
            case "give" -> {
                if (action.getParameters().size() >= 2) {
                    int qty = 1;
                    String itemName = "";
                    var p0 = action.getParameters().get(0);
                    var p1 = action.getParameters().get(1);
                    if (p0 instanceof ASTNode.NumberLiteral n) qty = (int) n.getValue();
                    if (p1 instanceof ASTNode.StringLiteral s) itemName = s.getValue();
                    var stack = ModScriptRegistry.getItemStack(itemName);
                    if (stack == null) stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE, qty);
                    else stack.setCount(qty);
                    player.getInventory().add(stack);
                }
            }
            case "teleport" -> {
                if (action.getParameters().size() >= 2) {
                    double x = 0, y = 0;
                    if (action.getParameters().get(0) instanceof ASTNode.NumberLiteral n) x = n.getValue();
                    if (action.getParameters().get(1) instanceof ASTNode.NumberLiteral n) y = n.getValue();
                    player.teleportTo(x, y, player.getZ());
                }
            }
            case "spawn", "summon" -> {
                if (!action.getParameters().isEmpty()) {
                    String mobName = "";
                    if (action.getParameters().get(0) instanceof ASTNode.StringLiteral s) mobName = s.getValue();
                    var type = ModScriptRegistry.getMobType(mobName);
                    if (type != null) {
                        var entity = type.create(player.level());
                        if (entity != null) {
                            entity.setPos(player.getX() + 2, player.getY(), player.getZ());
                            player.level().addFreshEntity(entity);
                        }
                    }
                }
            }
            case "apply" -> {
                if (!action.getParameters().isEmpty()) {
                    String effectName = "";
                    int duration = 10;
                    if (action.getParameters().get(0) instanceof ASTNode.StringLiteral s) effectName = s.getValue();
                    if (action.getParameters().size() > 1 && action.getParameters().get(1) instanceof ASTNode.NumberLiteral n) duration = (int) n.getValue();
                    ModScriptRegistry.applyEffect(player, effectName, duration);
                }
            }
            case "heal" -> {
                if (!action.getParameters().isEmpty()) {
                    float amount = 2.0f;
                    if (action.getParameters().get(0) instanceof ASTNode.NumberLiteral n) amount = (float) n.getValue();
                    player.heal(amount);
                }
            }
            case "deal" -> {
                if (!action.getParameters().isEmpty()) {
                    double amount = 0;
                    if (action.getParameters().get(0) instanceof ASTNode.NumberLiteral n) amount = n.getValue();
                    var entities = player.level().getEntities(player, player.getBoundingBox().inflate(5), e -> !(e instanceof Player));
                    for (var entity : entities) {
                        entity.hurt(entity.damageSources().playerAttack(player), (float) amount);
                    }
                }
            }
            case "play" -> {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            case "shoot" -> {
                var arrow = new net.minecraft.world.entity.projectile.Arrow(player.level(), player, net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.world.item.ItemStack.EMPTY);
                arrow.setPos(player.getX(), player.getEyeY(), player.getZ());
                arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F);
                player.level().addFreshEntity(arrow);
            }
            case "set" -> {
                if (action.getParameters().size() >= 2) {
                    String target = "";
                    if (action.getParameters().get(0) instanceof ASTNode.StringLiteral s) target = s.getValue();
                    if (target.equals("on fire") && action.getParameters().size() >= 4) {
                        int seconds = 5;
                        if (action.getParameters().get(3) instanceof ASTNode.NumberLiteral n) seconds = (int) n.getValue();
                        player.setRemainingFireTicks(seconds * 20);
                    }
                }
            }
            case "remove" -> {
                if (!action.getParameters().isEmpty()) {
                    String target = "";
                    if (action.getParameters().get(0) instanceof ASTNode.StringLiteral s) target = s.getValue();
                    if (target.equals("all")) {
                        player.level().getEntities(player, player.getBoundingBox().inflate(10), e -> !(e instanceof Player)).forEach(e -> e.discard());
                    }
                }
            }
        }
    }

    private void executeScript(EventScript script, ServerPlayer player) {
        try {
            ScriptRuntime.compileAndRun("event:" + script.eventName(), script.scriptContent(), player);
        } catch (Exception e) {
            // Silently ignore event script errors
        }
    }

    public static List<EventScript> getRegisteredScripts() { return List.copyOf(registeredScripts); }
    public static Map<String, List<List<ASTNode>>> getEventHandlers() { return eventHandlers; }

    public record EventScript(String eventName, String scriptContent) {}
}
