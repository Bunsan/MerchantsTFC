package com.aleksey.merchants.Blocks.Devices;

import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.aleksey.merchants.MerchantsMod;
import com.aleksey.merchants.Core.BlockList;
import com.aleksey.merchants.Handlers.GuiHandler;
import com.aleksey.merchants.TileEntities.TileEntityStall;
import com.bioxx.tfc.Blocks.BlockTerraContainer;
import com.bioxx.tfc.Core.TFCTabs;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockStall extends BlockTerraContainer
{
    @SideOnly(Side.CLIENT)
    private IIcon _sideIcon;
      
    @SideOnly(Side.CLIENT)
    private IIcon _topIcon;

    public BlockStall()
    {
        super(Material.wood);
        this.setCreativeTab(TFCTabs.TFCDevices);
        this.setBlockBounds(0, 0, 0, 1, 1, 1);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister register)
    {
        _topIcon = register.registerIcon("merchants:StallTop");
        _sideIcon = register.registerIcon("merchants:StallSide");
    }

    @Override
    public IIcon getIcon(int side, int meta)
    {
        return side <= 1 ? _topIcon: _sideIcon;
    }

    @Override
    public int damageDropped(int meta)
    {
        return meta;
    }

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    @Override
    public int getRenderType()
    {
        return BlockList.StallRenderId;
    }

    @Override
    public void onBlockPreDestroy(World world, int x, int y, int z, int meta) 
    {
        if (world.isRemote)
            return;

        TileEntityStall te = (TileEntityStall)world.getTileEntity(x, y, z);
        
        if (te == null)
            return;

        ItemStack is = new ItemStack(Item.getItemFromBlock(this), 1, 0);
        NBTTagCompound nbt = writeStallToNBT(te);
        is.setTagCompound(nbt);
        EntityItem ei = new EntityItem(world, x, y, z, is);
        
        world.spawnEntityInWorld(ei);

        for(int s = 0; s < te.getSizeInventory(); ++s)
            te.setInventorySlotContents(s, null);
    }

    /**
     * Called when the block is placed in the world.
     */
    @Override
    public void onBlockPlacedBy(World world, int i, int j, int k, EntityLivingBase player, ItemStack is)
    {
        super.onBlockPlacedBy(world, i, j, k, player, is);
        
        TileEntity te = world.getTileEntity(i, j, k);
        
        if (te == null || !is.hasTagCompound() || !(te instanceof TileEntityStall))
            return;

        TileEntityStall stall = (TileEntityStall)te;

        stall.readStallFromNBT(is.getTagCompound());
        
        world.markBlockForUpdate(i, j, k);
    }

    /**
     * Checks to see if its valid to put this block at the specified coordinates. Args: world, x, y, z
     */
    @Override
    public boolean canPlaceBlockAt(World par1World, int par2, int par3, int par4)
    {
        return true;
    }

    @Override
    public boolean canDropFromExplosion(Explosion exp)
    {

        return true;
    }

    @Override
    protected void dropBlockAsItem(World par1World, int par2, int par3, int par4, ItemStack par5ItemStack)
    {
    }

    public NBTTagCompound writeStallToNBT(TileEntityStall te)
    {
        NBTTagCompound nbt = new NBTTagCompound();
        
        te.writeStallToNBT(nbt);

        return nbt;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ)
    {
        if (world.isRemote)
        {
            world.markBlockForUpdate(x, y, z);
            return true;
        }

        TileEntity te = world.getTileEntity(x, y, z);

        if(te == null || !(te instanceof TileEntityStall))
            return false;
        
        TileEntityStall stall = (TileEntityStall)te;
        
        stall.calculateQuantitiesInWarehouse();
        
        int guiId = player.isSneaking() && stall.getIsWarehouseSpecified() ? GuiHandler.GuiBuyerStall: GuiHandler.GuiOwnerStall; 

        player.openGui(MerchantsMod.instance, guiId, world, x, y, z);

        return true;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess par1iBlockAccess, int par2, int par3, int par4, int par5)
    {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2)
    {
        return new TileEntityStall();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean addDestroyEffects(World world, int x, int y, int z, int meta, EffectRenderer effectRenderer)
    {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean addHitEffects(World worldObj, MovingObjectPosition target, EffectRenderer effectRenderer)
    {
        return true;
    }
}