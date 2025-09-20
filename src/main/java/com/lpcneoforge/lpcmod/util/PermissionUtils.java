package com.lpcneoforge.lpcmod.util;

import net.luckperms.api.LuckPermsProvider;
import net.minecraft.world.entity.player.Player;

public class PermissionUtils {

    public static boolean checkPermission(Player player, String perm) {
        if (player == null) {
            return false;
        }
        if (player.hasPermissions(3) || player.hasPermissions(4)) {
            return true;
        }
        var user = LuckPermsProvider.get().getUserManager().getUser(player.getUUID());
        if (user == null) {
            try {
                user = LuckPermsProvider.get().getUserManager().loadUser(player.getUUID()).get();
            } catch (Exception e) {
                return false;
            }
        }
        return user.getCachedData().getPermissionData().checkPermission(perm).asBoolean();
    }
}
