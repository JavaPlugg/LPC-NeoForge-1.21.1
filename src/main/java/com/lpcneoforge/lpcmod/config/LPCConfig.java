package com.lpcneoforge.lpcmod.config;

import lombok.Getter;
import net.neoforged.neoforge.common.ModConfigSpec;

@Getter
public class LPCConfig {

    private static final String PLACEHOLDERS = """
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
             %typemessage%     | tip: either global or local
             %chatmessage%
            -----------------------------------
            """;
    private static final String COLOR_CODES = """
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
            """;

    private final ModConfigSpec.ConfigValue<String> chatFormat;
    private final ModConfigSpec.ConfigValue<String> prefixLocal;
    private final ModConfigSpec.ConfigValue<String> prefixGlobal;
    private final ModConfigSpec.ConfigValue<String> prefixSpy;
    private final ModConfigSpec.ConfigValue<String> spyGroup;
    private final ModConfigSpec.ConfigValue<Integer> localRadius;
    private final ModConfigSpec.ConfigValue<Integer> deathRadius;
    private final ModConfigSpec spec;

    public LPCConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder()
                .comment(PLACEHOLDERS)
                .comment(COLOR_CODES);

        chatFormat = builder.comment("Chat format string with placeholders.")
                .define("chatFormat", "%typemessage%%prefix%%username%%suffix%: %chatmessage%");

        prefixLocal = builder.comment("Prefix for local chat messages")
                .define("prefixLocal", "[Л]");

        prefixGlobal = builder.comment("Prefix for global chat messages")
                .define("prefixGlobal", "[Г]");

        prefixSpy = builder.comment("Prefix for spying")
                .define("prefixSpy", "§6[Spy]");

        spyGroup = builder.comment("Group able to spy")
                .define("spyGroup", "WCViewer");

        localRadius = builder.comment("Radius for local messages.")
                .define("localRadius", 300);

        deathRadius = builder.comment("Radius for death messages.")
                .define("deathRadius", 300);

        spec = builder.build();
    }
}