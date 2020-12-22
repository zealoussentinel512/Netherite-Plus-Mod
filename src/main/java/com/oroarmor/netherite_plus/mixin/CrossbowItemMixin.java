package com.oroarmor.netherite_plus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.oroarmor.netherite_plus.config.NetheritePlusConfig;
import com.oroarmor.netherite_plus.item.NetheritePlusItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

	@Inject(method = "getArrow", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
	private static void createArrow(Level world, LivingEntity entity, ItemStack crossbow, ItemStack arrow, CallbackInfoReturnable<AbstractArrow> cir, ArrowItem arrowItem, AbstractArrow persistentProjectileEntity) {

		if (crossbow.getItem() != NetheritePlusItems.NETHERITE_CROSSBOW) {
			return;
		}

		persistentProjectileEntity.setBaseDamage(persistentProjectileEntity.getBaseDamage() * NetheritePlusConfig.DAMAGE.CROSSBOW_DAMAGE_MULTIPLIER.getValue() + NetheritePlusConfig.DAMAGE.CROSSBOW_DAMAGE_ADDITION.getValue());
		cir.setReturnValue(persistentProjectileEntity);
	}

}
