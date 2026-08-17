package com.modscript.script;

import com.modscript.registry.ModScriptRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptRuntime {
    private static final Map<String, List<ASTNode>> eventHandlers = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> currentProject = new ThreadLocal<>();

    public static void execute(String script) throws Exception {
        ModScriptLexer lexer = new ModScriptLexer(script);
        var tokens = lexer.tokenize();
        ModScriptParser parser = new ModScriptParser(tokens);
        var ast = parser.parse();
        executeNode(ast);
    }

    public static void compileAndRun(String project, String script, ServerPlayer player) {
        currentProject.set(project);
        try {
            var validation = ServerValidator.validate(script, player);
            if (!validation.valid()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cScript validation failed:"));
                validation.errors().forEach(e -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal("  §c- " + e)));
                return;
            }
            if (!validation.warnings().isEmpty()) {
                validation.warnings().forEach(w -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eWarning: " + w)));
            }
            ScriptSandbox.startExecution(Thread.currentThread());
            execute(script);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aScript executed successfully!"));
        } catch (Exception e) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cExecution error: " + e.getMessage()));
        } finally {
            ScriptSandbox.endExecution(Thread.currentThread());
        }
    }

    private static void executeNode(ASTNode node) throws Exception {
        if (node instanceof ASTNode.Program program) { for (var stmt : program.getStatements()) executeNode(stmt); }
        else if (node instanceof ASTNode.CreateItemStatement createItem) {
            ModScriptRegistry.registerItem(createItem.getName(), createItem.getProperties());
        }
        else if (node instanceof ASTNode.CreateBlockStatement createBlock) {
            ModScriptRegistry.registerBlock(createBlock.getName(), createBlock.getProperties());
        }
        else if (node instanceof ASTNode.CreateMobStatement createMob) {
            ModScriptRegistry.registerMob(createMob.getName(), createMob.getProperties());
        }
        else if (node instanceof ASTNode.CreateEffectStatement createEffect) {
            ModScriptRegistry.registerEffect(createEffect.getName(), createEffect.getProperties());
        }
        else if (node instanceof ASTNode.CreateRecipeStatement createRecipe) {
            ModScriptRegistry.registerRecipe(createRecipe.getName(), createRecipe.getProperties());
        }
        else if (node instanceof ASTNode.CreateAbilityStatement createAbility) {
            ModScriptRegistry.registerAbility(createAbility.getName(), createAbility.getProperties());
        }
        else if (node instanceof ASTNode.WhenStatement when) {
            com.modscript.script.EventManager.registerEvent(when.getEvent(), when.getActions());
        }
        else if (node instanceof ASTNode.ActionNode action) {
            executeAction(action);
        }
    }

    private static void executeAction(ASTNode.ActionNode action) throws Exception {
        ScriptSandbox.checkExecution(Thread.currentThread());
        switch (action.getType()) {
            case "give" -> executeGive(action.getParameters());
            case "teleport" -> executeTeleport(action.getParameters());
            case "spawn" -> executeSpawn(action.getParameters());
            case "remove" -> executeRemove(action.getParameters());
            case "apply" -> executeApply(action.getParameters());
            case "heal" -> executeHeal(action.getParameters());
            case "summon" -> executeSummon(action.getParameters());
            case "shoot" -> executeShoot(action.getParameters());
            case "set" -> executeSet(action.getParameters());
            case "deal" -> executeDeal(action.getParameters());
            case "play" -> executePlay(action.getParameters());
        }
    }

    private static void executeGive(java.util.List<ASTNode> params) throws Exception {
        if (params.size() < 2) return;
        int qty = getIntValue(params.get(0));
        String itemName = getStringValue(params.get(1));
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            ItemStack stack = ModScriptRegistry.getItemStack(itemName);
            if (stack == null) stack = new ItemStack(Items.STONE, qty);
            else stack.setCount(qty);
            player.getInventory().add(stack);
        }
    }

    private static void executeTeleport(java.util.List<ASTNode> params) throws Exception {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && params.size() >= 2) {
            double x = getDoubleValue(params.get(0));
            double y = getDoubleValue(params.get(1));
            double z = params.size() >= 3 ? getDoubleValue(params.get(2)) : player.getZ();
            player.teleportTo(x, y, z);
        }
    }

    private static void executeSpawn(java.util.List<ASTNode> params) throws Exception {
        if (params.isEmpty()) return;
        String mobName = getStringValue(params.get(0));
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            EntityType<?> type = ModScriptRegistry.getMobType(mobName);
            if (type != null) {
                var entity = type.create(player.level());
                if (entity != null) {
                    entity.setPos(player.getX() + 2, player.getY(), player.getZ());
                    player.level().addFreshEntity(entity);
                }
            }
        }
    }

    private static void executeRemove(java.util.List<ASTNode> params) throws Exception {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && !params.isEmpty()) {
            String target = getStringValue(params.get(0));
            if (target.equals("all")) {
                player.level().getEntities(player, player.getBoundingBox().inflate(10), e -> !(e instanceof Player)).forEach(e -> e.discard());
            }
        }
    }

    private static void executeApply(java.util.List<ASTNode> params) throws Exception {
        if (params.isEmpty()) return;
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            String effectName = getStringValue(params.get(0));
            int duration = params.size() > 1 && params.get(1) != null ? getIntValue(params.get(1)) : 10;
            if (player instanceof ServerPlayer sp) ModScriptRegistry.applyEffect(sp, effectName, duration);
        }
    }

    private static void executeHeal(java.util.List<ASTNode> params) throws Exception {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && !params.isEmpty()) {
            float amount = (float) getDoubleValue(params.get(0));
            player.heal(amount);
        }
    }

    private static void executeSummon(java.util.List<ASTNode> params) throws Exception { executeSpawn(params); }

    private static void executeShoot(java.util.List<ASTNode> params) throws Exception {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            var arrow = new net.minecraft.world.entity.projectile.Arrow(player.level(), player, ItemStack.EMPTY, ItemStack.EMPTY);
            arrow.setPos(player.getX(), player.getEyeY(), player.getZ());
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F);
            player.level().addFreshEntity(arrow);
        }
    }

    private static void executeSet(java.util.List<ASTNode> params) throws Exception {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && params.size() >= 2) {
            String target = getStringValue(params.get(0));
            if (target.equals("on fire") && params.size() >= 4) {
                int seconds = getIntValue(params.get(3));
                player.setRemainingFireTicks(seconds * 20);
            }
        }
    }

    private static void executeDeal(java.util.List<ASTNode> params) throws Exception {
        if (params.isEmpty()) return;
        double amount = getDoubleValue(params.get(0));
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            var entities = player.level().getEntities(player, player.getBoundingBox().inflate(5), e -> !(e instanceof Player));
            for (var entity : entities) {
                entity.hurt(entity.damageSources().playerAttack((ServerPlayer) player), (float) amount);
            }
        }
    }

    private static void executePlay(java.util.List<ASTNode> params) throws Exception {
        if (params.isEmpty()) return;
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static int getIntValue(ASTNode node) {
        if (node instanceof ASTNode.NumberLiteral num) return (int) num.getValue();
        if (node instanceof ASTNode.StringLiteral str) { try { return Integer.parseInt(str.getValue()); } catch (Exception e) { return 1; } }
        return 1;
    }

    private static double getDoubleValue(ASTNode node) {
        if (node instanceof ASTNode.NumberLiteral num) return num.getValue();
        if (node instanceof ASTNode.StringLiteral str) { try { return Double.parseDouble(str.getValue()); } catch (Exception e) { return 0; } }
        return 0;
    }

    private static String getStringValue(ASTNode node) {
        if (node instanceof ASTNode.StringLiteral str) return str.getValue();
        if (node instanceof ASTNode.NumberLiteral num) return String.valueOf((int) num.getValue());
        return "";
    }
}
