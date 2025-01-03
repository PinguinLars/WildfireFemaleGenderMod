/*
 * Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
 * Copyright (C) 2023-present WildfireRomeo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.wildfire.mixins.accessors;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.registry.RegistryKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EquipmentRenderer.TrimSpriteKey.class)
@Environment(EnvType.CLIENT)
public interface TrimSpriteKeyConstructorAccessor {
    // While it would've been possible to also simply access widener the constructor along with the class, updating
    // such an access widener in the event that Mojang changes this in the future is _far_ more of a headache
    // than simply using a mixin.
    @Invoker("<init>")
    static EquipmentRenderer.TrimSpriteKey newKey(ArmorTrim armorTrim, EquipmentModel.LayerType layerType, RegistryKey<EquipmentAsset> registryKey) {
        throw new UnsupportedOperationException("Something's gone very seriously wrong if we've gotten here!");
    }
}
