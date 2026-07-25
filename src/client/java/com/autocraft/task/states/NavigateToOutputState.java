package com.autocraft.task.states;

import com.autocraft.AutoCraftMod;
import com.autocraft.navigation.NavigationProvider;
import com.autocraft.task.CraftingTask;
import com.autocraft.task.TaskState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class NavigateToOutputState implements TaskState {
    private NavigationProvider nav;
    private int stuckTicks;

    @Override public void enter(CraftingTask ctx) {
        nav = AutoCraftMod.getInstance().getNavigationProvider();
        stuckTicks = 0;
        BlockPos out = ctx.getStation().getOutput();
        if (out != null) nav.navigateTo(out);
    }
    @Override public StateResult tick(CraftingTask ctx) {
        BlockPos out = ctx.getStation().getOutput();
        if (out == null) { ctx.stop("No output set"); return StateResult.STOPPED; }
        if (isWithinReach(out)) { ctx.transitionTo(new DepositResultState()); return StateResult.SUCCESS; }
        NavigationProvider.Result r = nav.tick();
        if (r == NavigationProvider.Result.ARRIVED) { ctx.transitionTo(new DepositResultState()); return StateResult.SUCCESS; }
        if (r == NavigationProvider.Result.PATHING) return StateResult.RUNNING;
        stuckTicks++;
        if (stuckTicks > 200) { ctx.transitionTo(new DepositResultState()); return StateResult.SUCCESS; }
        return StateResult.RUNNING;
    }
    @Override public void exit(CraftingTask ctx) { if (nav != null) nav.cancel(); }
    @Override public String getDescription() { return "Navigating to output..."; }

    private boolean isWithinReach(BlockPos p) {
        var pl = MinecraftClient.getInstance().player;
        return pl != null && pl.getPos().squaredDistanceTo(p.toCenterPos()) <= 25.0;
    }
}
