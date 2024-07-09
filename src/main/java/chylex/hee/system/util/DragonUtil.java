package chylex.hee.system.util;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Pattern;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.apache.commons.lang3.ArrayUtils;
import chylex.hee.system.logging.Stopwatch;

public final class DragonUtil{
	public static int portalEffectX, portalEffectZ;

	public static final DecimalFormat formatOnePlace = new DecimalFormat("0.0");
	public static final DecimalFormat formatTwoPlaces = new DecimalFormat("0.00");

	private static final Pattern regexChatFormatting = Pattern.compile("(?i)"+String.valueOf('\u00a7')+"[0-9A-FK-OR]");

	private static final double[] rayX = new double[]{ -1D, -1D, 1D, 1D },
								  rayZ = new double[]{ -1D, 1D, -1D, 1D };

	public static boolean canEntitySeePoint(EntityLivingBase entity, double x, double y, double z, double pointScale){
		return canEntitySeePoint(entity,x,y,z,pointScale,false);
	}

	public static boolean canEntitySeePoint(EntityLivingBase entity, double x, double y, double z, double pointScale, boolean fromCenter){
		Stopwatch.timeAverage("DragonUtil - canEntitySeePoint",800);

		double px = entity.posX,
			   py = entity.posY,
			   pz = entity.posZ,
			   ry = -entity.rotationYaw,
			   rp = -entity.rotationPitch;

		int t1x, t1z, t2x, t2z, t3y, t4y;

		if (!fromCenter){
			px -= MathUtil.lendirx(1.5D,ry);
			pz -= MathUtil.lendiry(1.5D,ry);
		}

		t1x = (int)(px+MathUtil.lendirx(600,ry-66));
		t1z = (int)(pz+MathUtil.lendiry(600,ry-66));
		t2x = (int)(px+MathUtil.lendirx(600,ry+66));
		t2z = (int)(pz+MathUtil.lendiry(600,ry+66));

		boolean isXZInYaw = MathUtil.triangle((int)x,(int)z,(int)px,(int)pz,t1x,t1z,t2x,t2z);
		boolean isYInPitch;

		if ((Math.abs(rp) < 70 && Math.abs(y - py) <= 2) || Math.abs(y - py) > 2)isYInPitch = true;
		else{
			t3y = (int)(py+MathUtil.lendirx(28,rp-60));
			t4y = (int)(py+MathUtil.lendirx(28,rp+60));
			isYInPitch = (y >= t3y && y <= t4y);
		}

		boolean[] isBlockedArr = new boolean[8];

		for(int a = 0; a < 4; a++){
			for(int b = 0; b < 2; b++){
				MovingObjectPosition mop = entity.worldObj.rayTraceBlocks(Vec3.createVectorHelper(px,py+0.12F,pz),Vec3.createVectorHelper(x+rayX[a]*pointScale,y+(b == 0?-1D:1D)*pointScale,z+rayZ[a]*pointScale));

				if (mop != null && mop.typeOfHit == MovingObjectType.BLOCK){
					isBlockedArr[a*2+b] = MathUtil.distance(mop.blockX+0.5D-x,mop.blockY+0.5D-y,mop.blockZ+0.5D-z) > 0.1D &&
										  MathUtil.distance(mop.blockX+0.5D-px,mop.blockY+0.62D-py,mop.blockZ-pz) > 2D;

					if (isBlockedArr[a*2+b]){
						if (!entity.worldObj.getBlock(mop.blockX,mop.blockY,mop.blockZ).isOpaqueCube())isBlockedArr[a*2+b] = false;
					}
				}
			}
		}

		boolean isBlocked = isBlockedArr[0] && isBlockedArr[1] && isBlockedArr[2] && isBlockedArr[3] && isBlockedArr[4] && isBlockedArr[5] && isBlockedArr[6] && isBlockedArr[7];

		Stopwatch.finish("DragonUtil - canEntitySeePoint");

		if (isBlocked)return false;
		else return (isXZInYaw && isYInPitch);
	}

	public static float rotateSmoothly(float sourceAngle, float targetAngle, float amount){
		while(sourceAngle < 0F)sourceAngle += 360F;
		while(sourceAngle >= 360F)sourceAngle -= 360F;

		float angleDifference = 180F-Math.abs(Math.abs(sourceAngle-targetAngle)-180F);
		if (angleDifference <= amount)return targetAngle;

		float d = sourceAngle < 180F ? sourceAngle+180F : sourceAngle-180F;
		sourceAngle += amount*((targetAngle > d && targetAngle < sourceAngle) || (sourceAngle < 180F && (targetAngle > d || targetAngle < sourceAngle)) ? -1 : 1);

		return sourceAngle;
	}

