package chylex.hee.mechanics.essence.handler;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.world.World;
import chylex.hee.api.interfaces.IAcceptFieryEssence;
import chylex.hee.mechanics.enhancements.types.EssenceAltarEnhancements;
import chylex.hee.packets.PacketPipeline;
import chylex.hee.packets.client.C11ParticleAltarOrb;
import chylex.hee.system.util.MathUtil;
import chylex.hee.tileentity.TileEntityEssenceAltar;

public class FieryEssenceHandler extends AltarActionHandler{
	private byte essenceUsageCounter;
	
	public FieryEssenceHandler(TileEntityEssenceAltar altar){
		super(altar);
	}
	
	@Override
	public void onUpdate(){
		List<Enum> enhancementList = altar.getEnhancements();
		
		World world = altar.getWorldObj();
		int level = altar.getEssenceLevel();
		int n = 35+Math.min(60,level>>3);
		boolean drained = false;
		
		int range = enhancementList.contains(EssenceAltarEnhancements.RANGE) ? 16 : 12;
		n += (range-12)*2;
		
		boolean hasSpeedEnh = enhancementList.contains(EssenceAltarEnhancements.SPEED);
		if (hasSpeedEnh)n = MathUtil.ceil(n*1.75D);
		
		for(int a = 0, xx, yy, zz; a < n; a++){
			xx = altar.xCoord+world.rand.nextInt(1+range)-(range>>1);
			yy = altar.yCoord+world.rand.nextInt(5)-2;
			zz = altar.zCoord+world.rand.nextInt(1+range)-(range>>1);
			
			Block block = altar.getWorldObj().getBlock(xx,yy,zz);
			TileEntity tile = altar.getWorldObj().getTileEntity(xx,yy,zz);
			drained = false;
			
			if (block == Blocks.lit_furnace || tile instanceof TileEntityFurnace){
				TileEntityFurnace furnace = (TileEntityFurnace)tile;
				
				if (furnace != null && furnace.isBurning() && canFurnaceSmelt(furnace)){
					n = 1+Math.min(8,level>>6);
					if (hasSpeedEnh)n = MathUtil.ceil(n*1.75D);
					
					for(int b = 0; b < n; b++){
						if (furnace.furnaceCookTime < 199){
							++furnace.furnaceCookTime;
							
							if (tryDrainEssence()){
								drained = true;
								if (--level <= 0)break;
							}
						}
						else break;
					}
					
					if (drained && world.rand.nextInt(6+(n>>1)) <= 4)createOrbParticle(xx,yy,zz);
					return;
				}
			}
			else if (block == Blocks.brewing_stand){
				TileEntityBrewingStand stand = (TileEntityBrewingStand)altar.getWorldObj().getTileEntity(xx,yy,zz);
				
				if (stand != null && stand.getBrewTime() > 1 && stand.getBrewTime() != 400){
					n = 1+Math.min(5,level>>6);
					if (hasSpeedEnh)n = MathUtil.ceil(n*1.75D);
					
					for(int b = 0; b < n; b++){
						stand.updateEntity();
						
						if (tryDrainEssence()){
							drained = true;
							if (--level <= 0)break;
						}
						
						if (stand.getBrewTime() <= 1)break;
					}
				}
			}
			else if (tile instanceof IAcceptFieryEssence){
				IAcceptFieryEssence acceptor = (IAcceptFieryEssence)tile;
				n = acceptor.getBoostAmount(level);
				// TODO boost speed?
				
				for(int b = 0; b < n; b++){
					acceptor.boost();
					
					if (tryDrainEssence()){
						drained = true;
						if (--level <= 0)break;
					}
				}
			}
		}
	}
	
	private boolean tryDrainEssence(){
		if (++essenceUsageCounter > (altar.getEnhancements().contains(EssenceAltarEnhancements.EFFICIENCY) ? 80 : 60)){
			essenceUsageCounter = 0;
			altar.drainEssence(1);
			return true;
		}
		
		return false;
	}
	
	private void createOrbParticle(int targetX, int targetY, int targetZ){
		PacketPipeline.sendToAllAround(altar,64D,new C11ParticleAltarOrb(altar,targetX+0.5D,targetY+0.5D,targetZ+0.5D));
	}
	
	@Override
	public void onTileWriteToNBT(NBTTagCompound nbt){
		nbt.setByte("F_essenceUsageCnt",essenceUsageCounter);
	}
	
	@Override
	public void onTileReadFromNBT(NBTTagCompound nbt){
		essenceUsageCounter = nbt.getByte("F_essenceUsageCnt");
	}
	
	// TileEntityFurnace.canSmelt()

	private boolean canFurnaceSmelt(TileEntityFurnace furnace){
		if (furnace.getStackInSlot(0) == null)return false;
		
		ItemStack itemstack = FurnaceRecipes.smelting().getSmeltingResult(furnace.getStackInSlot(0));
		if (itemstack == null)return false;
		
		ItemStack input = furnace.getStackInSlot(2);
		if (input == null)return true;
		if (!input.isItemEqual(itemstack))return false;
		int result = input.stackSize+itemstack.stackSize;
		return result <= furnace.getInventoryStackLimit() && result <= input.getMaxStackSize();
	}
}
