import java.io.*;
import java.util.*;
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
public class Vote4Item implements VoteListener {
    /** The logger instance. */
    private Logger log = Logger.getLogger("Vote4Item");
    HashMap<String, Integer> items = new HashMap<String, Integer>();

    public Vote4Item() {
        File configFile = new File("./plugins_mod/RainbowVotifier/Vote4Item.txt");
        if (!configFile.exists())
        {
            try {
                configFile.createNewFile();

                // Default: Give 4 Emeralds
                items.put("388", 4);

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
                log.warning("Error creating default Vote4Item configuration.");
            }
        }
        else
        {
            BufferedReader br = null;
            try {
                String currentLine;
                br = new BufferedReader(new FileReader(configFile));
                while ((currentLine = br.readLine()) != null) {
                    if (currentLine.startsWith("#")) {
                        continue;
                    }
                    String itemID;
                    int amount;
                    if (currentLine.contains("=")) {
                        String[] split = currentLine.split("=");
                        itemID = split[0];
                        amount = Integer.parseInt(split[1]);
                    } else {
                        itemID = currentLine;
                        amount = 1;
                    }
                    items.put(itemID, amount);
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
        if (vote.getUsername() != null)
        {
            // Broadcast Message
            Votifier.getServer().broadcastMessage(
                    "Thanks " + ChatColor.AQUA + vote.getUsername() +
                    ChatColor.RESET + " for voting on " +
                    ChatColor.BLUE + vote.getServiceName() + ".");
            MC_Player player = Votifier.getServer().getOnlinePlayerByName(vote.getUsername());
            if (player != null)
            {
                Iterator it = items.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) it.next();
                    String sItemID = entry.getKey();
                    int giveAmount = entry.getValue();

                    List<MC_ItemStack> inventory = player.getInventory();
                    int destinationSlot = -1;

                    int itemID;
                    int damageID;
                    if (sItemID.contains(":")) {
                        String[] splitID = sItemID.split(":");
                        itemID = Integer.parseInt(splitID[0]);
                        damageID = Integer.parseInt(splitID[1]);
                    } else {
                        itemID = Integer.parseInt(sItemID);
                        damageID = 0;
                    }

                    int stackAmount = giveAmount;
                    for (int invSlot = 0; invSlot < inventory.size(); invSlot++) {
                        if (destinationSlot == -1) {
                            MC_ItemStack is = inventory.get(invSlot);
                            // Free Slot
                            if ((is == null) || (is.getId() == 0)) {
                                destinationSlot = invSlot;
                            }
                            // Combine Stacks
                            if ((is.getId() == itemID) && (is.getDamage() == damageID) && (is.getCount() < (64 - stackAmount))) {
                                destinationSlot = invSlot;
                                stackAmount = is.getCount() + stackAmount;
                            }
                        }
                    }

                    MC_ItemStack voteReward = Votifier.getServer().createItemStack(itemID, stackAmount, damageID);

                    if (destinationSlot == -1) {
                        player.sendMessage(ChatColor.RED + "You do not have enough free space in your inventory. " + ChatColor.GREEN + "Dropping on ground.");
                        player.getWorld().dropItem(voteReward, player.getLocation(), null);
                        return;
                    }

                    inventory.set(destinationSlot, voteReward);
                    player.setInventory(inventory);
                    player.updateInventory();
                    player.sendMessage(ChatColor.GREEN + "You receive " + ChatColor.GOLD + giveAmount + " " + ChatColor.WHITE + voteReward.getFriendlyName());
                }
            }
        }
    }
}
