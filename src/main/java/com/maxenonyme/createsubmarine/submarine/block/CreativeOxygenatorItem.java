package com.maxenonyme.createsubmarine.submarine.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public class CreativeOxygenatorItem extends BlockItem {
    public CreativeOxygenatorItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (Screen.hasShiftDown()) {
            tooltipComponents.add(Component.empty());

            addTranslatableLines(tooltipComponents, "item.create_submarine.creative_oxygenator.tooltip.summary", ChatFormatting.LIGHT_PURPLE);
            addTranslatableLines(tooltipComponents, "item.create_submarine.creative_oxygenator.tooltip.behaviour1", ChatFormatting.LIGHT_PURPLE);
        } else {
            tooltipComponents.add(Component.translatable("create_submarine.tooltip.holdForInfo",
                Component.translatable("create_submarine.tooltip.keyShift").withStyle(ChatFormatting.GRAY))
                .withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    public static void addTranslatableLines(List<Component> tooltip, String key, ChatFormatting style) {
        String translated = Component.translatable(key).getString();
        for (String line : translated.split("\n")) {
            tooltip.add(Component.literal(line).withStyle(style));
        }
    }
}