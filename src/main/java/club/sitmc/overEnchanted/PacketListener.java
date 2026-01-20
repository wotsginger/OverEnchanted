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
        Integer level = AnvilListener.preparing.get(uuid);
        if (level == null) return;

        if (event.getPacketType() == PacketType.Play.Server.PLAYER_ABILITIES) {
            WrapperPlayServerPlayerAbilities wrapper =
                    new WrapperPlayServerPlayerAbilities(event);
            wrapper.setInCreativeMode(true);
            event.markForReEncode(true);
        }

        else if (event.getPacketType() == PacketType.Play.Server.WINDOW_PROPERTY) {
            WrapperPlayServerWindowProperty wrapper =
                    new WrapperPlayServerWindowProperty(event);
            if (wrapper.getId() == 0) {
                wrapper.setValue(level);
                event.markForReEncode(true);
            }
        }

        else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot wrapper =
                    new WrapperPlayServerSetSlot(event);
            event.getPostTasks().add(() ->
                    event.getUser().sendPacket(
                            new WrapperPlayServerWindowProperty(
                                    (byte) wrapper.getWindowId(), 0, level
                            )
                    )
            );
        }
    }

    public static WrapperPlayServerPlayerAbilities createExact(Player player) {
        return create(player, player.getGameMode() == GameMode.CREATIVE);
    }

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
