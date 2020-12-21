package me.geek.tom.personalspace;

import dev.onyxstudios.cca.api.v3.entity.PlayerComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class ReturnInfoComponent implements PlayerComponent<ReturnInfoComponent> {

    private boolean hasReturnDimension = false;
    private RegistryKey<World> returnDimension = null;
    private double returnX = -1;
    private double returnY = -1;
    private double returnZ = -1;

    public void setReturn(ServerPlayerEntity player) {
        this.hasReturnDimension = true;
        this.returnDimension = player.getServerWorld().getRegistryKey();
        this.returnX = player.getX();
        this.returnY = player.getY();
        this.returnZ = player.getZ();
    }

    public void clear() {
        this.hasReturnDimension = false;
        this.returnDimension = null;
        this.returnX = -1;
        this.returnY = -1;
        this.returnZ = -1;
    }

    public RegistryKey<World> correctPlayer(ServerPlayerEntity player) {
        if (!hasReturnDimension) return null;
        player.setPos(this.returnX, this.returnY, this.returnZ);
        RegistryKey<World> dim = this.returnDimension;
        this.clear();
        return dim;
    }

    public void restoreToPlayer(ServerPlayerEntity player) {
        if (!this.hasReturnDimension) return;
        player.teleport(player.getServer().getWorld(this.returnDimension), this.returnX, this.returnY, this.returnZ, 0, 0);
        this.clear();
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        this.hasReturnDimension = tag.getBoolean("HasReturn");
        if (this.hasReturnDimension) {
            this.returnDimension = RegistryKey.of(Registry.DIMENSION, new Identifier(tag.getString("ReturnDimension")));
            this.returnX = tag.getDouble("ReturnX");
            this.returnY = tag.getDouble("ReturnY");
            this.returnZ = tag.getDouble("ReturnZ");
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.putBoolean("HasReturn", this.hasReturnDimension);
        if (this.hasReturnDimension) {
            tag.putString("ReturnDimension", this.returnDimension.getValue().toString());
            tag.putDouble("ReturnX", this.returnX);
            tag.putDouble("ReturnY", this.returnY);
            tag.putDouble("ReturnZ", this.returnZ);
        }
    }
}
