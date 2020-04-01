package com.nisovin.magicjutsus.materials;

import org.bukkit.material.MaterialData;

public class MagicBlockAnyDataMaterial extends MagicBlockMaterial {

	public MagicBlockAnyDataMaterial(MaterialData data) {
		super(data);
	}
	
	@Override
	public boolean equals(MaterialData data) {
		return data.getItemType() == this.data.getItemType();
	}

}
