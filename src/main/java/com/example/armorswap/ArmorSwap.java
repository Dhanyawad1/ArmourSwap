package com.example.armorswap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
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

@Mod(modid = "armorswap", name = "Armor Swap", version = "2.1",
        clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class ArmorSwap {

    /** Pieces with less than this fraction of durability left are never worn. */
    private static final double MIN_DURABILITY_FRACTION = 0.30;

    /** Ticks to wait between pieces (1 tick = 50 ms). 3 -> ~0.45 s for a full swap. */
    private static final int TICKS_BETWEEN_PIECES = 3;

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
        if (down && !wasDown && !active && mc.currentScreen == null) {
            active = true; nextPiece = 0; cooldown = 0; changed = 0;   // start one operation
        }
        wasDown = down;
