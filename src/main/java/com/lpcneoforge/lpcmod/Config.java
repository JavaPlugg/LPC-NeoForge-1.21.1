package com.lpcneoforge.lpcmod;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<String> CHAT_FORMAT;
    public static final ModConfigSpec.ConfigValue<Integer> LOCAL_RADIUS;
    public static final ModConfigSpec.ConfigValue<Integer> DEATH_RADIUS;

    static {
        BUILDER.comment("""
                -----------------------------------
                - Placeholders:
                 %prefix%
                 %suffix%
                 %prefixes%
                 %suffixes%
                 %world%
                 %username%
                 %displayname%
                 %username-color%  | tip: add meta to user or group "meta.username-color.&<colorCode>"
                 %message-color%   | tip: add meta to user or group "meta.message-color.&<colorCode>"
                 %typemessage%     | tip: eather global or local
                 %chatmessage%
                -----------------------------------
                """);
        BUILDER.comment("""
                - Color Codes:
                 &0 - Black      | &1 - Dark Blue
                 &2 - Dark Green | &3 - Dark Aqua
                 &4 - Dark Red   | &5 - Dark Purple
                 &6 - Gold       | &7 - Gray
                 &8 - Dark Gray  | &9 - Blue
                 &a - Green      | &b - Aqua
                 &c - Red        | &d - Light Purple
                 &e - Yellow     | &f - White
                -----------------------------------
                 &k - Random Symbols | &l - Bold
                 &m - Strikethrough  | &n - Underline
                 &o - Italic         | &r - Reset
                 &g - Rainbow
                -----------------------------------
                 #RRGGBB - Hex Color Codes (e.g. #ff00ff)
                -----------------------------------
                ! in the begging of message to send GLOBAL message
                """);
        CHAT_FORMAT = BUILDER.comment("Chat format string with placeholders.")
                .define("chatFormat", "%typemessage%%prefix%%username%%suffix%: %chatmessage%");

        LOCAL_RADIUS = BUILDER.comment("Radius for local messages.")
                .define("localRadius", 300);

        DEATH_RADIUS = BUILDER.comment("Radius for death messages.")
                .define("deathRadius", 300);

        SPEC = BUILDER.build();
    }
}