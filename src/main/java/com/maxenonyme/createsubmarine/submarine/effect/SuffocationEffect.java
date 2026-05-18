package com.maxenonyme.createsubmarine.submarine.effect;
import com.maxenonyme.createsubmarine.CreateSubmarine;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
public class SuffocationEffect extends MobEffect {
    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> DAMAGE_KEY =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(CreateSubmarine.MOD_ID, "suffocation"));
    public SuffocationEffect() {
        super(MobEffectCategory.HARMFUL, 0x333333);
    }
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false));
        entity.hurt(new net.minecraft.world.damagesource.DamageSource(entity.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_KEY)), 1.0f + amplifier);
        return true;
    }
}
