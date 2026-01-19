package club.sitmc.overEnchanted;

import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AnvilListener implements Listener {

    private static final int MAX_ENCHANT_LEVEL = 10;
    public static final Map<UUID, Integer> preparing = new ConcurrentHashMap<>();

    @EventHandler
    public void onPrepare(PrepareAnvilEvent event) {
        if (!(event.getView() instanceof AnvilView view)) return;

        Player player = (Player) view.getPlayer();
        AnvilInventory inv = view.getTopInventory();

        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);

        if (left == null || right == null) {
            remove(player);
            return;
        }

        view.setMaximumRepairCost(100_000);

        ItemStack result = left.clone();

        Map<Enchantment, Integer> incoming = getIncomingEnchants(right);

        boolean hasEnchantMerge = !incoming.isEmpty();
        float cost = calculateInitialCost(left);
        int applied = 0;

        for (Map.Entry<Enchantment, Integer> entry : incoming.entrySet()) {
            Enchantment enchant = entry.getKey();

            int leftLevel  = getEnchantLevel(result, enchant);
            int rightLevel = entry.getValue();

            int finalLevel = calculateMergedLevel(leftLevel, rightLevel);
            if (finalLevel <= 0) continue;

            if (!canApplyEnchant(result, enchant, leftLevel)) continue;

            applyEnchant(result, enchant, finalLevel);
            applied++;

            // 经验成本：低等级线性，高等级指数
            if (finalLevel <= 4) {
                cost += finalLevel * 2.0f;
            } else {
                cost += Math.pow(finalLevel, 2.2) * 3.5f;
            }
        }

        String rename = view.getRenameText();
        boolean renaming = rename != null && !rename.isEmpty();

        if (applied == 0 && !renaming) return;

        if (renaming) {
            applyRename(result, rename);
            cost += 5; // 改名成本
        }

        event.setResult(result);

        int finalCost = Math.max(1, (int) cost);
        view.setRepairCost(finalCost);

        if (finalCost >= 40) {
            preparing.put(player.getUniqueId(), finalCost);
            PacketEvents.getAPI().getPlayerManager()
                    .sendPacket(player, PacketListener.create(player, true));
        } else {
            remove(player);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player
                && event.getView().getType() == org.bukkit.event.inventory.InventoryType.ANVIL) {
            remove(player);
        }
    }

    private void remove(Player player) {
        if (preparing.remove(player.getUniqueId()) != null) {
            PacketEvents.getAPI().getPlayerManager()
                    .sendPacket(player, PacketListener.createExact(player));
        }
    }

    private Map<Enchantment, Integer> getIncomingEnchants(ItemStack item) {
        if (item.getType() == Material.ENCHANTED_BOOK
                && item.getItemMeta() instanceof EnchantmentStorageMeta meta) {
            return meta.getStoredEnchants();
        }
        return item.getEnchantments();
    }

    private int getEnchantLevel(ItemStack item, Enchantment enchant) {
        if (item.getType() == Material.ENCHANTED_BOOK
                && item.getItemMeta() instanceof EnchantmentStorageMeta meta) {
            return meta.getStoredEnchantLevel(enchant);
        }
        return item.getEnchantmentLevel(enchant);
    }

    private int calculateMergedLevel(int left, int right) {
        if (right > left) return Math.min(right, MAX_ENCHANT_LEVEL);
        if (right == left && left > 0) return Math.min(left + 1, MAX_ENCHANT_LEVEL);
        return -1;
    }

    private boolean canApplyEnchant(ItemStack base, Enchantment enchant, int leftLevel) {
        if (!enchant.canEnchantItem(base)
                && base.getType() != Material.ENCHANTED_BOOK) {
            return false;
        }

        if (leftLevel == 0 && hasConflict(base, enchant)) return false;

        return true;
    }

    private void applyEnchant(ItemStack item, Enchantment enchant, int level) {
        if (item.getType() == Material.ENCHANTED_BOOK
                && item.getItemMeta() instanceof EnchantmentStorageMeta meta) {
            meta.addStoredEnchant(enchant, level, true);
            item.setItemMeta(meta);
        } else {
            item.addUnsafeEnchantment(enchant, level);
        }
    }

    private boolean hasConflict(ItemStack item, Enchantment check) {
        Map<Enchantment, Integer> existing =
                item.getType() == Material.ENCHANTED_BOOK
                        ? ((EnchantmentStorageMeta) item.getItemMeta()).getStoredEnchants()
                        : item.getEnchantments();

        for (Enchantment e : existing.keySet()) {
            if (e != check && e.conflictsWith(check)) return true;
        }
        return false;
    }

    private void applyRename(ItemStack item, String name) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
    }

    private float calculateInitialCost(ItemStack left) {
        float cost = 10f; // 基础成本
        Map<Enchantment, Integer> enchants =
                left.getType() == Material.ENCHANTED_BOOK
                        ? ((EnchantmentStorageMeta) left.getItemMeta()).getStoredEnchants()
                        : left.getEnchantments();

        for (int lvl : enchants.values()) {
            if (lvl <= 4) {
                cost += lvl * 2.0f; // 低等级线性
            } else {
                cost += Math.pow(lvl, 2.2) * 3.5f; // 高等级指数
            }
        }

        return cost;
    }
}
