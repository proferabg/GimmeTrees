package com.gimmecraft.gimmetrees;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Tree;

public class Utils {
	
	public static int treeFellerThreshold = GimmeTrees.cfg.getInt("Options.Threshold");

    protected static boolean treeFellerReachedThreshold = false;
	
	private static final int[][] directions = {
        new int[] {-2, -1}, new int[] {-2, 0}, new int[] {-2, 1}, new int[] {-1, -2}, new int[] {-1, -1}, 
        new int[] {-1, 0}, new int[] {-1, 1}, new int[] {-1, 2}, new int[] { 0, -2}, new int[] { 0, -1},
        new int[] { 0, 1}, new int[] { 0, 2}, new int[] { 1, -2}, new int[] { 1, -1}, new int[] { 1, 0}, 
        new int[] { 1, 1}, new int[] { 1, 2}, new int[] { 2, -1}, new int[] { 2, 0}, new int[] { 2, 1},
	};
	
	protected static void processTree(BlockState blockState, Set<BlockState> treeFellerBlocks) {
        List<BlockState> futureCenterBlocks = new ArrayList<BlockState>();

        // Check the block up and take different behavior (smaller search) if it's a log
        if (handleBlock(blockState.getBlock().getRelative(BlockFace.UP).getState(), futureCenterBlocks, treeFellerBlocks)) {
            for (int[] dir : directions) {
                handleBlock(blockState.getBlock().getRelative(dir[0], 0, dir[1]).getState(), futureCenterBlocks, treeFellerBlocks);

                if (treeFellerReachedThreshold) {
                    return;
                }
            }
        }
        else {
            // Cover DOWN
            handleBlock(blockState.getBlock().getRelative(BlockFace.DOWN).getState(), futureCenterBlocks, treeFellerBlocks);
            // Search in a cube
            for (int y = -1; y <= 1; y++) {
                for (int[] dir : directions) {
                    handleBlock(blockState.getBlock().getRelative(dir[0], y, dir[1]).getState(), futureCenterBlocks, treeFellerBlocks);

                    if (treeFellerReachedThreshold) {
                        return;
                    }
                }
            }
        }

        // Recursive call for each log found
        for (BlockState futureCenterBlock : futureCenterBlocks) {
            if (treeFellerReachedThreshold) {
                return;
            }

            processTree(futureCenterBlock, treeFellerBlocks);
        }
    }
	
	protected static boolean handleDurabilityLoss(Set<BlockState> treeFellerBlocks, ItemStack inHand) {
        short durabilityLoss = 0;
        Material type = inHand.getType();

        for (BlockState blockState : treeFellerBlocks) {
            if (isLog(blockState)) {
                durabilityLoss += 1;
            }
        }

        handleDurabilityChange(inHand, durabilityLoss);
        return (inHand.getDurability() < type.getMaxDurability());
    }
	
	private static boolean handleBlock(BlockState blockState, List<BlockState> futureCenterBlocks, Set<BlockState> treeFellerBlocks) {
        if (treeFellerBlocks.contains(blockState)) {
            return false;
        }

        // Without this check Tree Feller propagates through leaves until the threshold is hit
        if (treeFellerBlocks.size() > treeFellerThreshold) {
            treeFellerReachedThreshold = true;
        }

        if (isLog(blockState)) {
            treeFellerBlocks.add(blockState);
            futureCenterBlocks.add(blockState);
            return true;
        }
        else if (isLeaves(blockState)) {
            treeFellerBlocks.add(blockState);
            return false;
        }
        return false;
    }
	
	public static boolean isLog(BlockState blockState) {
        switch (blockState.getType()) {
            case LOG:
            case LOG_2:
            case HUGE_MUSHROOM_1:
            case HUGE_MUSHROOM_2:
                return true;

            default:
                return false;
        }
    }
	
	public static boolean isLeaves(BlockState blockState) {
        switch (blockState.getType()) {
            case LEAVES:
            case LEAVES_2:
                return true;

            default:
                return false;
        }
    }
	
