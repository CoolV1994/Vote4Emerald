import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import com.vexsoftware.votifier.Votifier;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VoteListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Created by Vinnie on 12/17/14.
 */
public class Vote4Item implements VoteListener {
    /** The logger instance. */
    private Logger log = Logger.getLogger("Vote4Item");
    /** The vote reward list. */
    private ArrayList<ItemStack> items = new ArrayList<ItemStack>();

    public Vote4Item() {
        File configFile = new File("./plugins/Votifier/Vote4Item.txt");
        if (!configFile.exists())
        {
            try {
                configFile.createNewFile();
                // Default: Give 4 Emeralds
                ItemStack emeralds = new ItemStack(388, 4);
                items.add(emeralds);
                // Write Default Config
                FileWriter fw = new FileWriter(configFile);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write("# Vote4Item Configuration");
                bw.newLine();
                bw.write("# Item ID:Damage=Amount");
                bw.newLine();
                bw.write("388=4");
                bw.newLine();
                bw.close();
            } catch (IOException e) {
                log.warning("Creating default Vote4Item configuration.");
            }
        }
        else
        {
            BufferedReader br = null;
            try {
                String currentLine;
                br = new BufferedReader(new FileReader(configFile));
                while ((currentLine = br.readLine()) != null) {
                    // Ignore comment
                    if (currentLine.startsWith("#")) {
                        continue;
                    }
                    // Separate ID from Amount
                    String sItemID;
                    int amount;
                    if (currentLine.contains("=")) {
                        String[] split = currentLine.split("=");
                        sItemID = split[0];
                        amount = Integer.parseInt(split[1]);
                    } else {
                        sItemID = currentLine;
                        amount = 1;
                    }
                    // Separate Item ID and Damage ID
                    int itemID;
                    short damageID;
                    if (sItemID.contains(":")) {
                        String[] splitID = sItemID.split(":");
                        itemID = Integer.parseInt(splitID[0]);
                        damageID = Short.parseShort(splitID[1]);
                    } else {
                        itemID = Integer.parseInt(sItemID);
                        damageID = 0;
                    }
                    // Add ItemStack to List
                    items.add(new ItemStack(itemID, amount, damageID));
                }
            } catch (IOException e) {
                log.warning("Error loading Vote4Item configuration.");
            } finally {
                try {
                    if (br != null)
                        br.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public void voteMade(final Vote vote)
    {
        log.info("Received: " + vote);
        String username = vote.getUsername();
        if (username != null)
        {
            // Broadcast Message
            Bukkit.broadcastMessage(
                    "Thanks " + ChatColor.AQUA + username +
                            ChatColor.RESET + " for voting on " +
                            ChatColor.BLUE + vote.getServiceName() + ".");
            // Give Items
            final Player player = Bukkit.getPlayerExact(username);
            if (player != null)
            {
                PlayerInventory inventory = player.getInventory();
                final HashMap<Integer, ItemStack> failed = inventory.addItem(items.toArray(new ItemStack[items.size()]));
                if (failed.size() > 0) {
                    Bukkit.getScheduler().runTask(Votifier.getInstance(), new Runnable() {
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
                    });
                }
                player.sendMessage(ChatColor.GREEN + "Vote reward received.");
            }
        }
    }
}
