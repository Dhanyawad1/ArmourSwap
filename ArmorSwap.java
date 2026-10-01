package com.example.armorswap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = "armorswap", name = "Armor Swap", version = "2.2",
        clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class ArmorSwap {

    /** Pieces with less than this fraction of durability left are never worn. */
    private static final double MIN_DURABILITY_FRACTION = 0.30;

    /** Ticks to wait between pieces (20 ticks = 1 second). 10 = 0.5 s. */
    private static final int TICKS_BETWEEN_PIECES = 10;

    /** Ticks to wait after the inventory opens before the first click. 6 = 0.3 s. */
    private static final int OPEN_DELAY_TICKS = 6;

    private static final int FIRST_ARMOR_CONTAINER_SLOT = 5;
    private static final int LAST_CONTAINER_SLOT = 44;

    private final KeyBinding key = new KeyBinding("Swap armor", Keyboard.KEY_H, "Armor Swap");
    private boolean wasDown = false;

    // swap-in-progress state
    private boolean active = false;
    private int nextPiece = 0;     // 0=helmet ... 3=boots
    private int cooldown = 0;
    private int changed = 0;

    @EventHandler
    public void init(FMLInitializationEvent e) {
        ClientRegistry.registerKeyBinding(key);
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.thePlayer;
        if (p == null || mc.playerController == null) { active = false; return; }

        boolean down = key.getKeyCode() > 0 && Keyboard.isKeyDown(key.getKeyCode());
        boolean pressed = down && !wasDown;
        wasDown = down;

        if (!active) {
            // One press = one operation. Open the inventory like pressing E.
            if (pressed && mc.currentScreen == null) {
                mc.displayGuiScreen(new GuiInventory(p));
                active = true;
                nextPiece = 0;
                changed = 0;
                cooldown = OPEN_DELAY_TICKS;
            }
            return;
        }

        // Player pressed Esc/E or something else took over the screen: stop.
        if (!(mc.currentScreen instanceof GuiInventory)) {
            active = false;
            msg(p, "Cancelled.");
            return;
        }

        if (cooldown > 0) { cooldown--; return; }

        // All pieces handled and the last wait is over: close and finish.
        if (nextPiece >= 4) {
            active = false;
            p.closeScreen();
            msg(p, changed == 0 ? "Armor already optimal." : "Swapped " + changed + " piece(s).");
            return;
        }

        // Find the next piece that needs changing (pieces already best are skipped).
        while (nextPiece < 4) {
            boolean swapped = processPiece(mc, p, nextPiece);
            nextPiece++;
            if (swapped) {
                changed++;
                cooldown = TICKS_BETWEEN_PIECES;
                break;
            }
        }
    }

    /** Handles one armor slot. Returns true if clicks were sent. */
    private boolean processPiece(Minecraft mc, EntityPlayerSP p, int t) {
        Container c = p.inventoryContainer;
        int armorSlot = FIRST_ARMOR_CONTAINER_SLOT + t;

        ItemStack worn = c.getSlot(armorSlot).getStack();
        int bestSlot = armorSlot;
        int bestDur = (worn != null && usable(worn)) ? remaining(worn) : -1;

        for (int i = FIRST_ARMOR_CONTAINER_SLOT + 4; i <= LAST_CONTAINER_SLOT; i++) {
            ItemStack s = c.getSlot(i).getStack();
            if (s == null || !s.getItem().isValidArmor(s, t, p)) continue;
            if (!usable(s)) continue;
            int d = remaining(s);
            if (d > bestDur) { bestDur = d; bestSlot = i; }
        }

        if (bestSlot == armorSlot) return false;

        click(mc, c, bestSlot);
        click(mc, c, armorSlot);
        click(mc, c, bestSlot);
        return true;
    }

    private boolean usable(ItemStack s) {
        int max = s.getMaxDamage();
        if (max <= 0) return true;
        return (double) (max - s.getItemDamage()) / max >= MIN_DURABILITY_FRACTION;
    }

    private int remaining(ItemStack s) {
        int max = s.getMaxDamage();
        return max <= 0 ? Integer.MAX_VALUE : max - s.getItemDamage();
    }

    private void click(Minecraft mc, Container c, int slot) {
        mc.playerController.windowClick(c.windowId, slot, 0, 0, mc.thePlayer);
    }

    private void msg(EntityPlayerSP p, String text) {
        p.addChatMessage(new ChatComponentText("\u00a77[ArmorSwap] " + text));
    }
}