	/**
	 * Returns the closest entity to the source. It never returns the source entity, even if it is inside the provided list.
	 */
	public static <T extends Entity> T getClosestEntity(Entity source, List<? extends T> list){
		double closestDist = Double.MAX_VALUE, currentDist;
		T closestEntity = null;

		for(T entity:list){
			if (!entity.isDead && entity != source && (currentDist = source.getDistanceSqToEntity(entity)) < closestDist){
				closestDist = currentDist;
				closestEntity = entity;
			}
		}

		return closestEntity;
	}


	/**
	 * Returns the sorted list of entities based by distance to the source. It never returns the source entity, even if it is inside the provided list.
	 */
	public static <T extends Entity> List<T> getClosestEntities(int maxAmount, Entity source, List<? extends T> list){
		List<T> closestEntities = new ArrayList<>();
		Map<T,Float> entities = new HashMap<>();

		if (maxAmount <= 0)return closestEntities;

		for(T entity:list){
			if (entity.isDead || entity == source)continue;
			entities.put(entity,source.getDistanceToEntity(entity));
		}

		for(Entry<T,Float> entry:CollectionUtil.sortMapByValueAsc(entities)){
			closestEntities.add(entry.getKey());
			if (--maxAmount <= 0)break;
		}

		return closestEntities;
	}

	public static double[] getNormalizedVector(double vecX, double vecZ){
		double len = Math.sqrt(vecX*vecX+vecZ*vecZ);
		return len == 0 ? new double[]{ 0, 0 } : new double[]{ vecX/len, vecZ/len };
	}

	public static Vec3 getRandomVector(Random rand){
		return Vec3.createVectorHelper(rand.nextDouble()-0.5D,rand.nextDouble()-0.5D,rand.nextDouble()-0.5D).normalize();
	}

	public static <T> T[] getNonNullValues(T[] array){
		if (array.length == 0)return array;

		int nonNull = 0, cnt = 0;
		for(T t:array){
			if (t != null)++nonNull;
		}

		T[] newArray = (T[])Array.newInstance(array.getClass().getComponentType(),nonNull);
		for(T t:array){
			if (t != null)newArray[cnt++] = t;
		}

		return newArray;
	}

	public static int getTopBlockY(World world, Block block, int x, int z, int startY){
		for(BlockPosM pos = new BlockPosM(x,startY,z); pos.y >= 0; pos.y--){
			if (pos.getBlock(world) == block)return pos.y;
		}

		return -1;
	}

	public static int getTopBlockY(World world, Block block, int x, int z){
		return getTopBlockY(world,block,x,z,255);
	}

	public static void spawnXP(Entity entity, int amount){
		for(int a = amount; a > 0;){
			int split = EntityXPOrb.getXPSplit(a);
			a -= split;
			entity.worldObj.spawnEntityInWorld(new EntityXPOrb(entity.worldObj,entity.posX,entity.posY,entity.posZ,a));
		}
	}

	public static void teleportToOverworld(EntityPlayerMP player){
		player.mcServer.getConfigurationManager().transferPlayerToDimension(player,0);
		player.playerNetServerHandler.playerEntity = player.mcServer.getConfigurationManager().respawnPlayer(player,0,true);
	}

	public static void createMobExplosion(Entity entity, double x, double y, double z, float strength, boolean fire){
		entity.worldObj.newExplosion(entity,x,y,z,strength,fire,entity.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing"));
	}

	public static void createMobExplosion(World world, double x, double y, double z, float strength, boolean fire){
		world.newExplosion(null,x,y,z,strength,fire,world.getGameRules().getGameRuleBooleanValue("mobGriefing"));
	}

	public static String stripChatFormatting(String str){
		return regexChatFormatting.matcher(str).replaceAll("");
	}

	public static int tryParse(String str, int def){
		try{
			return Integer.parseInt(str);
		}catch(NumberFormatException e){
			return def;
		}
	}

	public static int getDayDifference(Calendar cal1, Calendar cal2){
		return MathUtil.floor((double)Math.abs(cal1.getTimeInMillis()-cal2.getTimeInMillis())/(1000*60*60*24));
	}

	public static boolean canAddOneItemTo(ItemStack is, ItemStack itemToAdd){
		return is.isStackable() && is.getItem() == itemToAdd.getItem() &&
			   (!is.getHasSubtypes() || is.getItemDamage() == itemToAdd.getItemDamage()) &&
			   ItemStack.areItemStackTagsEqual(is,itemToAdd) &&
			   is.stackSize+1 <= is.getMaxStackSize();
	}

	public static boolean checkSystemProperty(String key){
		String str = System.getProperty("hee");
		return str != null && ArrayUtils.contains(str.split(","),key);
	}

	private DragonUtil(){}
}
