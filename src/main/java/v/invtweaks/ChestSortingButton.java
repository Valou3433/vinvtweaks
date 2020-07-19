package v.invtweaks;

import com.mojang.brigadier.Message;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.client.gui.screen.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentUtils;

public class ChestSortingButton extends Button
{
    private static final int widthIn = 10;
    private static final int heightIn = 10;
    private static final ITextComponent textComp = TextComponentUtils.toTextComponent(new Message()
    {
        @Override
        public String getString()
        {
            return "z";
        }
    });

    public ChestSortingButton(ChestScreen gui)
    {
        super(gui.getGuiLeft() + gui.getXSize() - widthIn - 4, gui.getGuiTop() + 4, widthIn, heightIn, textComp, new IPressable()
        {
            @Override
            public void onPress(Button p_onPress_1_)
            {
                VInvTweaks.sortInventory(gui.getContainer().getLowerChestInventory(), gui.getContainer().windowId, 0);
            }
        });
    }
    public ChestSortingButton(ShulkerBoxScreen gui)
    {
        super(gui.getGuiLeft() + gui.getXSize() - widthIn - 4, gui.getGuiTop() + 4, widthIn, heightIn, textComp, new IPressable()
        {
            @Override
            public void onPress(Button p_onPress_1_)
            {
                IInventory i = new IInventory()
                {
                    @Override
                    public int getSizeInventory()
                    {
                        return VInvTweaks.SHULKER_INVENTORY_SIZE;
                    }

                    @Override
                    public boolean isEmpty()
                    {
                        return false;
                    }

                    @Override
                    public ItemStack getStackInSlot(int index)
                    {
                        return gui.getContainer().getInventory().get(index);
                    }

                    @Override
                    public ItemStack decrStackSize(int index, int count) {return null;}
                    @Override
                    public ItemStack removeStackFromSlot(int index) {return null;}
                    @Override
                    public void setInventorySlotContents(int index, ItemStack stack) {}
                    @Override
                    public void markDirty() {}
                    @Override
                    public boolean isUsableByPlayer(PlayerEntity player) {return false;}
                    @Override
                    public void clear() {}
                };
                VInvTweaks.sortInventory(i, gui.getContainer().windowId, 0);
            }
        });
    }
}
