package powercrystals.minefactoryreloaded.core;

import java.util.Map.Entry;

import buildcraft.api.transport.IPipeEntry;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.liquids.ILiquidTank;
import net.minecraftforge.liquids.ITankContainer;
import net.minecraftforge.liquids.LiquidContainerRegistry;
import net.minecraftforge.liquids.LiquidStack;
import powercrystals.core.inventory.IInventoryManager;
import powercrystals.core.inventory.InventoryManager;
import powercrystals.core.position.BlockPosition;
import powercrystals.core.util.UtilInventory;
import powercrystals.minefactoryreloaded.api.IToolHammer;
import powercrystals.minefactoryreloaded.api.IDeepStorageUnit;
import powercrystals.minefactoryreloaded.core.ITankContainerBucketable;
import powercrystals.minefactoryreloaded.tile.base.TileEntityFactory;

public class MFRUtil
{
	public static ItemStack consumeItem(ItemStack stack)
	{
		if(stack.stackSize == 1)
		{
			if(stack.getItem().hasContainerItem())
			{
				return stack.getItem().getContainerItemStack(stack);
			}
			else
			{
				return null;
			}
		}
		else
		{
			stack.splitStack(1);
			return stack;
		}	
	}
	
	public static boolean isHoldingWrench(EntityPlayer player)
	{
		if(player.inventory.getCurrentItem() == null)
		{
			return false;
		}
		Item currentItem = Item.itemsList[player.inventory.getCurrentItem().itemID];
		if(currentItem != null && currentItem instanceof IToolHammer)
		{
			return true;
		}
		return false;
	}
	
	public static boolean manuallyFillTank(ITankContainerBucketable itcb, EntityPlayer entityplayer)
	{
		ItemStack ci = entityplayer.inventory.getCurrentItem();
		LiquidStack liquid = LiquidContainerRegistry.getLiquidForFilledItem(ci);
		if(liquid != null)
		{
			if(itcb.fill(ForgeDirection.UNKNOWN, liquid, false) == liquid.amount)
			{
				itcb.fill(ForgeDirection.UNKNOWN, liquid, true);
				if(!entityplayer.capabilities.isCreativeMode)
				{
					entityplayer.inventory.setInventorySlotContents(entityplayer.inventory.currentItem, consumeItem(ci));					
				}
				return true;
			}
		}
		return false;
	}
	
	public static boolean manuallyDrainTank(ITankContainerBucketable itcb, EntityPlayer entityplayer)
	{
		ItemStack ci = entityplayer.inventory.getCurrentItem();
		if(LiquidContainerRegistry.isEmptyContainer(ci))
		{
			for(ILiquidTank tank : itcb.getTanks(ForgeDirection.UNKNOWN))
			{
				LiquidStack tankLiquid = tank.getLiquid();
				ItemStack filledBucket = LiquidContainerRegistry.fillLiquidContainer(tankLiquid, ci);
				if(LiquidContainerRegistry.isFilledContainer(filledBucket))
				{
					LiquidStack bucketLiquid = LiquidContainerRegistry.getLiquidForFilledItem(filledBucket);
					if(entityplayer.capabilities.isCreativeMode)
					{
						tank.drain(bucketLiquid.amount, true);
						return true;
					}
					else if(ci.stackSize == 1)
					{
						tank.drain(bucketLiquid.amount, true);
						entityplayer.inventory.setInventorySlotContents(entityplayer.inventory.currentItem, filledBucket);
						return true;
					}
					else if(entityplayer.inventory.addItemStackToInventory(filledBucket))
					{
						tank.drain(bucketLiquid.amount, true);
						ci.stackSize -= 1;
						return true;
					}
				}
					
			}
		}
		return false;
	}
	
	public static void pumpLiquid(ILiquidTank tank, TileEntityFactory from)
	{
		if(tank != null && tank.getLiquid() != null && tank.getLiquid().amount > 0)
		{
			LiquidStack l = tank.getLiquid().copy();
			l.amount = Math.min(l.amount, LiquidContainerRegistry.BUCKET_VOLUME);
			for(BlockPosition adj : new BlockPosition(from).getAdjacent(true))
			{
				TileEntity tile = from.worldObj.getBlockTileEntity(adj.x, adj.y, adj.z);
				if(tile != null && tile instanceof ITankContainer)
				{
					int filled = ((ITankContainer)tile).fill(adj.orientation.getOpposite(), l, true);
					tank.drain(filled, true);
					l.amount -= filled;
					if(l.amount <= 0)
					{
						break;
					}
				}
			}
		}
	}
	
	public static void mergeStacks(ItemStack to, ItemStack from)
	{
		if(to == null || from == null) return;
		
		if(to.itemID != from.itemID || to.getItemDamage() != from.getItemDamage()) return;
		if(to.getTagCompound() != null || from.getTagCompound() != null) return;
		
		int amountToCopy = Math.min(to.getMaxStackSize() - to.stackSize, from.stackSize);
		to.stackSize += amountToCopy;
		from.stackSize -= amountToCopy;
	}
	
