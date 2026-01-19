package club.sitmc.overEnchanted;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowProperty;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class PacketListener extends PacketListenerAbstract {

    @Override
    public void onPacketSend(PacketSendEvent base) {
        if (!(base instanceof PacketPlaySendEvent event)) return;
        UUID uuid = event.getUser().getUUID();

        if (event.getPacketType() == PacketType.Play.Server.PLAYER_ABILITIES) {
            if (AnvilListener.preparing.containsKey(uuid)) {
                WrapperPlayServerPlayerAbilities wrapper = new WrapperPlayServerPlayerAbilities(event);
                wrapper.setInCreativeMode(true);
                event.markForReEncode(true);
            }
        }
        else if (event.getPacketType() == PacketType.Play.Server.WINDOW_PROPERTY) {
            Integer level = AnvilListener.preparing.get(uuid);
            if (level != null) {
                WrapperPlayServerWindowProperty wrapper = new WrapperPlayServerWindowProperty(event);
                if (wrapper.getId() == 0) {
                    wrapper.setValue(level);
                    event.markForReEncode(true);
                }
            }
        }
        else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            Integer level = AnvilListener.preparing.get(uuid);
            if (level != null) {
                WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
                event.getPostTasks().add(() ->
                        event.getUser().sendPacket(
                                new WrapperPlayServerWindowProperty((byte) wrapper.getWindowId(), 0, level)
                        )
                );
            }
        }
    }

    /**
     * 根据玩家真实的游戏模式创建一个恢复包
     */
    public static WrapperPlayServerPlayerAbilities createExact(Player player) {
        return create(player, player.getGameMode() == GameMode.CREATIVE);
    }

    /**
     * 创建一个自定义能力的包
     * @param creative 是否让客户端认为玩家处于创造模式
     */
    public static WrapperPlayServerPlayerAbilities create(Player player, boolean creative) {
        return new WrapperPlayServerPlayerAbilities(
                player.isInvulnerable(),
                player.isFlying(),
                player.getAllowFlight(),
                creative,
                player.getFlySpeed() / 2f,
                player.getWalkSpeed() / 2f
        );
    }

    public static void init() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListener());
    }
}