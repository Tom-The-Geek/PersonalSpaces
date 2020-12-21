package me.geek.tom.personalspace;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.dimension.DimensionOptions;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.PersistentWorldHandle;

import java.util.function.Consumer;

import static net.minecraft.server.command.CommandManager.literal;

public class PersonalSpace implements ModInitializer, EntityComponentInitializer {

    public static Logger LOGGER = LogManager.getLogger();

    public static final String MOD_ID = "personal-space";
    public static final String MOD_NAME = "PersonalSpace";

    private static final Identifier SPACE_DIMENSION_TYPE = new Identifier(MOD_ID, "player_space");
    private static final SimpleCommandExceptionType NOT_IN_SPACE = new SimpleCommandExceptionType(new LiteralText("You are not in a personal space!"));
    private static final SimpleCommandExceptionType ALREADY_IN_SPACE = new SimpleCommandExceptionType(new LiteralText("You are already in a personal space!"));

    private static final ComponentKey<ReturnInfoComponent> RETURN_INFO = ComponentRegistry.getOrCreate(
            new Identifier(MOD_ID, "return_info"), ReturnInfoComponent.class);

    public static ServerWorld correctPlayerWorldAndPosition(ServerPlayerEntity player, MinecraftServer server, RegistryKey<World> key) {
        if (!isSpaceWorld(key)) return null;
        RegistryKey<World> dim = RETURN_INFO.get(player).correctPlayer(player);
        if (dim == null) return null;
        return server.getWorld(dim);
    }

    @Override
    public void onInitialize() {
        log(Level.INFO, "Initializing");

        Registry.register(Registry.CHUNK_GENERATOR, SPACE_DIMENSION_TYPE, PersonalSpaceChunkGenerator.CODEC);

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
                dispatcher.register(literal("myspace")
                        .then(literal("enter").executes(PersonalSpace::enterSpace))
                        .then(literal("exit").executes(PersonalSpace::exitSpace))
        ));
    }

    private static int exitSpace(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (!isInSpace(player)) {
            throw NOT_IN_SPACE.create();
        }

        respawnPlayer(player);

        return 0;
    }

    private static void respawnPlayer(ServerPlayerEntity player) {
        RETURN_INFO.get(player).restoreToPlayer(player);
    }

    private static int enterSpace(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (isInSpace(player))
            throw ALREADY_IN_SPACE.create();

        ctx.getSource().sendFeedback(new LiteralText("Loading your personal space..."), false);
        getPersonalSpace(player, handle -> {
            RETURN_INFO.get(player).setReturn(player); // Allow the player to return.

            player.teleport(handle.asWorld(), 0, 13, 0, 0, 0);
            ctx.getSource().sendFeedback(new LiteralText("You joined your space!"), false);
        });

        return 0;
    }

    private static boolean isInSpace(ServerPlayerEntity player) {
        return isSpaceWorld(player.getEntityWorld().getRegistryKey());
    }

    private static boolean isSpaceWorld(RegistryKey<World> key) {
        return key.getValue().toString().startsWith(MOD_ID + ":space_");
    }

    private static void getPersonalSpace(ServerPlayerEntity player, Consumer<PersistentWorldHandle> handler) {
        Identifier id = new Identifier(MOD_ID, "space_" + player.getUuidAsString());
        MinecraftServer server = player.getServer();
        assert server != null;
        Fantasy.get(server).getOrOpenPersistentWorld(id, () -> new DimensionOptions(
                () -> server.getRegistryManager().getDimensionTypes().get(SPACE_DIMENSION_TYPE),
                new PersonalSpaceChunkGenerator(
                        () -> server.getRegistryManager().get(Registry.BIOME_KEY).get(BiomeKeys.PLAINS),
                        128
                )
        )).handleAsync((handle, t) -> {
            if (t != null) {
                player.getCommandSource().sendError(new LiteralText("Failed to open a personal space! An unexpected error occured!"));
                LOGGER.error("Failed to open personal space!", t);
            } else {
                handler.accept(handle);
            }

            return null;
        }, server);
    }

    public static void log(Level level, String message){
        LOGGER.log(level, "["+MOD_NAME+"] " + message);
    }

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(RETURN_INFO, p -> new ReturnInfoComponent());
    }
}
