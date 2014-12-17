import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

/**
 * Created by Vinnie on 12/17/2014.
 */
public class ItemDrop implements Runnable {
    private HashMap<Integer, ItemStack> failed;
    private Player player;

    public ItemDrop() {
    }

    public ItemDrop(HashMap<Integer, ItemStack> failed, Player player) {
        this.failed = failed;
        this.player = player;
    }

    @Override
    public void run() {
        for (int index = 0; index < failed.size(); index++) {
            ItemStack drop = failed.get(index);
            Location loc = player.getLocation();
            player.getWorld().dropItem(loc, drop);
            player.sendMessage(ChatColor.RED + "Could not add to inventory. Dropping " +
                    ChatColor.GOLD + drop.getType().name() + ChatColor.RED + " on ground.");
        }
    }
}
