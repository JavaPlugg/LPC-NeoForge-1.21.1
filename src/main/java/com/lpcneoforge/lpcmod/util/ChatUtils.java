package com.lpcneoforge.lpcmod.util;

import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import net.kyori.adventure.text.minimessage.MiniMessage;

public final class ChatUtils {

    public static MutableComponent applyColorToLiteral(String text, String colorCode) {
        MutableComponent component = ChatUtils.parseFormattedText(text);

        if (colorCode.length() > 1 && colorCode.startsWith("&")) {
            ChatFormatting formatting = ChatUtils.getChatFormattingByCode(colorCode.charAt(1));
            if (formatting != null && formatting.getColor() != null) {
                return Component.literal(text).withStyle(style -> style.withColor(formatting.getColor()));
            }
        }

        if (colorCode.length() == 7 && colorCode.startsWith("#")) {
            try {
                int rgb = Integer.parseInt(colorCode.substring(1), 16);
                return Component.literal(text).withStyle(style -> style.withColor(TextColor.fromRgb(rgb)));
            } catch (NumberFormatException ignored) {
            }
        }

        return component;
    }

    public static MutableComponent parseFormattedText(String text) {
        MutableComponent component = Component.literal("");
        StringBuilder buffer = new StringBuilder();
        Style currentStyle = Style.EMPTY.withColor(ChatFormatting.WHITE);
        boolean rainbow = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '#' && i + 6 < text.length()) {
                String hexCode = text.substring(i, i + 7);
                try {
                    int rgb = Integer.parseInt(hexCode.substring(1), 16);
                    rainbow = flushBuffer(component, buffer, currentStyle, rainbow);
                    currentStyle = currentStyle.withColor(TextColor.fromRgb(rgb));
                    i += 6;
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }

            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                char formatCode = text.charAt(i + 1);
                ChatFormatting formatting = getChatFormattingByCode(formatCode);

                if (formatting != null) {
                    rainbow = flushBuffer(component, buffer, currentStyle, rainbow);

                    if (formatCode == 'g') {
                        rainbow = true;
                    } else if (formatting == ChatFormatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else if (formatting.isColor()) {
                        Integer color = formatting.getColor();
                        if (color != null) {
                            currentStyle = currentStyle.withColor(color);
                        }
                    } else {
                        currentStyle = currentStyle.applyFormat(formatting);
                    }

                    i++;
                    continue;
                }
            }

            buffer.append(c);
        }

        flushBuffer(component, buffer, currentStyle, rainbow);
        return component;
    }

    private static boolean flushBuffer(MutableComponent root, StringBuilder buffer, Style currentStyle, boolean rainbow) {
        if (buffer.isEmpty()) {
            return rainbow;
        }
        if (rainbow) {
            root.append(getRainbow(buffer.toString()));
        } else {
            root.append(Component.literal(buffer.toString()).withStyle(currentStyle));
        }
        buffer.setLength(0);
        return false;
    }


    private static Component getRainbow(String text) {
        var registryOps = RegistryAccess.EMPTY.createSerializationContext(JsonOps.INSTANCE);
        var cmp = MiniMessage.miniMessage().deserialize("<rainbow>" + text);
        var input = GsonComponentSerializer.gson().serializeToTree(cmp);
        return ComponentSerialization.CODEC.decode(registryOps, input).getOrThrow(IllegalArgumentException::new).getFirst();
    }

    private static ChatFormatting getChatFormattingByCode(char code) {
        ChatFormatting formatting = ChatFormatting.getByCode(code);
        if (formatting == null) {
            formatting = ChatFormatting.WHITE;
        }
        return formatting;
    }
}
