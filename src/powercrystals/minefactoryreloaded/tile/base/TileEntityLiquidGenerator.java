package powercrystals.minefactoryreloaded.tile.base;

import cofh.core.util.fluid.FluidTankCore;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Locale;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import powercrystals.minefactoryreloaded.core.ITankContainerBucketable;
import powercrystals.minefactoryreloaded.gui.client.GuiFactoryInventory;
import powercrystals.minefactoryreloaded.gui.client.GuiLiquidGenerator;
import powercrystals.minefactoryreloaded.gui.container.ContainerFactoryGenerator;
import powercrystals.minefactoryreloaded.setup.Machine;

public abstract class TileEntityLiquidGenerator extends TileEntityFactoryGenerator implements ITankContainerBucketable {
	private int _liquidConsumedPerTick;
	private int _powerProducedPerConsumption;

	public TileEntityLiquidGenerator(Machine machine, int liquidConsumedPerTick,
			int ticksBetweenConsumption) {
		this(machine, liquidConsumedPerTick,
				machine.getActivationEnergy() * ticksBetweenConsumption,
				ticksBetweenConsumption);
	}

	public TileEntityLiquidGenerator(Machine machine, int liquidConsumedPerTick,
			int powerProducedPerConsumption, int ticksBetweenConsumption) {
		super(machine, ticksBetweenConsumption);
		_powerProducedPerConsumption = powerProducedPerConsumption;
		_liquidConsumedPerTick = liquidConsumedPerTick;
	}

	@Override
	protected boolean consumeFuel() {
		FluidStack drained = drain(null, _liquidConsumedPerTick, false);

		if (drained == null || drained.amount != _liquidConsumedPerTick)
			return false;

		drain(null, _liquidConsumedPerTick, true);
		return true;
	}

	@Override
	protected boolean hasFuel() {
		return _tanks[0].getFluidAmount() != 0;
	}

	@Override
	protected boolean canConsumeFuel(int space) {
		return space >= _powerProducedPerConsumption;
	}

	@Override
	protected int produceEnergy() {
		return _powerProducedPerConsumption;
	}

	protected abstract boolean isFluidFuel(FluidStack fuel);

	@Override
	protected FluidTankCore[] createTanks() {
		return new FluidTankCore[] {new FluidTankCore(BUCKET_VOLUME * 4)};
	}

	protected String getFluidName(FluidStack fluid) {
		if (fluid == null || fluid.getFluid() == null)
			return null;
		String name = fluid.getFluid().getName();
		if (name == null)
			return null;
		return name.trim().toLowerCase(Locale.US);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiFactoryInventory getGui(InventoryPlayer inventoryPlayer) {
		return new GuiLiquidGenerator(getContainer(inventoryPlayer), this);
	}

	@Override
	public ContainerFactoryGenerator getContainer(InventoryPlayer inventoryPlayer) {
		return new ContainerFactoryGenerator(this, inventoryPlayer);
	}

	@Override
	public String getGuiBackground() {
		return "fluidgenerator";
	}

	@Override
	public boolean allowBucketFill(ItemStack stack) {
		return true;
	}

	@Override
	public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
		if (resource != null && isFluidFuel(resource))
			for (FluidTankCore _tank : getTanks())
				if (_tank.getFluidAmount() == 0 || resource.isFluidEqual(_tank.getFluid()))
					return _tank.fill(resource, doFill);
		return 0;
	}

	@Override
	public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
		return drain(maxDrain, doDrain);
	}

	@Override
	public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
		return drain(resource, doDrain);
	}

	@Override
	public boolean canFill(EnumFacing from, Fluid fluid) {
		return true;
	}

	@Override
	public boolean canDrain(EnumFacing from, Fluid fluid) {
		return false;
	}
}
