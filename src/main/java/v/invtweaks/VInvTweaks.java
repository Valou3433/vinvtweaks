package v.invtweaks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.client.gui.screen.inventory.ShulkerBoxScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_K;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

@Mod("vinvtweaks")
public class VInvTweaks
{
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public static final boolean DEBUG = false;
    public static final int PLAYER_INVENTORY_SIZE = 36;
    public static final int SHULKER_INVENTORY_SIZE = 27;
    public static KeyBinding keySortBinding;

    public VInvTweaks()
    {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        //Make sure the mod being absent on the other network side does not cause the client to display the server as incompatible
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {}

    private void doClientStuff(final FMLClientSetupEvent event)
    {
        //register inventory sort key binding default on k
        keySortBinding = new KeyBinding("key.sort.desc", GLFW_KEY_K, "key.categories.inventory");
        ClientRegistry.registerKeyBinding(keySortBinding);
        MinecraftForge.EVENT_BUS.register(VKeyEventListener.class);

        //register gui init event to add sort button to chest/shulker gui
        MinecraftForge.EVENT_BUS.register(VGuiEventListener.class);

        MinecraftForge.EVENT_BUS.register(VToolBreakEventListener.class);
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {}
    private void processIMC(final InterModProcessEvent event) {}

    static class Slot
    {
        final String name;
        int position;
        final boolean isEmpty;
        final ItemStack itemStack;
        public Slot(String namel, int positionl, boolean isEmpty, ItemStack itemStack)
        {
            this.name = namel;
            this.position = positionl;
            this.isEmpty = isEmpty;
            this.itemStack = itemStack;
        }
    }

    public static void sortInventory(IInventory inventory, int windowId, int slotStart)
    {
        //group items of same type
        int size = inventory.getSizeInventory();
        if(inventory instanceof PlayerInventory) size = PLAYER_INVENTORY_SIZE; //should fix player inventory bug sorting hotbar
        if(DEBUG) System.out.println("Inventory size = " + size + "  (" + inventory.toString() + ")");
        for(int i = slotStart; i < size; i++)
        {
            ItemStack stacki = inventory.getStackInSlot(i);
            if(stacki.isEmpty()) continue;
            for(int j = i + 1; j < size; j++)
            {
                ItemStack stackj = inventory.getStackInSlot(j);
                if(stackj.isEmpty()) continue;
                if(stacki.isItemEqual(stackj) && stacki.isStackable())
                {
                    Minecraft.getInstance().playerController.windowClick(windowId, j, GLFW_MOUSE_BUTTON_1, ClickType.PICKUP, Minecraft.getInstance().player);
                    Minecraft.getInstance().playerController.windowClick(windowId, i, GLFW_MOUSE_BUTTON_1, ClickType.PICKUP, Minecraft.getInstance().player);
                    Minecraft.getInstance().playerController.windowClick(windowId, j, GLFW_MOUSE_BUTTON_1, ClickType.PICKUP, Minecraft.getInstance().player);
                }
            }
        }

        //create sorted list of inventory
        ArrayList<Slot> slotList = new ArrayList<Slot>();
        for(int i = slotStart; i < size; i++)
        {
            ItemStack stacki = inventory.getStackInSlot(i);
            slotList.add(new Slot(stacki.getDisplayName().getString(), i, stacki.isEmpty(), stacki));
        }
        slotList.sort(new Comparator<Slot>() {
            @Override
            public int compare(Slot slot, Slot t1) {
                return compareSlots(slot, t1);
            }
        });

        //sort inventory via windowclick
        for(int i = slotStart; i < size; i++)
        {
            Slot current = slotList.get(i - slotStart);
            if(current.isEmpty || inventory.getStackInSlot(current.position).isEmpty() || current.position == i) continue;
            if(DEBUG) System.out.println("Non-Empty Misplaced Slot " + (current.position - slotStart) + ", should go into position " + (i - slotStart));
            Minecraft.getInstance().playerController.windowClick(windowId, (current.position), GLFW_MOUSE_BUTTON_1, ClickType.PICKUP, Minecraft.getInstance().player);
            Minecraft.getInstance().playerController.windowClick(windowId, i, GLFW_MOUSE_BUTTON_1, ClickType.PICKUP, Minecraft.getInstance().player);

            if(!inventory.getStackInSlot(i).isEmpty())
            {
                //if destination slot is not empty, we swap
                Minecraft.getInstance().playerController.windowClick(windowId, (current.position), GLFW_MOUSE_BUTTON_1, ClickType.PICKUP, Minecraft.getInstance().player);
                for(int k = slotStart; k < size; k++)
                    if(slotList.get(k-slotStart).position == i) {slotList.get(k-slotStart).position = current.position; break;}
            }
            current.position = i;
        }
    }

    public static int compareSlots(Slot slot, Slot t1)
    {
        if(slot.isEmpty && t1.isEmpty) return 0;
        else if(slot.isEmpty) return 10000000;
        else if(t1.isEmpty) return -10000000;

        int alphaCompare = slot.name.compareTo(t1.name)*10000;

        if(alphaCompare != 0) return alphaCompare;

        //compare metadatas : new tools should go before old, enchants should be matched (for books)
        int damageCompare = (slot.itemStack.getDamage() - t1.itemStack.getDamage());
        if(damageCompare != 0) return damageCompare;

        ListNBT slotEnchants = slot.itemStack.getEnchantmentTagList();
        ListNBT tEnchants = slot.itemStack.getEnchantmentTagList();
        if(slotEnchants.size() > tEnchants.size()) return 1;
        else if(tEnchants.size() > slotEnchants.size()) return -1;
        for(int i = 0; i < slotEnchants.size(); i++)
        {
            int enchantCompare = tEnchants.get(i).getId() - slotEnchants.get(i).getId();
            if(enchantCompare != 0) return enchantCompare;
        }

        return 0;
    }

    static class VKeyEventListener
    {
        @SubscribeEvent
        public static void onKeyEvent(InputEvent.KeyInputEvent event)
        {
            if(keySortBinding.isPressed())
            {
                //sort player inventory
                //player inventory slots : 9 - 35 is main inventory
                sortInventory(Minecraft.getInstance().player.inventory, Minecraft.getInstance().player.container.windowId, 9);
            }
        }
    }
    static class VGuiEventListener
    {
        @SubscribeEvent
        public static void onGuiInit(final GuiScreenEvent.InitGuiEvent event)
        {
            if(event == null || event.getGui() == null) return;
            if(event.getGui() instanceof ChestScreen)
            {
                ChestScreen gui = (ChestScreen) event.getGui();
                event.addWidget(new ChestSortingButton(gui));
            }
            else if(event.getGui() instanceof ShulkerBoxScreen)
            {
                ShulkerBoxScreen gui = (ShulkerBoxScreen) event.getGui();
                event.addWidget(new ChestSortingButton(gui));
            }
        }
    }
    static class VToolBreakEventListener
    {
        @SubscribeEvent
        public static void onToolBreak(PlayerDestroyItemEvent event)
        {
            if(event == null) return;
            ItemStack destroyed = event.getOriginal();
            for(int i = 9; i < PLAYER_INVENTORY_SIZE; i++)
            {
                if(Minecraft.getInstance().player.inventory.mainInventory.get(i).getItem().equals(destroyed.getItem()))
                {
                    Minecraft.getInstance().playerController.windowClick(Minecraft.getInstance().player.container.windowId, i, GLFW_MOUSE_BUTTON_1, ClickType.PICKUP, Minecraft.getInstance().player);
                    Minecraft.getInstance().playerController.windowClick(Minecraft.getInstance().player.container.windowId, 36 + Minecraft.getInstance().player.inventory.currentItem, GLFW_MOUSE_BUTTON_1, ClickType.PICKUP, Minecraft.getInstance().player);
                    break;
                }
            }
        }
    }
}
