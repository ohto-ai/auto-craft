package com.autocraft.task.states;

import com.autocraft.AutoCraftMod;
import com.autocraft.navigation.NavigationProvider;
import com.autocraft.task.CraftingTask;
import com.autocraft.task.TaskState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class NavigateToInputState implements TaskState {
    private NavigationProvider nav;
    private int stuckTicks;

    @Override public void enter(CraftingTask ctx) {
        nav = AutoCraftMod.getInstance().getNavigationProvider();
        stuckTicks = 0;
        var inputs = ctx.getStation().getInputs();
        int idx = ctx.getCurrentInputIndex();
        if (idx < inputs.size()) nav.navigateTo(inputs.get(idx));
    }
    @Override public StateResult tick(CraftingTask ctx) {
        var inputs = ctx.getStation().getInputs();
        int idx = ctx.getCurrentInputIndex();
        if (idx >= inputs.size()) { ctx.transitionTo(new NavigateToWorkbenchState()); return StateResult.SUCCESS; }
        BlockPos target = inputs.get(idx);
        if (isWithinReach(target)) { ctx.transitionTo(new TakeMaterialsState()); return StateResult.SUCCESS; }
        NavigationProvider.Result r = nav.tick();
        if (r == NavigationProvider.Result.ARRIVED) { ctx.transitionTo(new TakeMaterialsState()); return StateResult.SUCCESS; }
        if (r == NavigationProvider.Result.PATHING) return StateResult.RUNNING;
        stuckTicks++;
        if (stuckTicks > 200) { AutoCraftMod.LOGGER.warn("Nav stuck, skipping to take materials"); ctx.transitionTo(new TakeMaterialsState()); return StateResult.SUCCESS; }
        return StateResult.RUNNING;
    }
    @Override public void exit(CraftingTask ctx) { if (nav != null) nav.cancel(); }
    @Override public String getDescription() { return "Navigating to input..."; }

    private boolean isWithinReach(BlockPos p) {
        var pl = MinecraftClient.getInstance().player;
        return pl != null && pl.getPos().squaredDistanceTo(p.toCenterPos()) <= 25.0;
    }
}
