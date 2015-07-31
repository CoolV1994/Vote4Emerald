import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import com.vexsoftware.votifier.Votifier;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VoteListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Created by Vinnie on 12/17/14.
 */
public class Vote4Item implements VoteListener {
    /** The logger instance. */
    private Logger log = Logger.getLogger("Vote4Item");
    /** The vote reward list. */
    private ArrayList<ItemStack> items = new ArrayList<ItemStack>();
    /** Broadcast messages. */
    private ArrayList<String> broadcast = new ArrayList<String>();
    /** Private messages. */
    private ArrayList<String> pm = new ArrayList<String>();

    public Vote4Item() {
        loadConfig();
    }

    @Override
    public void voteMade(final Vote vote)
    {
        log.info("[Vote4Item] Received: " + vote);
        String username = vote.getUsername();
        if (username != null)
        {
            // Broadcast Message
            for (String message : broadcast) {
                Bukkit.broadcastMessage(message
                        .replace("{username}", username)
                        .replace("{serviceName}", vote.getServiceName())
                );
            }
            // Get Player
            final Player player = Bukkit.getPlayer(username);
            if (player != null)
            {
                // Private Messages
                for (String message : pm) {
                    player.sendMessage(message
                            .replace("{username}", username)
                            .replace("{serviceName}", vote.getServiceName())
                    );
                }
                // Give Item
                PlayerInventory inventory = player.getInventory();
                final HashMap<Integer, ItemStack> failed = inventory.addItem(items.toArray(new ItemStack[items.size()]));
                if (failed.size() > 0) {
                    // No free space, Drop on ground
                    Bukkit.getScheduler().runTask(Votifier.getInstance(), new ItemDrop(failed, player));
                }
                player.sendMessage(ChatColor.GREEN + "Vote reward received.");
            }
        }
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
        String itemdata = line.substring(0, end);
        // Get Item ID and Amount
        if (itemdata.contains("=")) {
            String[] split = itemdata.split("=");
            sItemID = split[0];
            amount = Integer.parseInt(split[1]);
        } else {
            sItemID = itemdata;
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
        ItemMeta itemData = item.getItemMeta();
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
                itemData.addEnchant(new EnchantmentWrapper(eid), level, true);
            }
        }
        // Add Meta Data
        if (line.contains("{") && line.contains("}")) {
            String metadata = line.substring(line.indexOf('{') + 1, line.indexOf('}'));
            for (String data : metadata.split(";")) {
                if (data.startsWith("name=")) {
                    String name = data.substring(5);
                    if (name != null) {
                        itemData.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                    }
                    continue;
                }
                if (data.startsWith("lore=")) {
                    String lore = data.substring(5);
                    if (lore != null) {
                        lore = ChatColor.translateAlternateColorCodes('&', lore);
                        itemData.setLore(Arrays.asList(lore.split("\\|")));
                    }
                }
            }
        }
        // Return final item
        item.setItemMeta(itemData);
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
                // Broadcast Message
                if (currentLine.startsWith("broadcast=")) {
                    String msg = currentLine.substring(10);
                    if (msg != null) {
                        broadcast.add(ChatColor.translateAlternateColorCodes('&', msg));
                    }
                    continue;
                }
                // Private Message
                if (currentLine.startsWith("pm=")) {
                    String msg = currentLine.substring(3);
                    if (msg != null) {
                        pm.add(ChatColor.translateAlternateColorCodes('&', msg));
                    }
                    continue;
                }
                // Item
                try {
                    items.add(createItemStack(currentLine));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            br.close();
        } catch (FileNotFoundException ex) {
            log.warning("Could not find Vote4Item configuration. Creating default.");
            createDefaultConfig(configFile);
        } catch (IOException ex) {
            log.severe("Error loading Vote4Item configuration.");
            ex.printStackTrace();
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
            log.warning("Error creating default Vote4Item configuration.");
            ex.printStackTrace();
        }
    }
}
