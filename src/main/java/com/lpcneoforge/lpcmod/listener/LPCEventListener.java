package com.lpcneoforge.lpcmod.listener;

import com.lpcneoforge.lpcmod.config.LPCConfig;
import com.lpcneoforge.lpcmod.LPCNeoForge;
import com.lpcneoforge.lpcmod.util.ChatUtils;
import com.lpcneoforge.lpcmod.util.PermissionUtils;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.stream.Collectors;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@Getter
@Setter
public class LPCEventListener {

    private final LPCConfig config;
    private final Map<String, Function<ServerPlayer, String>> partMaker;

    private boolean onlyGlobalChat = false;

    public LPCEventListener(LPCConfig config) {
        this.config = config;

        Function<ServerPlayer, CachedMetaData> metaDataExtractor = player -> LuckPermsProvider.get().getPlayerAdapter(ServerPlayer.class).getMetaData(player);

        this.partMaker = Map.ofEntries(
                Map.entry("prefix", metaDataExtractor.andThen(CachedMetaData::getPrefix)),
                Map.entry("suffix", metaDataExtractor.andThen(CachedMetaData::getSuffix)),
                Map.entry("prefixes", metaDataExtractor.andThen(metaData -> metaData
                        .getPrefixes()
                        .keySet()
                        .stream()
                        .map(key -> metaData.getPrefixes().get(key))
                        .collect(Collectors.joining())
                )),
                Map.entry("suffixes", metaDataExtractor.andThen(metaData -> metaData
                        .getSuffixes()
                        .keySet()
                        .stream()
                        .map(key -> metaData.getSuffixes().get(key))
                        .collect(Collectors.joining())
                )),
                Map.entry("world", player -> {
                    @SuppressWarnings("resource")
                    Level level = player.level();
                    return level.toString();
                }),
                Map.entry("username", player -> player.getName().getString()),
                Map.entry("displayname", player -> player.getGameProfile().getName()),
                Map.entry("username-color", metaDataExtractor.andThen(metaData -> metaData.getMetaValue("username-color"))),
                Map.entry("message-color", metaDataExtractor.andThen(metaData -> metaData.getMetaValue("message-color")))
        );
    }

    @SubscribeEvent
    public void playerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer deadPlayer)) {
            return;
        }
        MinecraftServer server = deadPlayer.getServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            MutableComponent msg = Component.literal("");
            boolean canSpy = PermissionUtils.checkPermission(serverPlayer, "chat.spy");
            boolean closeEnough = serverPlayer.position().closerThan(deadPlayer.position(), config.getDeathRadius().get());
            if (!canSpy && !closeEnough) {
                continue;
            }
            if (canSpy && !closeEnough) {
                msg.append(Component.literal("§6[Spy]"));
            }
            msg.append(getVanillaDeathMessage(deadPlayer));
            serverPlayer.sendSystemMessage(msg);
        }
    }

    private Component getVanillaDeathMessage(ServerPlayer player) {
        try {
            return player.getCombatTracker().getDeathMessage();
        } catch (Exception e) {
            DamageSource lastDamageSource = player.getLastDamageSource();
            return lastDamageSource != null ? lastDamageSource.getLocalizedDeathMessage(player) : Component.literal(player.getName().getString() + " умер");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void chatMessage(ServerChatEvent event) {
        event.setCanceled(true);

        ServerPlayer player = event.getPlayer();
        String message = event.getMessage().getString();

        boolean isGlobal = true;
        if (!onlyGlobalChat) {
            if (message.charAt(0) == '!') {
                message = message.substring(1);
            } else {
                isGlobal = false;
            }
        }

        if (message.trim().isEmpty()) {
            return;
        }

        MutableComponent output = Component.literal("");
        for (String word : config.getChatFormat().get().split("%")) {
            word = word.toLowerCase();
            String toAppend = word;

            Function<ServerPlayer, String> partFunction = partMaker.get(word);
            if (partFunction != null) {
                toAppend = partFunction.apply(player);
            }

            MutableComponent wordComponent;
            if (word.equals("username")) {
                wordComponent = ChatUtils.applyColorToLiteral(toAppend, nullToEmpty(partMaker.get("username-color").apply(player)));
                output.append(wordComponent);
                continue;
            }
            if (word.equals("chatmessage")) {
                toAppend = message;
                wordComponent = ChatUtils.applyColorToLiteral(toAppend, nullToEmpty(partMaker.get("message-color").apply(player)));
                output.append(wordComponent);
                continue;
            }
            if (word.equalsIgnoreCase("typemessage")) {
                toAppend = onlyGlobalChat ? "" : (isGlobal ? "[Г]" : "[Л]");
            }
            if (toAppend != null) {
                output.append(ChatUtils.parseFormattedText(toAppend));
            }
        }

        Vec3 senderPosition = isGlobal ? null : player.position();
        MutableComponent newMessage = output.copy();
        player.server.execute(() -> {
            LPCNeoForge.LOGGER.info(newMessage.getString());
            broadcastMessage(player.level(), newMessage, senderPosition);
        });
    }


    private void broadcastMessage(Level world, MutableComponent message, Vec3 senderPosition) {
        MinecraftServer server = world.getServer();
        if (server == null) {
            return;
        }

        LuckPerms luckperms = LuckPermsProvider.get();
        QueryOptions emptyOptions = luckperms.getContextManager().getStaticQueryOptions();
        Group group = luckperms.getGroupManager().getGroup("WCViewer");

        for (Player player : server.getPlayerList().getPlayers()) {
            if (onlyGlobalChat) {
                sendMessage(player, message);
                continue;
            }

            User user = luckperms.getUserManager().getUser(player.getUUID());
            boolean isWCViewer = false;
            if (user != null) {
                isWCViewer = user.getInheritedGroups(emptyOptions).contains(group);
            }

            boolean isGlobal = senderPosition == null;
            if (isGlobal) {
                sendMessage(player, message);
                continue;
            }

            @SuppressWarnings("resource")
            Level level = player.level();
            double maxDistance = config.getLocalRadius().get();
            double maxDistanceSqr = maxDistance * maxDistance;
            double distanceSqr = player.distanceToSqr(senderPosition);

            if (level == world && distanceSqr <= maxDistanceSqr) {
                sendMessage(player, message);
                continue;
            }

            if (isWCViewer) {
                sendMessage(player, Component.literal("§6[Spy]").append(message));
            }
        }
    }

    private void sendMessage(Player player, MutableComponent message) {
        @SuppressWarnings("resource")
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }
        ServerPlayer serverPlayer = (ServerPlayer) player;
        serverPlayer.sendSystemMessage(message);
    }

    private String nullToEmpty(String string) {
        return string != null ? string : "";
    }
}
