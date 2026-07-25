package com.autocraft.interaction;

import com.autocraft.AutoCraftMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;

/**
 * Container interaction — uses interactBlock() for opening, click packets for slot ops.
 */
public class ContainerInteractor {

    public static void openContainer(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        Vec3d hit = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        BlockHitResult result = new BlockHitResult(hit, Direction.UP, pos, false);
        client.interactionManager.interactBlock(player, Hand.MAIN_HAND, result);
    }

    public static void quickMove(int syncId, int slot) {
        sendClick(syncId, slot, 0, SlotActionType.QUICK_MOVE);
    }

    public static void leftClick(int syncId, int slot) {
        sendClick(syncId, slot, 0, SlotActionType.PICKUP);
    }

    public static void rightClick(int syncId, int slot) {
        sendClick(syncId, slot, 1, SlotActionType.PICKUP);
    }

    private static void sendClick(int syncId, int slot, int button, SlotActionType action) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || player.networkHandler == null) return;
        ScreenHandler handler = player.currentScreenHandler;
        int stateId = getStateId(handler);
        player.networkHandler.sendPacket(new ClickSlotC2SPacket(
                syncId, stateId, slot, button, action,
                handler.getCursorStack(), new Int2ObjectArrayMap<>()));
    }

    public static boolean isContainerOpen() {
        return MinecraftClient.getInstance().currentScreen instanceof HandledScreen;
    }

    public static void closeContainer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.networkHandler != null && isContainerOpen()) {
            client.player.networkHandler.sendPacket(
                    new net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket(
                            client.player.currentScreenHandler.syncId));
        }
    }

    private static int getStateId(ScreenHandler handler) {
        try {
            return (int) handler.getClass().getDeclaredField("field_29053").get(handler);
        } catch (Exception e) {
            try {
                return (int) handler.getClass().getDeclaredField("stateId").get(handler);
            } catch (Exception e2) {
                return 0;
            }
        }
    }
}
