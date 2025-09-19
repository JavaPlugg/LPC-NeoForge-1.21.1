package com.lpcneoforge.lpcmod.commands;

import com.lpcneoforge.lpcmod.Config;
import com.lpcneoforge.lpcmod.LPCNeoForge;
import com.lpcneoforge.lpcmod.server.LPCEvents;
import com.lpcneoforge.lpcmod.server.PermissionUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import static com.lpcneoforge.lpcmod.server.ChatUtils.parseFormattedText;

public class AchatCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("achat")
                .then(Commands.literal("show")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(AchatCommand::handleShow)
                        )
                        .requires(source ->
                                PermissionUtils.checkPermission(source.getPlayer(), "command.achat.show"))
                )
                .then(Commands.literal("on")
                        .executes(AchatCommand::handleTypeChat)
                        .requires(source ->
                                PermissionUtils.checkPermission(source.getPlayer(), "command.achat.on"))
                )
                .then(Commands.literal("off")
                        .executes(AchatCommand::handleTypeChat)
                        .requires(source ->
                                PermissionUtils.checkPermission(source.getPlayer(), "command.achat.off"))
                )
                .then(Commands.literal("radius")
                        .then(Commands.literal("get")
                                .executes(AchatCommand::handleGetConfigRadius)
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer())
                                        .executes(AchatCommand::handleSetConfigRadius)
                                )
                        )
                        .requires(source ->
                                PermissionUtils.checkPermission(source.getPlayer(), "command.achat.radius"))
                )
                .then(Commands.literal("death")
                        .then(Commands.literal("get")
                                .executes(AchatCommand::handleGetConfigDeathRadius)
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer())
                                        .executes(AchatCommand::handleSetConfigDeathRadius)
                                )
                        )
                        .requires(source ->
                                PermissionUtils.checkPermission(source.getPlayer(), "command.achat.death"))
                )
        );
    }

    private static int handleShow(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Boolean value = BoolArgumentType.getBool(context, "value");

        if (LPCNeoForge.getLuckperms().getGroupManager().getGroup("WCViewer") == null) {
            LPCNeoForge.getLuckperms().getGroupManager().createAndLoadGroup("WCViewer").join();
        }

        User user = LPCNeoForge.getLuckperms().getUserManager().getUser(context.getSource().getPlayer().getUUID());

        if (value) {
            user.data().add(Node.builder("group.WCViewer").build());
            source.sendSuccess(() -> parseFormattedText("&l&3[LPC] &r&fТеперь вам видно все чаты"), false);
        } else {
            user.data().remove(Node.builder("group.WCViewer").build());
            source.sendSuccess(() ->
                    parseFormattedText(String.format("&l&3[LPC] &r&fТеперь вы видите локальный чат в радиусе %d блоков",
                            Config.LOCAL_RADIUS.get())), false);
        }

        LPCNeoForge.getLuckperms().getUserManager().saveUser(user);

        return 1;
    }

    private static int handleTypeChat(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String lastNode = context.getNodes().getLast().getNode().getName();
        if (lastNode.equals("on")) {
            LPCEvents.onlyGlobalChat = true;
            source.sendSuccess(() -> parseFormattedText("&l&3[LPC] &r&fВ процессе только глобальный чат"), false);
        } else {
            LPCEvents.onlyGlobalChat = false;
            source.sendSuccess(() -> parseFormattedText("&l&3[LPC] &r&fВ процессе и локальный, и глобальный чаты"), false);
        }
        return 1;
    }

    private static int handleGetConfigRadius(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Integer radius = Config.LOCAL_RADIUS.get();
        source.sendSuccess(() -> parseFormattedText(String.format("&l&3[LPC] &r&fРадиус локального чата: %d", radius)), false);
        return 1;
    }

    private static int handleSetConfigRadius(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Integer value = IntegerArgumentType.getInteger(context, "value");
        Config.LOCAL_RADIUS.set(value);
        source.sendSuccess(() -> parseFormattedText(String.format("&l&3[LPC] &r&fРадиус локального чата изменен на: %d", value)), false);
        return 1;
    }

    private static int handleGetConfigDeathRadius(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Integer deathRadius = Config.DEATH_RADIUS.get();
        source.sendSuccess(() -> parseFormattedText(String.format("&l&3[LPC] &r&fРадиус сообщений о смерти: %d", deathRadius)), false);
        return 1;
    }

    private static int handleSetConfigDeathRadius(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Integer value = IntegerArgumentType.getInteger(context, "value");
        Config.DEATH_RADIUS.set(value);
        source.sendSuccess(() -> parseFormattedText(String.format("&l&3[LPC] &r&fРадиус сообщений о смерти изменен на: %d", value)), false);
        return 1;
    }
}
