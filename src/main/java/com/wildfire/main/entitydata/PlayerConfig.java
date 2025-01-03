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

package com.wildfire.main.entitydata;

import com.google.gson.JsonObject;
import com.wildfire.main.WildfireGender;
import com.wildfire.main.config.ConfigKey;
import com.wildfire.main.config.Configuration;
import com.wildfire.main.Gender;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * A version of {@link EntityConfig} backed by a {@link Configuration} for use with players
 */
public class PlayerConfig extends EntityConfig {

	public boolean needsSync;
	public boolean needsCloudSync;
	public SyncStatus syncStatus = SyncStatus.UNKNOWN;

	private final Configuration cfg;
	protected boolean hurtSounds = Configuration.HURT_SOUNDS.getDefault();
	protected boolean armorPhysOverride = Configuration.ARMOR_PHYSICS_OVERRIDE.getDefault();
	protected boolean showBreastsInArmor = Configuration.SHOW_IN_ARMOR.getDefault();

	/**
	 * @deprecated Use {@link #updateGender(Gender)} instead
	 */
	@Deprecated
	public PlayerConfig(UUID uuid, Gender gender) {
		this(uuid);
		updateGender(gender);
	}

	public PlayerConfig(UUID uuid) {
		super(uuid);
		this.cfg = new Configuration(this.uuid.toString());
		this.cfg.set(Configuration.USERNAME, this.uuid);
		this.cfg.setDefault(Configuration.GENDER);
		this.cfg.setDefault(Configuration.BUST_SIZE);
		this.cfg.setDefault(Configuration.HURT_SOUNDS);

		this.cfg.setDefault(Configuration.BREASTS_OFFSET_X);
		this.cfg.setDefault(Configuration.BREASTS_OFFSET_Y);
		this.cfg.setDefault(Configuration.BREASTS_OFFSET_Z);
		this.cfg.setDefault(Configuration.BREASTS_UNIBOOB);
		this.cfg.setDefault(Configuration.BREASTS_CLEAVAGE);

		this.cfg.setDefault(Configuration.BREAST_PHYSICS);
		this.cfg.setDefault(Configuration.ARMOR_PHYSICS_OVERRIDE);
		this.cfg.setDefault(Configuration.SHOW_IN_ARMOR);
		this.cfg.setDefault(Configuration.BOUNCE_MULTIPLIER);
		this.cfg.setDefault(Configuration.FLOPPY_MULTIPLIER);
		this.cfg.setDefault(Configuration.VOICE_PITCH);
	}

	// this shouldn't ever be called on players, but just to be safe, override with a noop.
	@Override
	public void readFromStack(@NotNull ItemStack chestplate) {}

	public Configuration getConfig() {
		return cfg;
	}

	private <VALUE> boolean updateValue(ConfigKey<VALUE> key, VALUE value, Consumer<VALUE> setter) {
		if (key.validate(value)) {
			setter.accept(value);
			return true;
		}
		return false;
	}

	public boolean updateGender(Gender value) {
		return updateValue(Configuration.GENDER, value, v -> this.gender = v);
	}

	public boolean updateBustSize(float value) {
		return updateValue(Configuration.BUST_SIZE, value, v -> this.pBustSize = v);
	}

	public boolean hasHurtSounds() {
		return hurtSounds;
	}

	public boolean updateVoicePitch(float value) {
		return updateValue(Configuration.VOICE_PITCH, value, v -> this.voicePitch = v);
	}

	public boolean updateHurtSounds(boolean value) {
		return updateValue(Configuration.HURT_SOUNDS, value, v -> this.hurtSounds = v);
	}

	public boolean updateBreastPhysics(boolean value) {
		return updateValue(Configuration.BREAST_PHYSICS, value, v -> this.breastPhysics = v);
	}

	public boolean getArmorPhysicsOverride() {
		return armorPhysOverride;
	}

	public boolean updateArmorPhysicsOverride(boolean value) {
		return updateValue(Configuration.ARMOR_PHYSICS_OVERRIDE, value, v -> this.armorPhysOverride = v);
	}

	public boolean showBreastsInArmor() {
		return showBreastsInArmor;
	}

	public boolean updateShowBreastsInArmor(boolean value) {
		return updateValue(Configuration.SHOW_IN_ARMOR, value, v -> this.showBreastsInArmor = v);
	}

	public boolean updateBounceMultiplier(float value) {
		return updateValue(Configuration.BOUNCE_MULTIPLIER, value, v -> this.bounceMultiplier = v);
	}

	public boolean updateFloppiness(float value) {
		return updateValue(Configuration.FLOPPY_MULTIPLIER, value, v -> this.floppyMultiplier = v);
	}