	public static boolean isAxe(ItemStack item) {
        Material type = item.getType();

        switch (type) {
            case DIAMOND_AXE:
            case GOLD_AXE:
            case IRON_AXE:
            case STONE_AXE:
            case WOOD_AXE:
                return true;

            default:
                return false;
        }
    }
	
	public static void handleDurabilityChange(ItemStack itemStack, int durabilityModifier) {
        handleDurabilityChange(itemStack, durabilityModifier, 1.0);
    }

    /**
     * Modify the durability of an ItemStack.
     *
     * @param itemStack The ItemStack which durability should be modified
     * @param durabilityModifier the amount to modify the durability by
     * @param maxDamageModifier the amount to adjust the max damage by
     */
    public static void handleDurabilityChange(ItemStack itemStack, int durabilityModifier, double maxDamageModifier) {
        Material type = itemStack.getType();
        short maxDurability = type.getMaxDurability();
        durabilityModifier = (int) Math.min(durabilityModifier / (itemStack.getEnchantmentLevel(Enchantment.DURABILITY) + 1), maxDurability * maxDamageModifier);

        itemStack.setDurability((short) Math.min(itemStack.getDurability() + durabilityModifier, maxDurability));
    }
    
    @SuppressWarnings("deprecation")
	public void processTreeFeller(BlockState blockState, Player player) {
        Set<BlockState> treeFellerBlocks = new HashSet<BlockState>();

        treeFellerReachedThreshold = false;

        processTree(blockState, treeFellerBlocks);

        // If the player is trying to break too many blocks
        if (treeFellerReachedThreshold) {
            treeFellerReachedThreshold = false;

            player.sendMessage(GimmeTrees.instance.getPrefix() + ChatColor.translateAlternateColorCodes('&', "&7This tree is too big."));
            return;
        }

        // If the tool can't sustain the durability loss
        if (!handleDurabilityLoss(treeFellerBlocks, player.getItemInHand())) {
            player.sendMessage(GimmeTrees.instance.getPrefix() + ChatColor.translateAlternateColorCodes('&', "&7Your axe splintered into a million pieces."));
            return;
        }

        dropBlocks(treeFellerBlocks);
        treeFellerReachedThreshold = false; // Reset the value after we're done with Tree Feller each time.
    }

    /**
     * Handles the dropping of blocks
     *
     * @param treeFellerBlocks List of blocks to be dropped
     */
    private void dropBlocks(Set<BlockState> treeFellerBlocks) {
        for (BlockState blockState : treeFellerBlocks) {
            Block block = blockState.getBlock();

            Material material = blockState.getType();

            if (material == Material.HUGE_MUSHROOM_1 || material == Material.HUGE_MUSHROOM_2) {
                dropItems(blockState.getLocation(), block.getDrops());
            } else {
                //TODO Remove this workaround when casting to Tree works again
                if (blockState.getData() instanceof Tree) {
                    Tree tree = (Tree) blockState.getData();
                    tree.setDirection(BlockFace.UP);
                }

                switch (material) {
                    case LOG:
                    case LOG_2:
                        dropItems(blockState.getLocation(), block.getDrops());
                        break;

                    case LEAVES:
                    case LEAVES_2:
                        dropItems(blockState.getLocation(), block.getDrops());
                        break;

                    default:
                        break;
                }
            }

            blockState.setType(Material.AIR);
            blockState.update(true);
        }
    }
    
    public static void dropItems(Location location, Collection<ItemStack> drops) {
        for (ItemStack drop : drops) {
            dropItem(location, drop);
        }
    }

    /**
     * Drop items at a given location.
     *
     * @param location The location to drop the items at
     * @param is The items to drop
     * @param quantity The amount of items to drop
     */
    public static void dropItems(Location location, ItemStack is, int quantity) {
        for (int i = 0; i < quantity; i++) {
            dropItem(location, is);
        }
    }

    /**
     * Drop an item at a given location.
     *
     * @param location The location to drop the item at
     * @param itemStack The item to drop
     * @return Dropped Item entity or null if invalid or cancelled
     */
    public static Item dropItem(Location location, ItemStack itemStack) {
        if (itemStack.getType() == Material.AIR) {
            return null;
        }

        return location.getWorld().dropItemNaturally(location, itemStack);
    }

}
