package me.geek.tom.personalspace;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.chunk.VerticalBlockSample;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

public class PersonalSpaceChunkGenerator extends ChunkGenerator {

    public static final Codec<PersonalSpaceChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Biome.REGISTRY_CODEC.stable().fieldOf("biome").forGetter(g -> g.biome),
                    Codec.INT.stable().fieldOf("radius").forGetter(g -> g.radius)
            ).apply(instance, instance.stable(PersonalSpaceChunkGenerator::new)));

    private final Supplier<Biome> biome;
    private final int radius;

    public PersonalSpaceChunkGenerator(Supplier<Biome> biome, int radius) {
        super(new FixedBiomeSource(biome), new StructuresConfig(Optional.empty(), Collections.emptyMap()));
        this.radius = radius;
        this.biome = biome;
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public ChunkGenerator withSeed(long seed) {
        return this;
    }

    @Override
    public void buildSurface(ChunkRegion region, Chunk chunk) {
        BlockPos base = chunk.getPos().getStartPos();
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int chunkX = 0; chunkX < 16; chunkX++) {
            int x = base.getX() + chunkX;
            cursor.setX(chunkX);
            for (int chunkZ = 0; chunkZ < 16; chunkZ++) {
                int z = base.getZ() + chunkZ;
                cursor.setZ(chunkZ);
                int height = getHeight(x, z);

                if (height == 0) continue;
                if (height == 10) {
                    cursor.setY(9);
                    chunk.setBlockState(cursor, Blocks.BEDROCK.getDefaultState(), false);
                }
                if (height == 256) {
                    for (int y = 0; y < 256; y++) {
                        cursor.setY(y);
                        if (y == 9) chunk.setBlockState(cursor, Blocks.BEDROCK.getDefaultState(), false);
                        else chunk.setBlockState(cursor, Blocks.BARRIER.getDefaultState(), false);
                    }
                }
            }
        }
    }

    @Override
    public void populateNoise(WorldAccess world, StructureAccessor structures, Chunk chunk) { }

    @Override
    public void generateFeatures(ChunkRegion region, StructureAccessor structures) { }

    @Override
    public void populateEntities(ChunkRegion region) { }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmapType) {
        return getHeight(x, z);
    }

    private int getHeight(int x, int z) {
        if (isWithinRadius(x, z)) {
            return 10;
        } else if (isOuterWall(x, z)) {
            return 255;
        } else {
            return 0;
        }
    }

    private boolean isWithinRadius(int x, int z) {
        return x * x + z * z <= radius * radius;
    }

    private boolean isOuterWall(int x, int z) {
        int distanceSq = x * x + z * z;
        return distanceSq > (radius * radius) && distanceSq <= (radius + 2) * (radius + 2);
    }

    @Override
    public BlockView getColumnSample(int x, int z) {
        BlockState[] states = new BlockState[256];
        states[9] = Blocks.BEDROCK.getDefaultState();
        if (isWithinRadius(x, z)) {
            for (int y = 0; y < 256; y++) {
                if (y == 9) continue;
                states[y] = Blocks.AIR.getDefaultState();
            }
        } else if (isOuterWall(x, z)) {
            for (int y = 0; y < 256; y++) {
                if (y == 9) continue;
                states[y] = Blocks.BARRIER.getDefaultState();
            }
        } else {
            for (int y = 0; y < 256; y++) {
                states[y] = Blocks.AIR.getDefaultState();
            }
        }
        return new VerticalBlockSample(states);
    }
}
