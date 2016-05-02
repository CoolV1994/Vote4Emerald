import com.vexsoftware.votifier.Votifier;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VoteListener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Vinnie on 12/17/14.
 */
public class Vote4Item implements VoteListener {
    // The logger instance
    private static final Logger LOGGER = Logger.getLogger("Vote4Item");
    // The vote reward list
    private final List<ItemStack> items;
    // Broadcast messages
    private final List<String> broadcast;
    // Private messages
    private final List<String> pm;
    // Reward messages
    private String rewardClaimed;
    private String rewardDropped;

    public Vote4Item() {
        pm = new ArrayList<String>();
        broadcast = new ArrayList<String>();
        items = new ArrayList<ItemStack>();
        rewardClaimed = ChatColor.GREEN + "Vote reward received.";
        rewardDropped = ChatColor.RED + "Could not add to inventory. Dropping " +
                    ChatColor.GOLD + "{item}" + ChatColor.RED + " on ground.";
        loadConfig();
    }

    @Override
    public void voteMade(final Vote vote) {
        LOGGER.log(Level.INFO, "[Vote4Item] Received: {0}", vote);
        String username = vote.getUsername();
        if (username == null) {
            return;
        }
        // Get Player
        Player player = Bukkit.getPlayer(username);
        // Lists with replacements
        ArrayList<String> broadcastMessage = new ArrayList<String>();
        ArrayList<String> privateMessage = new ArrayList<String>();
        // Broadcast Message
        for (String message : broadcast) {
            broadcastMessage.add(message
                    .replaceAll("\\{username\\}", username)
                    .replaceAll("\\{serviceName\\}", vote.getServiceName())
            );
        }
        // Private Messages
        for (String message : pm) {
            privateMessage.add(message
                    .replaceAll("\\{username\\}", username)
                    .replaceAll("\\{serviceName\\}", vote.getServiceName())
            );
        }
        // Give Item
        ItemStack[] reward = items.toArray(new ItemStack[items.size()]);
        Bukkit.getScheduler().runTask(Votifier.getInstance(),
                new RewardTask(player, reward,
                        broadcastMessage, privateMessage,
                        rewardClaimed, rewardDropped));
    }

    private ItemStack createItemStack(String line) {
        // Separate ID from Amount
        String sItemID;
        int amount;
        // Separate meta data from item data
        int end = line.length();
        if (line.contains("[")) {
            end = line.indexOf("[");
        }
        if (line.contains("{") && end > line.indexOf("{")) {
            end = line.indexOf("{");
        }
        String itemData = line.substring(0, end);
        // Get Item ID and Amount
        if (itemData.contains("=")) {
            String[] split = itemData.split("=");
            sItemID = split[0];
            amount = Integer.parseInt(split[1]);
        } else {
            sItemID = itemData;
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
        // Create the ItemStack
        ItemStack item = new ItemStack(itemID, amount, damageID);
        ItemMeta itemMeta = item.getItemMeta();
        // Add Enchantments
        if (line.contains("[") && line.contains("]")) {
            String enchantments = line.substring(line.indexOf('[') + 1, line.indexOf(']'));
            for (String enchantment : enchantments.split(";")) {
                int eid;
                int level;
                if (enchantment.contains(",")) {
                    String[] enchant = enchantment.split(",");
                    eid = Integer.parseInt(enchant[0]);
                    level = Integer.parseInt(enchant[1]);
                } else {
                    eid = Integer.parseInt(enchantment);
                    level = 1;
                }
                itemMeta.addEnchant(new EnchantmentWrapper(eid), level, true);
            }
        }
        // Add Meta Data
        if (line.contains("{") && line.contains("}")) {
            String metadata = line.substring(line.indexOf('{') + 1, line.indexOf('}'));
            for (String data : metadata.split(";")) {
                if (data.startsWith("name=")) {
                    String name = data.substring(5);
                    itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                    continue;
                }
                if (data.startsWith("lore=")) {
                    String lore = data.substring(5);
                    lore = ChatColor.translateAlternateColorCodes('&', lore);
                    itemMeta.setLore(Arrays.asList(lore.split("\\|")));
                }
            }
        }
        // Return final item
        item.setItemMeta(itemMeta);
        return item;
    }

    private void loadConfig() {
        File configFile = new File("plugins/Votifier/Vote4Item.txt");
        String currentLine;
        try {
            BufferedReader br = new BufferedReader(new FileReader(configFile));
            while ((currentLine = br.readLine()) != null) {
                // Ignore comment
                if (currentLine.startsWith("#")) {
                    continue;
                }
                // Reward Messages
                if (currentLine.startsWith("rewardClaimed=")) {
                    String message = currentLine.substring(14);
                    if (!message.isEmpty()) {
                        rewardClaimed = ChatColor.translateAlternateColorCodes('&', message);
                    }
                    continue;
                }
                if (currentLine.startsWith("rewardDropped=")) {
                    String message = currentLine.substring(14);
                    if (!message.isEmpty()) {
                        rewardDropped = ChatColor.translateAlternateColorCodes('&', message);
                    }
                    continue;
                }
                // Broadcast Message
                if (currentLine.startsWith("broadcast=")) {
                    String message = currentLine.substring(10);
                    if (!message.isEmpty()) {
                        broadcast.add(ChatColor.translateAlternateColorCodes('&', message));
                    }
                    continue;
                }
                // Private Message
                if (currentLine.startsWith("pm=")) {
                    String message = currentLine.substring(3);
                    if (!message.isEmpty()) {
                        pm.add(ChatColor.translateAlternateColorCodes('&', message));
                    }
                    continue;
                }
                // Item
                try {
                    items.add(createItemStack(currentLine));
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error creating vote reward.\n\tLine: " + currentLine, ex);
                }
            }
            br.close();
        } catch (FileNotFoundException ex) {
            LOGGER.warning("Could not find Vote4Item configuration. Creating default.");
            createDefaultConfig(configFile);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error loading Vote4Item configuration.", ex);
        }
    }

    private void createDefaultConfig(File configFile) {
        // Default vote message
        String msg = "Thanks &b{username}&r for voting on &9{serviceName}!";
        broadcast.add(ChatColor.translateAlternateColorCodes('&', msg));
        // Default: Give 4 Emeralds
        ItemStack emeralds = new ItemStack(388, 4);
        items.add(emeralds);
        try {
            // Write Default Config
            FileWriter fw = new FileWriter(configFile);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("# Vote4Item Configuration");
            bw.newLine();
            bw.write("#");
            bw.newLine();
            bw.write("# Broadcast Message");
            bw.newLine();
            bw.write("broadcast=" + msg);
            bw.newLine();
            bw.write("#");
            bw.newLine();
            bw.write("# Vote Rewards");
            bw.newLine();
            bw.write("# ItemID:Damage=Amount[EnchantID;EnchantID,Level]{name=Custom &cName;lore=Line 1|&4Line 2}");
            bw.newLine();
            bw.write("388=4");
            bw.newLine();
            bw.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error creating default Vote4Item configuration.", ex);
        }
    }
}
