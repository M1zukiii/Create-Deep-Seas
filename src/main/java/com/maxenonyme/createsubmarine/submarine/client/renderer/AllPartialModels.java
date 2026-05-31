package com.maxenonyme.createsubmarine.submarine.client.renderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import com.maxenonyme.createsubmarine.CreateSubmarine;
import net.minecraft.resources.ResourceLocation;
public class AllPartialModels {
    public static final PartialModel BALLAST_WHEEL = PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateSubmarine.MOD_ID, "block/ballast_vent/gear"));
    public static final PartialModel ELECTROLYZER_GLASS = PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateSubmarine.MOD_ID, "block/electrolyzer_glass"));
    public static final PartialModel STEEL_CABLE = PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateSubmarine.MOD_ID, "block/steel_cable/steel_cable"));
    public static final PartialModel STEEL_CABLE_KNOT = PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateSubmarine.MOD_ID, "block/steel_cable/knot"));
    public static final PartialModel POULIS_CORE = PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateSubmarine.MOD_ID, "block/poulis_core"));
    public static void init() {}
}