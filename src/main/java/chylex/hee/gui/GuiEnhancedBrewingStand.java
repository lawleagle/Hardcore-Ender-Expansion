package chylex.hee.gui;
import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import chylex.hee.tileentity.TileEntityEnhancedBrewingStand;

@SideOnly(Side.CLIENT)
public class GuiEnhancedBrewingStand extends GuiBrewingStand{
	private static ResourceLocation guiResource = new ResourceLocation("hardcoreenderexpansion:textures/gui/enhanced_brewing_stand.png");
	
	private TileEntityEnhancedBrewingStand brewingStand;
	 
	public GuiEnhancedBrewingStand(InventoryPlayer inv, TileEntityEnhancedBrewingStand tile){
		super(inv,tile);
		inventorySlots = new ContainerEnhancedBrewingStand(inv,tile);
		ySize = 191;
		brewingStand = tile;
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int x, int y){
		super.drawGuiContainerForegroundLayer(x,y);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		int powderReq = brewingStand.getField(1);
		
		fontRendererObj.drawStringWithShadow(
			(brewingStand.getHoldingPowder() < powderReq ? EnumChatFormatting.YELLOW : EnumChatFormatting.WHITE)+String.valueOf(powderReq),
		81,ySize-114,0x404040);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float renderPartialTicks, int x, int y){
		GL11.glColor4f(1F,1F,1F,1F);
		mc.getTextureManager().bindTexture(guiResource);
		int guiX = 1+(width-xSize)/2;
		int guiY = (height-ySize)/2;
		drawTexturedModalRect(guiX,guiY,0,0,xSize,ySize);
		int brewTime = brewingStand.getField(0);

		if (brewTime > 0){
			int texPos = (int)(28F*(1F-(float)brewTime/brewingStand.getField(2)));
			if (texPos > 0)drawTexturedModalRect(guiX+98,guiY+16,176,0,9,texPos);

			switch(brewTime/2%7){
				case 0: texPos = 29; break;
				case 1: texPos = 24; break;
				case 2: texPos = 20; break;
				case 3: texPos = 16; break;
				case 4: texPos = 11; break;
				case 5: texPos = 6; break;
				case 6: texPos = 0;
			}

			if (texPos > 0)drawTexturedModalRect(guiX+66,guiY+14+29-texPos,185,29-texPos,12,texPos);
		}
	}
}
