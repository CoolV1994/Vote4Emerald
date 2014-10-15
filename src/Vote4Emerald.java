import java.util.List;
import java.util.logging.Logger;

import PluginReference.ChatColor;
import PluginReference.MC_ItemStack;
import PluginReference.MC_Player;
import com.vexsoftware.votifier.Votifier;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VoteListener;

/**
 * Created by Vinnie on 10/15/14.
 */
public class Vote4Emerald implements VoteListener {
    /** The logger instance. */
    private Logger log = Logger.getLogger("Vote4Emerald");

    public Vote4Emerald() {}

    @Override
    public void voteMade(final Vote vote) {
        log.info("Received: " + vote);
        if (vote.getUsername() != null) {
            Votifier.getServer().broadcastMessage(
                    "Thanks " + ChatColor.AQUA + vote.getUsername() + ChatColor.RESET + " for voting on " + vote.getServiceName() + ".");
            MC_Player player = Votifier.getServer().getOnlinePlayerByName(vote.getUsername());
            if (player != null) {
                List<MC_ItemStack> inventory = player.getInventory();
                int freeSlot = -1;
                int stackAmt = 1;
                for (int i = 0; i < inventory.size(); i++)
                {
                    if (freeSlot == -1) {
                        MC_ItemStack is = inventory.get(i);
                        if ((is == null) || (is.getId() == 0)) {
                            freeSlot = i;
                        }
                        if ((is.getId() == 388) && (is.getCount() < 64)) {
                            freeSlot = i;
                            stackAmt = is.getCount() + 1;
                        }
                    }
                }
                if (freeSlot == -1)
                {
                    player.sendMessage(ChatColor.RED + "You do not have enough free space in your inventory.");
                    return;
                }

                MC_ItemStack is = Votifier.getServer().createItemStack(388, stackAmt, 0);
                inventory.set(freeSlot, is);
                player.setInventory(inventory);
                player.updateInventory();
                player.sendMessage(ChatColor.GREEN + "You receive an Emerald.");
            }
        }
    }
}