	public static ItemStack dropStackDirected(TileEntityFactory from, ItemStack s, ForgeDirection towards)
	{
		if(s == null)
		{
			return null;
		}
		s = s.copy();
		BlockPosition bp = new BlockPosition(from.xCoord, from.yCoord, from.zCoord);
		bp.orientation = towards;
		bp.moveForwards(1);
		TileEntity te = from.worldObj.getBlockTileEntity(bp.x, bp.y, bp.z);
		if(te != null && te instanceof IInventory)
		{
			IInventoryManager manager = InventoryManager.create((IInventory)te, towards.getOpposite());
			s = manager.addItem(s);
		}
		else if(te != null && te instanceof IPipeEntry && ((IPipeEntry)te).acceptItems())
		{
			((IPipeEntry)te).entityEntering(s.copy(), towards);
			s.stackSize = 0;
		}
		
		if(s != null && s.stackSize > 0 && !from.worldObj.isBlockSolidOnSide(bp.x, bp.y, bp.z, towards.getOpposite()))
		{
			dropStackOnGround(s, BlockPosition.fromFactoryTile(from), from.worldObj, towards);
		}
		return s;
	}
	
	public static void dropStack(TileEntityFactory from, ItemStack s)
	{
		if(s == null)
		{
			return;
		}
		s = s.copy();
		if(from.worldObj.isRemote || s.stackSize <= 0)
		{
			return;
		}
		for(ForgeDirection o : UtilInventory.findPipes(from.worldObj, from.xCoord, from.yCoord, from.zCoord))
		{
			BlockPosition bp = new BlockPosition(from.xCoord, from.yCoord, from.zCoord);
			bp.orientation = o;
			bp.moveForwards(1);
			TileEntity te = from.worldObj.getBlockTileEntity(bp.x, bp.y, bp.z);
			if(te != null && te instanceof IPipeEntry && ((IPipeEntry)te).acceptItems())
			{
				((IPipeEntry)te).entityEntering(s, o);
				return;
			}
		}
		
		for(Entry<ForgeDirection, IInventory> chest : UtilInventory.findChests(from.worldObj, from.xCoord, from.yCoord, from.zCoord).entrySet())
		{
			if(chest.getValue().getInvName() == "Engine")
			{
				continue;
			}
			if(chest.getValue() instanceof IDeepStorageUnit)
			{
				IDeepStorageUnit idsu = (IDeepStorageUnit)chest.getValue();
				// don't put s in a DSU if it has NBT data
				if(s.getTagCompound() != null)
				{
					continue;
				}
				// don't put s in a DSU if the stored quantity is nonzero and it's mismatched with the stored item type
				if(idsu.getStoredItemType() != null && (idsu.getStoredItemType().itemID != s.itemID || idsu.getStoredItemType().getItemDamage() != s.getItemDamage()))
				{
					continue;
				}
			}
			IInventoryManager manager = InventoryManager.create((IInventory)chest.getValue(), chest.getKey().getOpposite());
			s = manager.addItem(s);
			if(s == null || s.stackSize == 0)
			{
				return;
			}
		}
		
		if(s != null && s.stackSize > 0)
		{
			dropStackOnGround(s, BlockPosition.fromFactoryTile(from), from.worldObj, from.getDropDirection());
		}
	}
	
	private static void dropStackOnGround(ItemStack stack, BlockPosition from, World world, ForgeDirection towards)
	{
		float dropOffsetX = 0.0F;
		float dropOffsetY = 0.0F;
		float dropOffsetZ = 0.0F;
		
		switch(towards)
		{
			case UNKNOWN:
			case UP:
				dropOffsetX = 0.5F;
				dropOffsetY = 1.5F;
				dropOffsetZ = 0.5F;
				break;
			case DOWN:
				dropOffsetX = 0.5F;
				dropOffsetY = -0.75F;
				dropOffsetZ = 0.5F;
				break;
			case NORTH:
				dropOffsetX = 0.5F;
				dropOffsetY = 0.5F;
				dropOffsetZ = -0.5F;
				break;
			case SOUTH:
				dropOffsetX = 0.5F;
				dropOffsetY = 0.5F;
				dropOffsetZ = 1.5F;
				break;
			case EAST:
				dropOffsetX = 1.5F;
				dropOffsetY = 0.5F;
				dropOffsetZ = 0.5F;
				break;
			case WEST:
				dropOffsetX = -0.5F;
				dropOffsetY = 0.5F;
				dropOffsetZ = 0.5F;
				break;
			default:
				break;
				
		}
		
		EntityItem entityitem = new EntityItem(world, from.x + dropOffsetX, from.y + dropOffsetY, from.z + dropOffsetZ, stack.copy());
		entityitem.motionX = 0.0D;
		if(towards != ForgeDirection.DOWN) entityitem.motionY = 0.3D;
		entityitem.motionZ = 0.0D;
		entityitem.delayBeforeCanPickup = 20;
		world.spawnEntityInWorld(entityitem);
		stack.stackSize = 0;
	}
}
