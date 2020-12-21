package me.geek.tom.personalspace.mixins;

import me.geek.tom.personalspace.PersonalSpace;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class MixinPlayerManager {

    @Unique
    private final ThreadLocal<ServerPlayerEntity> playerJoining = new ThreadLocal<>();

    @Inject(method = "onPlayerConnect", at = @At("HEAD"))
    private void correctSpawnWorldHook_capturePlayer(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        playerJoining.set(player);
    }

    @Redirect(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorld(Lnet/minecraft/util/registry/RegistryKey;)Lnet/minecraft/server/world/ServerWorld;"))
    private @Nullable ServerWorld correctSpawnWorldHook_onPlayerConnect(MinecraftServer server, RegistryKey<World> key) {
        ServerWorld world = server.getWorld(key);

        if (world == null && playerJoining.get() != null) {
            world = PersonalSpace.correctPlayerWorldAndPosition(playerJoining.get(), server, key);
        }

        return world;
    }
}
