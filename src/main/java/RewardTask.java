import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.List;

/**
 * Created by Vinnie on 12/17/2014.
 */
public class RewardTask implements Runnable {
    // The player instance
    private Player player;
    // The vote reward
    private ItemStack[] items;
    // Broadcast messages
    private List<String> broadcast;
    // Private messages
    private List<String> pm;
    // Reward messages
    private String claimed;
    private String dropped;

    public RewardTask() {
    }

    public RewardTask(Player player, ItemStack[] items,
            List<String> broadcast, List<String> pm,
            String claimed, String dropped) {
        this.player = player;
        this.items = items;
        this.broadcast = broadcast;
        this.pm = pm;
        this.claimed = claimed;
        this.dropped = dropped;
    }

    @Override
    public void run() {
        // Broadcast Message
        for (String message : broadcast) {
            Bukkit.broadcastMessage(message);
        }
        // No player / Offline
        if (player == null) {
            return;
        }
        // Private Messages
        for (String message : pm) {
            player.sendMessage(message);
        }
        // Give Item
        Map<Integer, ItemStack> failed = player.getInventory().addItem(items);
        if (failed.isEmpty()) {
            player.sendMessage(claimed);
            return;
        }
        // No free space, Drop on ground
        Location location = player.getLocation();
        World world = player.getWorld();
        for (ItemStack item : failed.values()) {
            world.dropItem(location, item);
            player.sendMessage(dropped.replaceAll("\\{item\\}", item.getType().name()));
        }
        player.sendMessage(claimed);
    }
}