	public SyncStatus getSyncStatus() {
		return this.syncStatus;
	}

	/**
	 * @deprecated Use {@link #toJson()} instead
	 */
	@Deprecated
	public static JsonObject toJsonObject(PlayerConfig plr) {
		return plr.toJson();
	}

	public JsonObject toJson() {
		return cfg.SAVE_VALUES.deepCopy();
	}

	public boolean hasLocalConfig() {
		return cfg.exists();
	}

	public void loadFromDisk(boolean markForSync) {
		this.syncStatus = SyncStatus.CACHED;
		cfg.load();
		loadFromConfig(markForSync);
	}

	public void loadFromConfig(boolean markForSync) {
		updateGender(cfg.get(Configuration.GENDER));
		updateBustSize(cfg.get(Configuration.BUST_SIZE));
		updateHurtSounds(cfg.get(Configuration.HURT_SOUNDS));
		updateVoicePitch(cfg.get(Configuration.VOICE_PITCH));

		//physics
		updateBreastPhysics(cfg.get(Configuration.BREAST_PHYSICS));
		updateShowBreastsInArmor(cfg.get(Configuration.SHOW_IN_ARMOR));
		updateArmorPhysicsOverride(cfg.get(Configuration.ARMOR_PHYSICS_OVERRIDE));
		updateBounceMultiplier(cfg.get(Configuration.BOUNCE_MULTIPLIER));
		updateFloppiness(cfg.get(Configuration.FLOPPY_MULTIPLIER));

		breasts.updateXOffset(cfg.get(Configuration.BREASTS_OFFSET_X));
		breasts.updateYOffset(cfg.get(Configuration.BREASTS_OFFSET_Y));
		breasts.updateZOffset(cfg.get(Configuration.BREASTS_OFFSET_Z));
		breasts.updateUniboob(cfg.get(Configuration.BREASTS_UNIBOOB));
		breasts.updateCleavage(cfg.get(Configuration.BREASTS_CLEAVAGE));

		if(markForSync) {
			this.needsSync = true;
		}
	}

	/**
	 * @deprecated Use {@link #loadFromDisk(boolean)} instead
	 */
	@Deprecated
	public static PlayerConfig loadCachedPlayer(UUID uuid, boolean markForSync) {
		PlayerConfig plr = WildfireGender.getPlayerById(uuid);
		if (plr != null && plr.hasLocalConfig()) {
			plr.loadFromDisk(markForSync);
		}
		return plr;
	}

	public static void saveGenderInfo(PlayerConfig plr) {
		Configuration config = plr.getConfig();
		config.set(Configuration.USERNAME, plr.uuid);
		config.set(Configuration.GENDER, plr.getGender());
		config.set(Configuration.BUST_SIZE, plr.getBustSize());
		config.set(Configuration.HURT_SOUNDS, plr.hasHurtSounds());
		config.set(Configuration.VOICE_PITCH, plr.getVoicePitch());

		//physics
		config.set(Configuration.BREAST_PHYSICS, plr.hasBreastPhysics());
		config.set(Configuration.SHOW_IN_ARMOR, plr.showBreastsInArmor());
		config.set(Configuration.ARMOR_PHYSICS_OVERRIDE, plr.getArmorPhysicsOverride());
		config.set(Configuration.BOUNCE_MULTIPLIER, plr.getBounceMultiplier());
		config.set(Configuration.FLOPPY_MULTIPLIER, plr.getFloppiness());

		config.set(Configuration.BREASTS_OFFSET_X, plr.getBreasts().getXOffset());
		config.set(Configuration.BREASTS_OFFSET_Y, plr.getBreasts().getYOffset());
		config.set(Configuration.BREASTS_OFFSET_Z, plr.getBreasts().getZOffset());
		config.set(Configuration.BREASTS_UNIBOOB, plr.getBreasts().isUniboob());
		config.set(Configuration.BREASTS_CLEAVAGE, plr.getBreasts().getCleavage());

		config.save();
		plr.needsSync = true;
		plr.needsCloudSync = true;
	}

	@Override
	public boolean hasJacketLayer() {
		throw new UnsupportedOperationException("PlayerConfig does not support #hasJacketLayer(); use PlayerEntity#isPartVisible instead");
	}

	public void updateFromJson(JsonObject json) {
		json.asMap().forEach(this.cfg.SAVE_VALUES::add);
		loadFromConfig(false);
		this.syncStatus = SyncStatus.SYNCED;
	}

	public enum SyncStatus {
		CACHED, SYNCED, UNKNOWN
	}
}
