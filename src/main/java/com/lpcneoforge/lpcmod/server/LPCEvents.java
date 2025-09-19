package com.lpcneoforge.lpcmod.server;

import com.lpcneoforge.lpcmod.Config;
import com.lpcneoforge.lpcmod.LPCNeoForge;
import com.mojang.datafixers.util.Pair;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.stream.Collectors;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import static com.lpcneoforge.lpcmod.server.ChatUtils.*;

public class LPCEvents {
    private static Pair<Boolean, BlockPos> isGlobal;
    public static Boolean onlyGlobalChat = false;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void PlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer deadPlayer)) {
            return;
        }
        MinecraftServer server = deadPlayer.getServer();
        if (server == null) {
            return;
        }
        LuckPerms luckPerms = LPCNeoForge.getLuckperms();
        if (luckPerms == null) {
            return;
        }
        for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            MutableComponent msg = MutableComponent.create(PlainTextContents.create(""));
            boolean canSpy = PermissionUtils.checkPermission(serverPlayer, "chat.spy");
            boolean closeEnough = serverPlayer.blockPosition().closerThan(deadPlayer.blockPosition(), Config.DEATH_RADIUS.get());
            if (!canSpy && !closeEnough) {
                continue;
            }
            if (canSpy && !closeEnough) {
                msg.append(ChatUtils.applyColorToLiteral("[Spy]", "&6"));
            }
            Entity killer = event.getSource().getEntity();
            if (killer == null) {
                continue;
            }
            Component killerName = killer.getName();
            Component deadPlayerName = deadPlayer.getName();
            msg.append("%s был убит %s".formatted(deadPlayerName, killerName));
            serverPlayer.sendSystemMessage(msg);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void ChatMessage(ServerChatEvent event) {
        ServerPlayer serverPlayer = event.getPlayer();
        Component originalMessage = event.getMessage();
        MutableComponent fullMessage = Component.literal("<" + serverPlayer.getName().getString() + "> ").append(originalMessage);


        Pair<Boolean, Component> pair = onServerChat(serverPlayer, fullMessage);
        if (pair != null) {
            if (pair.getFirst()) {
                MutableComponent newMessage = pair.getSecond().copy();
                if (fullMessage != newMessage) {
                    event.setCanceled(true);

                    serverPlayer.server.execute(() -> {
                        LPCNeoForge.LOGGER.info(newMessage.getString());
                        broadcastMessage(serverPlayer.level(), newMessage);
                    });
                }
            }
        }
    }

    public static Pair<Boolean, Component> onServerChat(ServerPlayer player, Component messageComponent) {
        if (LPCNeoForge.getLuckperms() != null) {
            final CachedMetaData metaData = LPCNeoForge.getLuckperms()
                    .getPlayerAdapter(ServerPlayer.class).getMetaData(player);

            String user = player.getName().getString();
            String message = messageComponent.getString();
            String prefix = metaData.getPrefix() != null ? metaData.getPrefix() : "";
            String suffix = metaData.getSuffix() != null ? metaData.getSuffix() : "";
            String prefixes = metaData.getPrefixes().keySet().stream().map(key -> metaData.getPrefixes().get(key)).collect(Collectors.joining());
            String suffixes = metaData.getSuffixes().keySet().stream().map(key -> metaData.getSuffixes().get(key)).collect(Collectors.joining());
            String world = player.level().toString();
            String displayName = player.getGameProfile().getName();
            String usernameColor = metaData.getMetaValue("username-color") != null ? metaData.getMetaValue("username-color") : "";
            String messageColor = metaData.getMetaValue("message-color") != null ? metaData.getMetaValue("message-color") : "";
            if (!message.contains(user)) {
                return null;
            }

            if (message.contains("> ")) {
                message = message.substring(message.split("> ")[0].length() + 2);
            }

            if (!onlyGlobalChat) {
                if (message.charAt(0) == '!') {
                    isGlobal = new Pair<>(true, null);
                    message = message.substring(1);
                } else {
                    isGlobal = new Pair<>(false, player.getOnPos());
                }
            }

            MutableComponent output = Component.literal("");
            String raw_outputstring = Config.CHAT_FORMAT.get();
            for (String word : raw_outputstring.split("%")) {
                String toAppend = word;
                MutableComponent wordComponent;

                if (word.equalsIgnoreCase("prefix")) {
                    toAppend = prefix;
                } else if (word.equalsIgnoreCase("suffix")) {
                    toAppend = suffix;
                } else if (word.equalsIgnoreCase("prefixes")) {
                    toAppend = prefixes;
                } else if (word.equalsIgnoreCase("suffixes")) {
                    toAppend = suffixes;
                } else if (word.equalsIgnoreCase("world")) {
                    toAppend = world;
                } else if (word.equalsIgnoreCase("username")) {
                    toAppend = user;
                    assert usernameColor != null;
                    wordComponent = applyColorToLiteral(toAppend, usernameColor);
                    output.append(wordComponent);
                    continue;
                } else if (word.equalsIgnoreCase("displayname")) {
                    toAppend = displayName;
                } else if (word.equalsIgnoreCase("username-color")) {
                    toAppend = usernameColor;
                } else if (word.equalsIgnoreCase("message-color")) {
                    toAppend = messageColor;
                } else if (word.equalsIgnoreCase("chatmessage")) {
                    toAppend = message;
                    assert messageColor != null;
                    wordComponent = applyColorToLiteral(toAppend, messageColor);
                    output.append(wordComponent);
                    continue;
                } else if (word.equalsIgnoreCase("typemessage")) {
                    if (onlyGlobalChat) toAppend = "";
                    else toAppend = (isGlobal.getFirst()) ? "[Г]" : "[Л]";
                }

                assert toAppend != null;
                MutableComponent wordcomponent = parseFormattedText(toAppend);
                output.append(wordcomponent);
            }

            return new Pair<>(true, output);
        }
        return null;
    }


    public static void broadcastMessage(Level world, MutableComponent message) {
        MinecraftServer server = world.getServer();
        if (server == null) {
            return;
        }

        QueryOptions emptyOptions = LPCNeoForge.getLuckperms().getContextManager().getStaticQueryOptions();
        Group group = LPCNeoForge.getLuckperms().getGroupManager().getGroup("WCViewer");

        for (Player player : server.getPlayerList().getPlayers()) {
            if (onlyGlobalChat) {
                sendMessage(player, message, false);
                continue;
            }

            User user = LPCNeoForge.getLuckperms().getUserManager().getUser(player.getUUID());
            boolean isWCViewer;

            if (user != null) isWCViewer = user.getInheritedGroups(emptyOptions).contains(group);
            else isWCViewer = false;

            if (!isGlobal.getFirst()) {
                double distance = Math.sqrt(player.getOnPos().distSqr(isGlobal.getSecond()));
                if (player.level() != world || distance > Config.LOCAL_RADIUS.get()) {
                    if (isWCViewer) {
                        sendMessage(player, Component.literal("§6[Spy]").append(message), false);
                    }
                    continue;
                }
            }
            sendMessage(player, message, false);
        }
    }

    public static void sendMessage(Player player, MutableComponent message, boolean emptyLine) {
        if (player.level().isClientSide) {
            return;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        if (emptyLine) {
            serverPlayer.sendSystemMessage(Component.literal(""));
        }

        serverPlayer.sendSystemMessage(message);
    }
}
