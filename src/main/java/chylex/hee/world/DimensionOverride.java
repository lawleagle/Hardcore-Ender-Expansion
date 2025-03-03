package chylex.hee.world;
import java.lang.reflect.Field;
import java.util.Hashtable;

import chylex.hee.world.structure.sanctuary.ComponentSanctuary;
import chylex.hee.world.structure.sanctuary.StructureSanctuary;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import chylex.hee.world.biome.BiomeGenHardcoreEnd;
import chylex.hee.world.structure.island.ComponentIsland;
import chylex.hee.world.structure.island.StructureIsland;
import chylex.hee.world.structure.tower.ComponentTower;
import chylex.hee.world.structure.tower.StructureTower;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public final class DimensionOverride{
	public static void setup(){
		BiomeGenBase.getBiomeGenArray()[9] = null;
		BiomeGenBase.sky = new BiomeGenHardcoreEnd(9).setColor(8421631).setBiomeName("Sky").setDisableRain();
		BiomeGenBase.getBiomeGenArray()[9] = BiomeGenBase.sky;

		MapGenStructureIO.registerStructure(StructureTower.class,"hardcoreenderdragon_EndTower");
		MapGenStructureIO.func_143031_a(ComponentTower.class,"hardcoreenderdragon_EndTowerC"); // OBFUSCATED register structure component
		MapGenStructureIO.registerStructure(StructureIsland.class,"hardcoreenderdragon_EndIsland");
		MapGenStructureIO.func_143031_a(ComponentIsland.class,"hardcoreenderdragon_EndIslandC");
//		MapGenStructureIO.registerStructure(StructureSanctuary.class,"hee_EndSanctuary");
//		MapGenStructureIO.func_143031_a(ComponentSanctuary.class,"hee_EndSanctuaryC");

		if (BiomeGenHardcoreEnd.overrideWorldGen)MinecraftForge.EVENT_BUS.register(new DimensionOverride());
	}

	public static void postInit(){
		if (!(BiomeGenBase.sky instanceof BiomeGenHardcoreEnd))throw new RuntimeException("End biome class mismatch, Hardcore Ender Expansion cannot proceed! Biome class: "+BiomeGenBase.sky.getClass().getName());
		((BiomeGenHardcoreEnd)BiomeGenBase.getBiome(9)).overrideMobLists();

		DimensionManager.unregisterProviderType(1);
		DimensionManager.registerProviderType(1,WorldProviderHardcoreEnd.class,false);
	}

	public static void verifyIntegrity(){
		try{
			Field f = DimensionManager.class.getDeclaredField("providers");
			f.setAccessible(true); // let it throw NPE if the field isn't found

			Class<?> cls = ((Hashtable<Integer,Class<? extends WorldProvider>>)f.get(null)).get(1);
			if (cls != WorldProviderHardcoreEnd.class)throw new RuntimeException("End world provider class mismatch, Hardcore Ender Expansion cannot proceed! Provider class: "+(cls == null ? "<null>" : cls.getName()));
		}catch(NullPointerException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e){
			throw new RuntimeException("End world provider check failed!",e);
		}
	}

	private DimensionOverride(){}

	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load e){
		if (e.world.provider.dimensionId == 1 && e.world instanceof WorldServer){
			WorldServer world = (WorldServer)e.world;
			world.chunkProvider = world.theChunkProviderServer = new ChunkProviderServerOverride(world);
		}
	}

	public static final class ChunkProviderServerOverride extends ChunkProviderServer{
		public ChunkProviderServerOverride(WorldServer world){
			super(world,world.theChunkProviderServer.currentChunkLoader,world.theChunkProviderServer.currentChunkProvider);
		}

		@Override
		public void populate(IChunkProvider provider, int x, int z){
			Chunk chunk = provideChunk(x,z);

			if (!chunk.isTerrainPopulated){
				chunk.func_150809_p();

				if (currentChunkProvider != null){
					currentChunkProvider.populate(provider,x,z);
					chunk.setChunkModified();
				}
			}
		}
	}
}
