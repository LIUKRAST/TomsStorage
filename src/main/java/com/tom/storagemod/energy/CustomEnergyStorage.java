package com.tom.storagemod.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.energy.EnergyStorage;

/**
 * Copyright © 2019 by Lorenzo Magni
 * This file is part of OreCraft Utilities.
 * OreCraft Utilities is under "The 3-Clause BSD License", you can find a copy <a href="https://opensource.org/licenses/BSD-3-Clause">here</a>.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
public class CustomEnergyStorage extends EnergyStorage {
    public CustomEnergyStorage(int capacity) {
        super(capacity);
    }

    public CustomEnergyStorage(int capacity, int maxTransfer) {
        super(capacity, maxTransfer);
    }

    public CustomEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public CustomEnergyStorage(int capacity, int maxReceive, int maxExtract, int energy) {
        super(capacity, maxReceive, maxExtract, energy);
    }

    public void readFromNBT(CompoundTag compound) {
        capacity = compound.getInt("capacity");
        energy = compound.getInt("energy");
        maxReceive = compound.getInt("max_receive");
        maxExtract = compound.getInt("max_extract");

        if (energy > capacity) energy = capacity;
    }

    public CompoundTag writeToNBT(CompoundTag compound) {
        if (energy < 0) energy = 0;

        compound.putInt("capacity", capacity);
        compound.putInt("energy", energy);
        compound.putInt("max_receive", maxReceive);
        compound.putInt("max_extract", maxExtract);
        return compound;
    }

    public CustomEnergyStorage setCapacity(int capacity) {
        this.capacity = capacity;
        if (energy > capacity) energy = capacity;
        return this;
    }

    public CustomEnergyStorage setMaxReceive(int maxReceive) {
        this.maxReceive = maxReceive;
        return this;
    }

    public CustomEnergyStorage setMaxExtract(int maxExtract) {
        this.maxExtract = maxExtract;
        return this;
    }

    public CustomEnergyStorage setMaxTransfer(int maxTransfer) {
        setMaxReceive(maxTransfer);
        setMaxExtract(maxTransfer);
        return this;
    }

    public void setEnergyStored(int energy) {
        this.energy = energy;

        if (this.energy > capacity) this.energy = capacity;
        else if (this.energy < 0) this.energy = 0;
    }

    public void modifyEnergyStored(int energy) {
        this.energy += energy;

        if (this.energy > capacity) this.energy = capacity;
        else if (this.energy < 0) this.energy = 0;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int energyReceived = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
        if (!simulate) energy += energyReceived;
        return energyReceived;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int energyExtracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));
        if (!simulate) energy -= energyExtracted;
        return energyExtracted;
    }

    public int getMaxReceive() {
        return maxReceive;
    }

    public int getMaxExtract() {
        return maxExtract;
    }

    public void consume(int energy) {
        this.energy -= energy;

        if (this.energy > capacity) this.energy = capacity;
        else if (this.energy < 0) this.energy = 0;
    }
}
