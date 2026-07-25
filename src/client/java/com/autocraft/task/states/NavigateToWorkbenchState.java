package com.autocraft.task.states;

import com.autocraft.AutoCraftMod;
import com.autocraft.navigation.NavigationProvider;
import com.autocraft.task.CraftingTask;
import com.autocraft.task.TaskState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class NavigateToWorkbenchState implements TaskState {
    private NavigationProvider nav;
    private int stuckTicks;

    @Override public void enter(CraftingTask ctx) {
        nav = AutoCraftMod.getInstance().getNavigationProvider();
        stuckTicks = 0;
        BlockPos wb = ctx.getStation().getWorkbench();
        if (wb != null) nav.navigateTo(wb);
    }
    @Override public StateResult tick(CraftingTask ctx) {
        BlockPos wb = ctx.getStation().getWorkbench();
        if (wb == null) { ctx.stop("No workbench set"); return StateResult.STOPPED; }
        if (isWithinReach(wb)) { ctx.transitionTo(new PlaceRecipeState()); return StateResult.SUCCESS; }
        NavigationProvider.Result r = nav.tick();
        if (r == NavigationProvider.Result.ARRIVED) { ctx.transitionTo(new PlaceRecipeState()); return StateResult.SUCCESS; }
        if (r == NavigationProvider.Result.PATHING) return StateResult.RUNNING;
        stuckTicks++;
        if (stuckTicks > 200) { ctx.transitionTo(new PlaceRecipeState()); return StateResult.SUCCESS; }
        return StateResult.RUNNING;
    }
    @Override public void exit(CraftingTask ctx) { if (nav != null) nav.cancel(); }
    @Override public String getDescription() { return "Navigating to workbench..."; }

    private boolean isWithinReach(BlockPos p) {
        var pl = MinecraftClient.getInstance().player;
        return pl != null && pl.getPos().squaredDistanceTo(p.toCenterPos()) <= 25.0;
    }
}
