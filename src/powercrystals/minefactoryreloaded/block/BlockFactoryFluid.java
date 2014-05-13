package powercrystals.minefactoryreloaded.block;

import cofh.pcc.random.WeightedRandomItemStack;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.IIcon;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import powercrystals.minefactoryreloaded.MFRRegistry;
import powercrystals.minefactoryreloaded.MineFactoryReloadedCore;
import powercrystals.minefactoryreloaded.api.rednet.IRedNetNoConnection;
import powercrystals.minefactoryreloaded.setup.MFRConfig;

public class BlockFactoryFluid extends BlockFluidClassic implements IRedNetNoConnection
{ // TODO: convert to BlockFluidFinite
	private IIcon _iconFlowing;
	private IIcon _iconStill;
	protected String fluidName;
	private static Fluid ensureFluid(String name)
	{
		Fluid fluid = FluidRegistry.getFluid(name);
		if (fluid.canBePlacedInWorld())
			ReflectionHelper.setPrivateValue(Fluid.class, fluid, null, "block");
		return fluid;
	}

	public BlockFactoryFluid(String liquidName)
	{
		super(ensureFluid(liquidName), Material.water);
		setBlockName("mfr.liquid." + liquidName + ".still");
		setHardness(100.0F);
		setLightOpacity(3);
		fluidName = liquidName;
	}

	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity)
	{
		super.onEntityCollidedWithBlock(world, x, y, z, entity);

		if(entity instanceof EntityLivingBase)
		{
			NBTTagCompound data = entity.getEntityData();
			if (world.isRemote | data.getLong("mfr:fluidTimer" + fluidName) > world.getTotalWorldTime())
			{
				return;
			}
			data.setLong("mfr:fluidTimer" + fluidName, world.getTotalWorldTime() + 40);
			
			EntityLivingBase ent = (EntityLivingBase)entity;
			if (this == MineFactoryReloadedCore.milkLiquid)
			{
				ent.addPotionEffect(new PotionEffect(Potion.digSpeed.id, 6 * 20, 0));
			}
			else if (this == MineFactoryReloadedCore.sludgeLiquid)
			{
				ent.addPotionEffect(new PotionEffect(Potion.wither.id, 12 * 20, 0));
				ent.addPotionEffect(new PotionEffect(Potion.weakness.id, 12 * 20, 0));
				ent.addPotionEffect(new PotionEffect(Potion.confusion.id, 12 * 20, 0));
			}
			else if (this == MineFactoryReloadedCore.sewageLiquid)
			{
				ent.addPotionEffect(new PotionEffect(Potion.hunger.id, 12 * 20, 0));
				ent.addPotionEffect(new PotionEffect(Potion.poison.id, 12 * 20, 0));
				ent.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 12 * 20, 0));
			}
			else if (this == MineFactoryReloadedCore.essenceLiquid)
			{
				ent.addPotionEffect(new PotionEffect(Potion.nightVision.id, 60 * 20, 0));
			}
			else if (this == MineFactoryReloadedCore.biofuelLiquid)
			{
				ent.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 12 * 20, 0));
			}
		}
	}

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z)
    {
    	int light = super.getLightValue(world, x, y, z); 
        if (maxScaledLight != 0)
            light = Math.max(light, 2);
        return light;
    }

	@Override
	public void updateTick(World world, int x, int y, int z, Random rand)
	{
		/*BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
		l: if (biome != null && biome.getFloatTemperature() > 1.9f)//*/
		l: if (world.provider.isHellWorld)
		{
			if (!isSourceBlock(world, x, y, z))
			{
				if (world.setBlockToAir(x, y, z))
					return;
				break l;
			}
			ItemStack drop = null;
			Block block = Blocks.air;
			if (this == MineFactoryReloadedCore.milkLiquid)
			{
				if (rand.nextInt(50) == 0)
					drop = new ItemStack(Items.dye, rand.nextInt(2), 15);
			}
			else if (this == MineFactoryReloadedCore.sludgeLiquid)
			{
				drop = ((WeightedRandomItemStack)WeightedRandom.
						getRandomItem(rand, MFRRegistry.getSludgeDrops())).getStack();
			}
			else if (this == MineFactoryReloadedCore.sewageLiquid)
			{
				drop = new ItemStack(MineFactoryReloadedCore.fertilizerItem, 1 + rand.nextInt(2));
			}
			else if (this == MineFactoryReloadedCore.essenceLiquid)
			{
				if (world.setBlockToAir(x, y, z))
				{
					int i = rand.nextInt(5) + 10;
					while (i > 0)
					{
						int j = EntityXPOrb.getXPSplit(i);
						i -= j;
						world.spawnEntityInWorld(new EntityXPOrb(world,
								x + rand.nextDouble(), y + rand.nextDouble(), z + rand.nextDouble(), j));
					}
					fizz(world, x, y, z, rand);
					return;
				}
				break l;
			}
			else if (this == MineFactoryReloadedCore.biofuelLiquid)
			{
				if (world.setBlockToAir(x, y, z))
				{
					if (MFRConfig.enableFuelExploding.getBoolean(true))
						world.createExplosion(null, x, y, z, 4, true);
					fizz(world, x, y, z, rand);
					return;
				}
				break l;
			}
			else if (this == MineFactoryReloadedCore.meatLiquid)
			{
				if (rand.nextInt(5) != 0)
					drop = new ItemStack(MineFactoryReloadedCore.meatIngotRawItem, rand.nextInt(2));
				else
					drop = new ItemStack(MineFactoryReloadedCore.meatIngotCookedItem, rand.nextInt(2));
			}
			else if (this == MineFactoryReloadedCore.pinkSlimeLiquid)
			{
				if (rand.nextBoolean())
					drop = new ItemStack(MineFactoryReloadedCore.pinkSlimeballItem, rand.nextInt(3));
				else
					if (rand.nextInt(5) != 0)
						drop = new ItemStack(MineFactoryReloadedCore.meatNuggetRawItem, rand.nextInt(2));
					else
						drop = new ItemStack(MineFactoryReloadedCore.meatNuggetCookedItem, rand.nextInt(2));
			}
			else if (this == MineFactoryReloadedCore.chocolateMilkLiquid)
			{
				if (rand.nextBoolean())
					drop = new ItemStack(Items.dye, rand.nextInt(2), 3);
			}
			else if (this == MineFactoryReloadedCore.mushroomSoupLiquid)
			{
				if (rand.nextInt(5) == 0)
					block = (rand.nextBoolean() ? Blocks.brown_mushroom : Blocks.red_mushroom);
				else
					if (rand.nextBoolean())
						drop = new ItemStack(Blocks.brown_mushroom, rand.nextInt(2));
					else
						drop = new ItemStack(Blocks.red_mushroom, rand.nextInt(2));
			}
			if (world.setBlock(x, y, z, block, 0, 3))
			{
				if (drop != null && drop.stackSize > 0)
					this.dropBlockAsItem(world, x, y, z, drop);
				
				fizz(world, x, y, z, rand);
				return;
			}
		}
		super.updateTick(world, x, y, z, rand);
	}
	
	protected void fizz(World world, int x, int y, int z, Random rand)
	{
		world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D,
				"random.fizz", 0.5F, 2.6F + (rand.nextFloat() - rand.nextFloat()) * 0.8F);
		for (int l = 0; l < 8; ++l)
		{
			world.spawnParticle("largesmoke",
					x + rand.nextDouble(), y + rand.nextDouble(), z + rand.nextDouble(),
					0.0D, 0.0D, 0.0D);
		}
	}

	@Override
	public String getUnlocalizedName()
	{
		return "fluid." + this.unlocalizedName;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(IIconRegister ir)
	{
		_iconStill = ir.registerIcon("minefactoryreloaded:" + getUnlocalizedName());
		_iconFlowing = ir.registerIcon("minefactoryreloaded:" + getUnlocalizedName().replace(".still", ".flowing"));
	}

	@Override
	public IIcon getIcon(int side, int meta)
	{
		return side <= 1 ? _iconStill : _iconFlowing;
	}
}
