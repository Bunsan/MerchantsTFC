package com.aleksey.merchants.TileEntities;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;

import com.aleksey.merchants.Core.WarehouseBookInfo;
import com.aleksey.merchants.Helpers.PrepareTradeResult;
import com.aleksey.merchants.Helpers.WarehouseManager;
import com.aleksey.merchants.Items.ItemWarehouseBook;
import com.bioxx.tfc.TileEntities.NetworkTileEntity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityStall extends NetworkTileEntity implements IInventory
{
    public static final int PriceCount = 5;
    public static final int ItemCount = 2 * PriceCount + 1;

    public static final int[] PricesSlotIndexes = new int[] { 0, 2, 4, 6, 8 };
    public static final int[] GoodsSlotIndexes = new int[] { 1, 3, 5, 7 };

    private static final byte _actionId_ClearPrices = 0;
    private static final byte _actionId_Buy = 1;

    private ItemStack[] _storage;
    private WarehouseManager _warehouse;
    private String _ownerUserName;
    private WarehouseBookInfo _bookInfo;

    public TileEntityStall()
    {
        _storage = new ItemStack[ItemCount];
        _warehouse = new WarehouseManager();
    }

    public boolean getIsOwnerSpecified()
    {
        return _ownerUserName != null;
    }

    public void setOwnerUserName(String ownerUserName)
    {
        _ownerUserName = ownerUserName;
        _bookInfo = null;
    }

    public String getOwnerUserName()
    {
        return _ownerUserName;
    }

    public WarehouseBookInfo getBookInfo()
    {
        return _bookInfo;
    }

    public void calculateQuantitiesInWarehouse()
    {
        if(_ownerUserName == null)
            return;
        
        ItemStack itemStack = _storage[ItemCount - 1];
        
        if(itemStack != null && itemStack.getItem() instanceof ItemWarehouseBook)
        {
            _bookInfo = WarehouseBookInfo.readFromNBT(itemStack.getTagCompound());
            
            if(_warehouse.existWarehouse(this.xCoord, this.yCoord, this.zCoord, _bookInfo, this.worldObj))
                _warehouse.searchContainers(_bookInfo, this.worldObj);
            else
                _bookInfo = null;
        }
        else
            _bookInfo = null;

        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
    }

    public int getQuantityInWarehouse(ItemStack itemStack)
    {
        return _warehouse.getQuantity(itemStack);
    }

    public int getContainersInWarehouse()
    {
        return _warehouse.getContainers();
    }

    public PrepareTradeResult prepareTrade(ItemStack goodStack, ItemStack payStack)
    {
        return _bookInfo != null && _warehouse.existWarehouse(this.xCoord, this.yCoord, this.zCoord, _bookInfo, this.worldObj)
            ? _warehouse.prepareTrade(goodStack, payStack, this.worldObj)
            : PrepareTradeResult.NoGoods;
    }

    public void confirmTrade()
    {
        _warehouse.confirmTrade(this.worldObj);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox()
    {
        AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord + 1, yCoord + 1, zCoord + 1);
        return bb;
    }

    @Override
    public void closeInventory()
    {
        int newMeta = 0;
        
        for(int i = 0; i < _storage.length; i++)
        {
            if(_storage[i] != null)
            {
                newMeta = 1;
                break;
            }
        }
        
        int meta = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
        
        if(meta != newMeta)
            this.worldObj.setBlockMetadataWithNotify(this.xCoord, this.yCoord, this.zCoord, newMeta, 2);
    }

    @Override
    public ItemStack decrStackSize(int i, int j)
    {
        if (_storage[i] != null)
        {
            if (_storage[i].stackSize <= j)
            {
                ItemStack is = _storage[i];
                _storage[i] = null;
                return is;
            }
            
            ItemStack isSplit = _storage[i].splitStack(j);
            
            if (_storage[i].stackSize == 0)
                _storage[i] = null;
            
            return isSplit;
        }
        else
        {
            return null;
        }
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 64;
    }

    @Override
    public String getInventoryName()
    {
        return "gui.Stall.Title";
    }

    @Override
    public int getSizeInventory()
    {
        return ItemCount;
    }

    @Override
    public ItemStack getStackInSlot(int i)
    {
        return _storage[i];
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int i)
    {
        return _storage[i];
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer)
    {
        return false;
    }

    @Override
    public void openInventory()
    {
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack is)
    {
        if (!ItemStack.areItemStacksEqual(_storage[i], is))
        {
            _storage[i] = is;
        }
    }

    @Override
    public boolean hasCustomInventoryName()
    {
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack)
    {
        return false;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        writeStallToNBT(nbt);

        _warehouse.writeToNBT(nbt);

        if(_bookInfo != null)
        {
            NBTTagCompound bookTag = new NBTTagCompound();
            _bookInfo.writeToNBT(bookTag);
            
            nbt.setTag("Book", bookTag);
        }
    }

    public void writeStallToNBT(NBTTagCompound nbt)
    {
        if(_ownerUserName != null)
            nbt.setString("OwnerUserName", _ownerUserName);

        NBTTagList itemList = new NBTTagList();

        for (int i = 0; i < _storage.length; i++)
        {
            if (_storage[i] != null)
            {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("Slot", (byte) i);

                _storage[i].writeToNBT(itemTag);

                itemList.appendTag(itemTag);
            }
        }

        nbt.setTag("Items", itemList);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);

        readStallFromNBT(nbt);

        _warehouse.readFromNBT(nbt);
        
        _bookInfo = nbt.hasKey("Book") ? WarehouseBookInfo.readFromNBT(nbt.getCompoundTag("Book")): null;
    }

    public void readStallFromNBT(NBTTagCompound nbt)
    {
        _ownerUserName = nbt.hasKey("OwnerUserName") ? nbt.getString("OwnerUserName"): null;

        NBTTagList itemList = nbt.getTagList("Items", 10);

        for (int i = 0; i < itemList.tagCount(); i++)
        {
            NBTTagCompound itemTag = itemList.getCompoundTagAt(i);
            byte byte0 = itemTag.getByte("Slot");

            if (byte0 >= 0 && byte0 < _storage.length)
                setInventorySlotContents(byte0, ItemStack.loadItemStackFromNBT(itemTag));
        }
    }

    @Override
    public void handleInitPacket(NBTTagCompound nbt)
    {
        _ownerUserName = nbt.hasKey("OwnerUserName") ? nbt.getString("OwnerUserName"): null;

        _warehouse.readFromNBT(nbt);
        
        _bookInfo = nbt.hasKey("Book") ? WarehouseBookInfo.readFromNBT(nbt.getCompoundTag("Book")): null;

        this.worldObj.func_147479_m(xCoord, yCoord, zCoord);
    }

    @Override
    public void createInitNBT(NBTTagCompound nbt)
    {
        if(_ownerUserName != null)
            nbt.setString("OwnerUserName", _ownerUserName);

        _warehouse.writeToNBT(nbt);
        
        if(_bookInfo != null)
        {
            NBTTagCompound bookTag = new NBTTagCompound();
            _bookInfo.writeToNBT(bookTag);
            
            nbt.setTag("Book", bookTag);
        }
    }

    @Override
    public void handleDataPacket(NBTTagCompound nbt)
    {
        if (!nbt.hasKey("Action"))
            return;

        byte action = nbt.getByte("Action");

        switch (action)
        {
            case _actionId_ClearPrices:
                actionHandlerClearPrices();
                break;
            case _actionId_Buy:
                actionHandlerBuy(nbt);
                break;
        }
    }

    @Override
    public void updateEntity()
    {
    }

    //Send action to server
    public void actionClearPrices()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setByte("Action", _actionId_ClearPrices);
        this.broadcastPacketInRange(this.createDataPacket(nbt));

        this.worldObj.func_147479_m(xCoord, yCoord, zCoord);
    }

    private void actionHandlerClearPrices()
    {
        for (int i = 0; i < _storage.length - 1; i++)
            _storage[i] = null;

        this.worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }
    
    
    //Send action to client
    public void actionBuy(ItemStack itemStack)
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setByte("Action", _actionId_Buy);
        
        NBTTagCompound itemTag = new NBTTagCompound();
        itemStack.writeToNBT(itemTag);
        
        nbt.setTag("Item", itemTag);
        
        this.broadcastPacketInRange(this.createDataPacket(nbt));

        this.worldObj.func_147479_m(xCoord, yCoord, zCoord);
    }
    
    private void actionHandlerBuy(NBTTagCompound nbt)
    {
        NBTTagCompound itemTag = nbt.getCompoundTag("Item");
        ItemStack itemStack = ItemStack.loadItemStackFromNBT(itemTag);
        
        this.entityplayer.inventory.setItemStack(itemStack);
    }
}