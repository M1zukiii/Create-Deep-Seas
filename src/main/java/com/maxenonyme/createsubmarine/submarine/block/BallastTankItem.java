package com.maxenonyme.createsubmarine.submarine.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public class BallastTankItem extends BlockItem {
    public BallastTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (Screen.hasShiftDown()) {
            tooltipComponents.add(Component.empty());

            int createYellow = 0xEBC255;
            int createOrange = 0xEE9246;

            String part1 = Component.translatable("item.create_submarine.ballast_tank.tooltip.summary_part1").getString();
            String part2 = Component.translatable("item.create_submarine.ballast_tank.tooltip.summary_part2").getString();

            String[] linesPart1 = part1.split("\n");
            for (int i = 0; i < linesPart1.length; i++) {
                Component lineComp = Component.literal(linesPart1[i]).withStyle(style -> style.withColor(createYellow));
                if (i == linesPart1.length - 1) {
                    lineComp = lineComp.copy().append(Component.translatable("block.create_submarine.ballast_vent")
                                       .withStyle(style -> style.withColor(createOrange)))
                                       .append(Component.literal(part2).withStyle(style -> style.withColor(createYellow)));
                }
                tooltipComponents.add(lineComp);
            }

            tooltipComponents.add(Component.empty());

            addTranslatableLines(tooltipComponents, "item.create_submarine.ballast_tank.tooltip.goggles", createYellow);
        } else {
            tooltipComponents.add(Component.translatable("create_submarine.tooltip.holdForInfo",
                Component.translatable("create_submarine.tooltip.keyShift").withStyle(ChatFormatting.GRAY))
                .withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    public static void addTranslatableLines(List<Component> tooltip, String key, int colorHex) {
        String translated = Component.translatable(key).getString();
        for (String line : translated.split("\n")) {
            tooltip.add(Component.literal(line).withStyle(style -> style.withColor(colorHex)));
        }
    }
}