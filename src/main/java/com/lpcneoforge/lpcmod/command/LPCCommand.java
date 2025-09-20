package com.lpcneoforge.lpcmod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public interface LPCCommand {

    void register(CommandDispatcher<CommandSourceStack> dispatcher);
}
