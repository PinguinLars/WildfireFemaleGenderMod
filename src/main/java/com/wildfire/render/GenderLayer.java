/*
    Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
    Copyright (C) 2023 WildfireRomeo

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.wildfire.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wildfire.api.IGenderArmor;
import com.wildfire.main.entitydata.Breasts;
import com.wildfire.main.WildfireHelper;
import com.wildfire.main.config.GeneralClientConfig;
import com.wildfire.main.entitydata.EntityConfig;
import com.wildfire.physics.BreastPhysics;
import com.wildfire.render.WildfireModelRenderer.BreastModelBox;
import com.wildfire.render.WildfireModelRenderer.OverlayModelBox;
import com.wildfire.render.WildfireModelRenderer.PositionTextureVertex;
import java.util.ConcurrentModificationException;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.wildfire.main.entitydata.PlayerConfig;
import com.wildfire.main.WildfireGender;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.*;

import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.ClientHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class GenderLayer<ENTITY extends LivingEntity, MODEL extends HumanoidModel<ENTITY>> extends RenderLayer<ENTITY, MODEL> {

	private final TextureAtlas armorTrimAtlas;

	private BreastModelBox lBreast, rBreast;
	private final OverlayModelBox lBreastWear, rBreastWear;
	private final BreastModelBox lBoobArmor, rBoobArmor;

	private float preBreastSize = 0f;

	public GenderLayer(RenderLayerParent<ENTITY, MODEL> renderer, ModelManager modelManager) {
		super(renderer);

		armorTrimAtlas = modelManager.getAtlas(Sheets.ARMOR_TRIMS_SHEET);

		lBreast = new BreastModelBox(64, 64, 16, 17, -4F, 0.0F, 0F, 4, 5, 4, 0.0F, false);
		rBreast = new BreastModelBox(64, 64, 20, 17, 0, 0.0F, 0F, 4, 5, 4, 0.0F, false);
		lBreastWear = new OverlayModelBox(true,64, 64, 17, 34, -4F, 0.0F, 0F, 4, 5, 3, 0.0F, false);
		rBreastWear = new OverlayModelBox(false,64, 64, 21, 34, 0, 0.0F, 0F, 4, 5, 3, 0.0F, false);

		lBoobArmor = new BreastModelBox(64, 32, 16, 17, -4F, 0.0F, 0F, 4, 5, 3, 0.0F, false);
		rBoobArmor = new BreastModelBox(64, 32, 20, 17, 0, 0.0F, 0F, 4, 5, 3, 0.0F, false);
	}

	//Copy of Forge's patched in HumanoidArmorLayer#getArmorResource but with the removal of the string to rl map lookup
	public ResourceLocation getArmorResource(Entity entity, ItemStack stack, EquipmentSlot slot, @Nullable String type) {
		ArmorItem item = (ArmorItem) stack.getItem();
		String texture = item.getMaterial().getName();
		String domain = "minecraft";
		int idx = texture.indexOf(':');
		if (idx != -1) {
			domain = texture.substring(0, idx);
			texture = texture.substring(idx + 1);
		}
		String s1 = String.format(Locale.ROOT, "%s:textures/models/armor/%s_layer_%d%s.png", domain, texture,
			(slot == EquipmentSlot.LEGS ? 2 : 1), type == null ? "" : String.format(Locale.ROOT, "_%s", type));

		s1 = ClientHooks.getArmorTexture(entity, stack, s1, slot, type);
		return new ResourceLocation(s1);
	}

	@Nullable
	protected EntityConfig getConfig(ENTITY entity) {
		try {
			return EntityConfig.getEntity(entity);
		} catch(ConcurrentModificationException e) {
			// likely a temporary failure, try again later
			return null;
		}
	}

	@Override
	public void render(@NotNull PoseStack matrixStack, @NotNull MultiBufferSource bufferSource, int packedLightIn, @NotNull ENTITY entity, float limbAngle,
		float limbDistance, float partialTicks, float animationProgress, float headYaw, float headPitch) {
		if (GeneralClientConfig.INSTANCE.disableRendering.get() || entity.isSpectator()) {
			//Rendering is disabled client side, or the entity is in spectator so only the head will be rendered
			return;
		}
		//Surround with a try/catch to fix for essential mod.
		try {
            EntityConfig entityConfig = getConfig(entity);
			if(entityConfig == null) return;

			ItemStack armorStack = entity.getItemBySlot(EquipmentSlot.CHEST);
			//Note: When the stack is empty the helper will fall back to an implementation that returns the proper data
			IGenderArmor genderArmor = WildfireHelper.getArmorConfig(armorStack);
			boolean isChestplateOccupied = genderArmor.coversBreasts();
			if (genderArmor.alwaysHidesBreasts() || !entityConfig.showBreastsInArmor() && isChestplateOccupied) {
				//If the armor always hides breasts or there is armor and the player configured breasts
				// to be hidden when wearing armor, we can just exit early rather than doing any calculations
				return;
			}
            RenderType breastRenderType = null;
            ResourceLocation entityTexture = getBreastTexture(entity);
            if (entityTexture != null) {
                //RenderType selection copied from LivingEntityRenderer#getRenderType
                boolean bodyVisible = !entity.isInvisible();
                Minecraft minecraft = Minecraft.getInstance();
                boolean translucent = !bodyVisible && minecraft.player != null && !entity.isInvisibleTo(minecraft.player);
                if (translucent) {
                    breastRenderType = RenderType.itemEntityTranslucentCull(entityTexture);
                } else if (bodyVisible) {
                    breastRenderType = RenderType.entityTranslucent(entityTexture);
                } else if (minecraft.shouldEntityAppearGlowing(entity)) {
                    breastRenderType = RenderType.outline(entityTexture);
                } else {
                    if (!isChestplateOccupied) {
                        //Exit early if we don't need to render the breasts, and we don't need to render the armor
                        return;
                    }
                }
            } else if (!isChestplateOccupied) {
                //Exit early if we don't need to render the breasts, and we don't need to render the armor
                return;
            }

			Breasts breasts = entityConfig.getBreasts();
			float breastOffsetX = Math.round((Math.round(breasts.getXOffset() * 100f) / 100f) * 10) / 10f;
			float breastOffsetY = -Math.round((Math.round(breasts.getYOffset() * 100f) / 100f) * 10) / 10f;
			float breastOffsetZ = -Math.round((Math.round(breasts.getZOffset() * 100f) / 100f) * 10) / 10f;

			BreastPhysics leftBreastPhysics = entityConfig.getLeftBreastPhysics();
			final float bSize = leftBreastPhysics.getBreastSize(partialTicks);
			float outwardAngle = (Math.round(breasts.getCleavage() * 100f) / 100f) * 100f;
			outwardAngle = Math.min(outwardAngle, 10);


			float reducer = 0;
			if (bSize < 0.84f) reducer++;
			if (bSize < 0.72f) reducer++;

			if (preBreastSize != bSize) {
				lBreast = new BreastModelBox(64, 64, 16, 17, -4F, 0.0F, 0F, 4, 5, (int) (4 - breastOffsetZ - reducer), 0.0F, false);
				rBreast = new BreastModelBox(64, 64, 20, 17, 0, 0.0F, 0F, 4, 5, (int) (4 - breastOffsetZ - reducer), 0.0F, false);
				preBreastSize = bSize;
			}

			//Note: We only render if the entity is not visible to the player, so we can assume it is visible to the player
			float overlayAlpha = entity.isInvisible() ? 0.15F : 1;

			RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

			float lTotal = Mth.lerp(partialTicks, leftBreastPhysics.getPrePositionY(), leftBreastPhysics.getPositionY());
			float lTotalX = Mth.lerp(partialTicks, leftBreastPhysics.getPrePositionX(), leftBreastPhysics.getPositionX());
			float leftBounceRotation = Mth.lerp(partialTicks, leftBreastPhysics.getPreBounceRotation(), leftBreastPhysics.getBounceRotation());
			float rTotal;
			float rTotalX;
			float rightBounceRotation;
			if (breasts.isUniboob()) {
				rTotal = lTotal;
				rTotalX = lTotalX;
				rightBounceRotation = leftBounceRotation;
			} else {
				BreastPhysics rightBreastPhysics = entityConfig.getRightBreastPhysics();
				rTotal = Mth.lerp(partialTicks, rightBreastPhysics.getPrePositionY(), rightBreastPhysics.getPositionY());
				rTotalX = Mth.lerp(partialTicks, rightBreastPhysics.getPrePositionX(), rightBreastPhysics.getPositionX());
				rightBounceRotation = Mth.lerp(partialTicks, rightBreastPhysics.getPreBounceRotation(), rightBreastPhysics.getBounceRotation());
			}
			float breastSize = bSize * 1.5f;
			if (breastSize > 0.7f) breastSize = 0.7f;
			if (bSize > 0.7f) {
				breastSize = bSize;
			}

			if (breastSize < 0.02f) return;

			float zOff = 0.0625f - (bSize * 0.0625f);
			breastSize = bSize + 0.5f * Math.abs(bSize - 0.7f) * 2f;

			//If the armor physics is overridden ignore resistance
			float resistance = entityConfig.getArmorPhysicsOverride() ? 0 : Mth.clamp(genderArmor.physicsResistance(), 0, 1);
			//Note: We only check if the breathing animation should be enabled if the chestplate's physics resistance
			// is less than or equal to 0.5 so that if we won't be rendering it we can avoid doing extra calculations
			boolean breathingAnimation = entityConfig.canBreathe() && resistance <= 0.5F &&
                                         (!entity.isUnderWater() || MobEffectUtil.hasWaterBreathing(entity) ||
										  entity.level().getBlockState(BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ())).is(Blocks.BUBBLE_COLUMN));
			boolean bounceEnabled = entityConfig.hasBreastPhysics() && (!isChestplateOccupied || resistance < 1); //oh, you found this?

			int combineTex = LivingEntityRenderer.getOverlayCoords(entity, 0);
			HumanoidModel<ENTITY> model = getParentModel();
			boolean hasJacketLayer = entity instanceof Player player ? player.isModelPartShown(PlayerModelPart.JACKET) : entityConfig.hasJacketLayer();
			renderBreastWithTransforms(entity, model, armorStack, matrixStack, bufferSource, breastRenderType, packedLightIn, combineTex, overlayAlpha, bounceEnabled,
				lTotalX, lTotal, leftBounceRotation, breastSize, breastOffsetX, breastOffsetY, breastOffsetZ, zOff, outwardAngle, breasts.isUniboob(),
				isChestplateOccupied, breathingAnimation, true, hasJacketLayer);
			renderBreastWithTransforms(entity, model, armorStack, matrixStack, bufferSource, breastRenderType, packedLightIn, combineTex, overlayAlpha, bounceEnabled,
				rTotalX, rTotal, rightBounceRotation, breastSize, -breastOffsetX, breastOffsetY, breastOffsetZ, zOff, -outwardAngle, breasts.isUniboob(),
				isChestplateOccupied, breathingAnimation, false, hasJacketLayer);
			RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		} catch(Exception e) {
			WildfireGender.LOGGER.error("Failed to render gender layer", e);
		}
	}

	private void renderBreastWithTransforms(ENTITY entity, HumanoidModel<ENTITY> model, ItemStack armorStack, PoseStack matrixStack, MultiBufferSource bufferSource,
		@Nullable RenderType breastRenderType, int packedLightIn, int combineTex, float alpha, boolean bounceEnabled, float totalX, float total, float bounceRotation,
		float breastSize, float breastOffsetX, float breastOffsetY, float breastOffsetZ, float zOff, float outwardAngle, boolean uniboob, boolean isChestplateOccupied,
		boolean breathingAnimation, boolean left, boolean hasJacketLayer) {
		matrixStack.pushPose();
		//Surround with a try/catch to fix for essential mod.
		try {
			if (entity.isBaby()) {
				float f1 = 1.0F / model.babyBodyScale;
				matrixStack.scale(f1, f1, f1);
				matrixStack.translate(0.0D, model.bodyYOffset / 16.0F, 0.0D);
			}
			ModelPart body = model.body;
			matrixStack.translate(body.x * 0.0625f, body.y * 0.0625f, body.z * 0.0625f);
			if (body.zRot != 0.0F) {
				matrixStack.mulPose(new Quaternionf().rotationXYZ(0f, 0f, body.zRot));
			}
			if (body.yRot != 0.0F) {
				matrixStack.mulPose(new Quaternionf().rotationXYZ(0f, body.yRot, 0f));
			}
			if (body.xRot != 0.0F) {
				matrixStack.mulPose(new Quaternionf().rotationXYZ(body.xRot, 0f, 0f));
			}

			if (bounceEnabled) {
				matrixStack.translate(totalX / 32f, 0, 0);
				matrixStack.translate(0, total / 32f, 0);
			}

			matrixStack.translate(breastOffsetX * 0.0625f, 0.05625f + (breastOffsetY * 0.0625f), zOff - 0.0625f * 2f + (breastOffsetZ * 0.0625f)); //shift down to correct position

			if (!uniboob) {
				matrixStack.translate(-0.0625f * 2 * (left ? 1 : -1), 0, 0);
			}
			if (bounceEnabled) {
				matrixStack.mulPose(new Quaternionf().rotationXYZ(0, (float) (bounceRotation * (Math.PI / 180f)), 0));
			}
			if (!uniboob) {
				matrixStack.translate(0.0625f * 2 * (left ? 1 : -1), 0, 0);
			}

			float rotationMultiplier = 0;
			if (bounceEnabled) {
				matrixStack.translate(0, -0.035f * breastSize, 0); //shift down to correct position
				rotationMultiplier = -total / 12f;
			}
			float totalRotation = breastSize + rotationMultiplier;
			if (!bounceEnabled) {
				totalRotation = breastSize;
			}
			if (totalRotation > breastSize + 0.2F) {
				totalRotation = breastSize + 0.2F;
			}
			totalRotation = Math.min(totalRotation, 1); //hard limit for MAX

			if (isChestplateOccupied) {
				matrixStack.translate(0, 0, 0.01f);
			}

			matrixStack.mulPose(new Quaternionf().rotationXYZ(0, (float) (outwardAngle * (Math.PI / 180f)), 0));
			matrixStack.mulPose(new Quaternionf().rotationXYZ((float) (-35f * totalRotation * (Math.PI / 180f)), 0, 0));

			if (breathingAnimation) {
				float f5 = -Mth.cos(entity.tickCount * 0.09F) * 0.45F + 0.45F;
				matrixStack.mulPose(new Quaternionf().rotationXYZ((float) (f5 * (Math.PI / 180f)), 0, 0));
			}

			matrixStack.scale(0.9995f, 1f, 1f); //z-fighting FIXXX

			renderBreast(entity, armorStack, matrixStack, bufferSource, breastRenderType, packedLightIn, combineTex, alpha, left, hasJacketLayer);
		} catch(Exception e) {
			WildfireGender.LOGGER.error("Failed to render breast", e);
		}
		matrixStack.popPose();
	}

	@Nullable
	private ResourceLocation getBreastTexture(ENTITY entity) {
		return entity instanceof AbstractClientPlayer player ? player.getSkin().texture() : null;
	}

	private void shiftForJacket(PoseStack matrixStack) {
		matrixStack.translate(0, 0, -0.015f);
		matrixStack.scale(1.05f, 1.05f, 1.05f);
	}

	private void renderBreast(ENTITY entity, ItemStack armorStack, PoseStack matrixStack, MultiBufferSource bufferSource,
		@Nullable RenderType breastRenderType, int packedLightIn, int packedOverlayIn, float alpha, boolean left, boolean hasJacketLayer) {
		if (breastRenderType != null) {
			//Only render the breasts if we have a render type for them
			VertexConsumer vertexConsumer = bufferSource.getBuffer(breastRenderType);
			renderBox(left ? lBreast : rBreast, matrixStack, vertexConsumer, packedLightIn, packedOverlayIn, 1F, 1F, 1F, alpha);
			if (hasJacketLayer) {
				shiftForJacket(matrixStack);
				renderBox(left ? lBreastWear : rBreastWear, matrixStack, vertexConsumer, packedLightIn, packedOverlayIn, 1F, 1F, 1F, alpha);
			}
		} else if (hasJacketLayer) {//Copy exact size
			shiftForJacket(matrixStack);
		}
		//TODO: Eventually we may want to expose a way via the API for mods to be able to override rendering
		// be it because they are not an armor item or the way they render their armor item is custom
		//Render Breast Armor
		if (!armorStack.isEmpty() && armorStack.getItem() instanceof ArmorItem armorItem) {
			ResourceLocation armorTexture = getArmorResource(entity, armorStack, EquipmentSlot.CHEST, null);
			ResourceLocation overlayTexture = null;
			float armorR = 1f;
			float armorG = 1f;
			float armorB = 1f;
			if (armorItem instanceof DyeableLeatherItem dyeableItem) {
				overlayTexture = getArmorResource(entity, armorStack, EquipmentSlot.CHEST, "overlay");
				int color = dyeableItem.getColor(armorStack);
				armorR = (float) (color >> 16 & 255) / 255.0F;
				armorG = (float) (color >> 8 & 255) / 255.0F;
				armorB = (float) (color & 255) / 255.0F;
			}
			matrixStack.pushPose();
			matrixStack.translate(left ? 0.001f : -0.001f, 0.015f, -0.015f);
			matrixStack.scale(1.05f, 1, 1);
			WildfireModelRenderer.BreastModelBox armor = left ? lBoobArmor : rBoobArmor;
			RenderType armorType = RenderType.armorCutoutNoCull(armorTexture);
			VertexConsumer armorVertexConsumer = bufferSource.getBuffer(armorType);
			renderBox(armor, matrixStack, armorVertexConsumer, packedLightIn, OverlayTexture.NO_OVERLAY, armorR, armorG, armorB, 1);
			if (overlayTexture != null) {
				RenderType overlayType = RenderType.armorCutoutNoCull(overlayTexture);
				VertexConsumer overlayVertexConsumer = bufferSource.getBuffer(overlayType);
				renderBox(armor, matrixStack, overlayVertexConsumer, packedLightIn, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
			}
			ArmorTrim.getTrim(entity.level().registryAccess(), armorStack, true).ifPresent(trim -> {
				ArmorMaterial armorMaterial = armorItem.getMaterial();
				TextureAtlasSprite sprite = this.armorTrimAtlas.getSprite(trim.outerTexture(armorMaterial));
				VertexConsumer trimVertexConsumer = sprite.wrap(bufferSource.getBuffer(Sheets.armorTrimsSheet(trim.pattern().value().decal())));
				renderBox(armor, matrixStack, trimVertexConsumer, packedLightIn, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
			});

			if (armorStack.hasFoil()) {
				renderBox(armor, matrixStack, bufferSource.getBuffer(RenderType.armorEntityGlint()), packedLightIn, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
			}

			matrixStack.popPose();
		}
	}

	private static void renderBox(WildfireModelRenderer.ModelBox model, PoseStack matrixStack, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn,
		float red, float green, float blue, float alpha) {
		Matrix4f matrix4f = matrixStack.last().pose();
		Matrix3f matrix3f =	matrixStack.last().normal();
		for (WildfireModelRenderer.TexturedQuad quad : model.quads) {
			Vector3f vector3f = new Vector3f(quad.normal.getX(), quad.normal.getY(), quad.normal.getZ());
			vector3f.mul(matrix3f);
			for (PositionTextureVertex vertex : quad.vertexPositions) {
				bufferIn.vertex(matrix4f, vertex.x() / 16.0F, vertex.y() / 16.0F, vertex.z() / 16.0F)
					.color(red, green, blue, alpha)
					.uv(vertex.texturePositionX(), vertex.texturePositionY())
					.overlayCoords(packedOverlayIn)
					.uv2(packedLightIn)
					.normal(vector3f.x(), vector3f.y(), vector3f.z())
					.endVertex();
			}
		}
	}
}
