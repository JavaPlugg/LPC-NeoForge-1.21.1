package com.lpcneoforge.lpcmod.command;

import com.lpcneoforge.lpcmod.config.LPCConfig;
import com.lpcneoforge.lpcmod.util.ChatUtils;
import com.lpcneoforge.lpcmod.listener.LPCEventListener;
import com.lpcneoforge.lpcmod.util.PermissionUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.ScopedNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class AchatCommand implements LPCCommand {

    private final LPCConfig config;
    private final LPCEventListener eventListener;

    public AchatCommand(LPCConfig config, LPCEventListener eventListener) {
        this.config = config;
        this.eventListener = eventListener;
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("achat")
                .then(Commands.literal("show")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(this::handleShow)
                        )
                        .requires(source -> PermissionUtils.checkPermission(source.getPlayer(), "command.achat.show"))
                )
                .then(Commands.literal("on")
                        .executes(this::handleTypeChat)
                        .requires(source -> PermissionUtils.checkPermission(source.getPlayer(), "command.achat.on"))
                )
                .then(Commands.literal("off")
                        .executes(this::handleTypeChat)
                        .requires(source -> PermissionUtils.checkPermission(source.getPlayer(), "command.achat.off"))
                )
                .then(Commands.literal("radius")
                        .then(Commands.literal("get")
                                .executes(this::handleGetConfigRadius)
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer())
                                        .executes(this::handleSetConfigRadius)
                                )
                        )
                        .requires(source -> PermissionUtils.checkPermission(source.getPlayer(), "command.achat.radius"))
                )
                .then(Commands.literal("death")
                        .then(Commands.literal("get")
                                .executes(this::handleGetConfigDeathRadius)
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer())
                                        .executes(this::handleSetConfigDeathRadius)
                                )
                        )
                        .requires(source -> PermissionUtils.checkPermission(source.getPlayer(), "command.achat.death"))
                )
        );
    }

    private int handleShow(CommandContext<CommandSourceStack> context) {
        GroupManager groupManager = LuckPermsProvider.get().getGroupManager();
        if (groupManager.getGroup("WCViewer") == null) {
            groupManager.createAndLoadGroup("WCViewer").join();
        }

        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }

        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUUID());
        if (user == null) {
            return 0;
        }

        boolean allChatsVisible = BoolArgumentType.getBool(context, "value");
        CommandSourceStack source = context.getSource();
        ScopedNode<?, ?> mcViewerGroup = Node.builder("group.WCViewer").build();

        if (allChatsVisible) {
            user.data().add(mcViewerGroup);
            source.sendSuccess(() ->
                    ChatUtils.parseFormattedText("&l&3[LPC] &r&fТеперь вам видно все чаты"), false
            );
        } else {
            user.data().remove(mcViewerGroup);
            source.sendSuccess(() ->
                    ChatUtils.parseFormattedText(String.format(
                            "&l&3[LPC] &r&fТеперь вы видите локальный чат в радиусе %d блоков",
                            config.getLocalRadius().get()
                    )), false
            );
        }

        LuckPermsProvider.get().getUserManager().saveUser(user);

        return Command.SINGLE_SUCCESS;
    }

    private int handleTypeChat(CommandContext<CommandSourceStack> context) {
        String lastNode = context.getNodes().getLast().getNode().getName();
        boolean isOn = lastNode.equals("on");

        eventListener.setOnlyGlobalChat(isOn);
        String formattedText = isOn ? "&l&3[LPC] &r&fВ процессе только глобальный чат" : "&l&3[LPC] &r&fВ процессе и локальный, и глобальный чаты";

        context.getSource().sendSuccess(() ->
                ChatUtils.parseFormattedText(formattedText), false
        );
        return Command.SINGLE_SUCCESS;
    }

    private int handleGetConfigRadius(CommandContext<CommandSourceStack> context) {
        int localRadius = config.getLocalRadius().get();
        context.getSource().sendSuccess(() ->
                ChatUtils.parseFormattedText(String.format("&l&3[LPC] &r&fРадиус локального чата: %d", localRadius)), false
        );
        return Command.SINGLE_SUCCESS;
    }

    private int handleSetConfigRadius(CommandContext<CommandSourceStack> context) {
        int localRadius = IntegerArgumentType.getInteger(context, "value");
        config.getLocalRadius().set(localRadius);
        context.getSource().sendSuccess(() ->
                ChatUtils.parseFormattedText(String.format("&l&3[LPC] &r&fРадиус локального чата изменен на: %d", localRadius)), false
        );
        return Command.SINGLE_SUCCESS;
    }

    private int handleGetConfigDeathRadius(CommandContext<CommandSourceStack> context) {
        int deathRadius = config.getDeathRadius().get();
        context.getSource().sendSuccess(() ->
                ChatUtils.parseFormattedText(String.format("&l&3[LPC] &r&fРадиус сообщений о смерти: %d", deathRadius)), false
        );
        return Command.SINGLE_SUCCESS;
    }

    private int handleSetConfigDeathRadius(CommandContext<CommandSourceStack> context) {
        int deathRadius = IntegerArgumentType.getInteger(context, "value");
        config.getDeathRadius().set(deathRadius);
        context.getSource().sendSuccess(() ->
                ChatUtils.parseFormattedText(String.format("&l&3[LPC] &r&fРадиус сообщений о смерти изменен на: %d", deathRadius)), false
        );
        return Command.SINGLE_SUCCESS;
    }
}
