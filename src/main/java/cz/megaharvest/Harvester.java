package cz.megaharvest;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Jednorázová sklizeň 3x3. Používá výhradně běžné klientské akce (rozbití bloku,
 * položení předmětu), takže na serveru vypadá jako hráč, který to udělal rukou.
 */
public final class Harvester {
    private static final int RADIUS = 1; // 1 => 3x3

    /** Plodina -> předmět, kterým se zasazuje. */
    private static final Map<Block, Item> SEEDS = Map.of(
            Blocks.WHEAT, Items.WHEAT_SEEDS,
            Blocks.CARROTS, Items.CARROT,
            Blocks.POTATOES, Items.POTATO,
            Blocks.BEETROOTS, Items.BEETROOT_SEEDS,
            Blocks.NETHER_WART, Items.NETHER_WART);

    private Harvester() {}

    public static void run(Minecraft mc) {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        MultiPlayerGameMode gm = mc.gameMode;
        if (player == null || level == null || gm == null || player.isSpectator()) {
            return;
        }

        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            msg(player, "no_target");
            return;
        }

        // Střed: pozemek, na který hráč míří, nebo plodina nad ním.
        BlockPos center = hit.getBlockPos();
        if (!SEEDS.containsKey(level.getBlockState(center).getBlock())) {
            center = center.above();
        }
        if (!SEEDS.containsKey(level.getBlockState(center).getBlock())) {
            msg(player, "no_target");
            return;
        }

        Inventory inv = player.getInventory();
        int originalSlot = inv.getSelectedSlot();
        Deque<int[]> swaps = new ArrayDeque<>();
        int harvested = 0;
        int replanted = 0;

        try {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, 0, dz);
                    BlockState state = level.getBlockState(pos);
                    if (!isMature(state) || !inReach(player, pos)) {
                        continue;
                    }
                    Item seed = SEEDS.get(state.getBlock());

                    gm.startDestroyBlock(pos, Direction.UP);
                    if (!level.getBlockState(pos).isAir()) {
                        continue; // rozbití se nepovedlo
                    }
                    harvested++;

                    if (replant(mc, player, gm, pos, seed, swaps)) {
                        replanted++;
                    }
                }
            }
        } finally {
            gm.stopDestroyBlock();
            // Vrať inventář do původního stavu (v opačném pořadí než se prohazovalo).
            while (!swaps.isEmpty()) {
                int[] s = swaps.pop();
                gm.handleContainerInput(player.inventoryMenu.containerId, s[0], s[1], ContainerInput.SWAP, player);
            }
            inv.setSelectedSlot(originalSlot);
        }

        if (harvested == 0) {
            msg(player, "nothing");
        } else {
            player.sendSystemMessage(
                    Component.translatable("message.megaharvest.result", harvested, replanted));
        }
    }

    private static boolean replant(Minecraft mc, LocalPlayer player, MultiPlayerGameMode gm,
                                   BlockPos pos, Item seed, Deque<int[]> swaps) {
        BlockPos ground = pos.below();
        if (!inReach(player, ground) || !equipSeed(gm, player, seed, swaps)) {
            return false;
        }
        BlockHitResult place = new BlockHitResult(
                Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false);
        InteractionResult result = gm.useItemOn(player, InteractionHand.MAIN_HAND, place);
        return result.consumesAction();
    }

    /** Dostane semena do vybraného hotbar slotu (hotbar -> přepnutí slotu, inventář -> SWAP). */
    private static boolean equipSeed(MultiPlayerGameMode gm, LocalPlayer player, Item seed, Deque<int[]> swaps) {
        Inventory inv = player.getInventory();
        int selected = inv.getSelectedSlot();
        if (inv.getItem(selected).is(seed)) {
            return true;
        }
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(seed)) {
                inv.setSelectedSlot(i);
                return true;
            }
        }
        for (int i = 9; i < 36; i++) {
            if (inv.getItem(i).is(seed)) {
                gm.handleContainerInput(player.inventoryMenu.containerId, i, selected, ContainerInput.SWAP, player);
                swaps.push(new int[] {i, selected});
                return true;
            }
        }
        return false;
    }

    /** Vanilla dosah pro bloky je 4,5 (server toleruje o něco víc). */
    private static boolean inReach(LocalPlayer player, BlockPos pos) {
        return new AABB(pos).distanceToSqr(player.getEyePosition()) <= 4.5 * 4.5;
    }

    private static boolean isMature(BlockState state) {
        if (!SEEDS.containsKey(state.getBlock())) {
            return false;
        }
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && age.getName().equals("age")) {
                return state.getValue(age) >= Collections.max(age.getPossibleValues());
            }
        }
        return false;
    }

    private static void msg(LocalPlayer player, String key) {
        player.sendSystemMessage(Component.translatable("message.megaharvest." + key));
    }
}
