package me.crafter.mc.lockettepro;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class DependencyProtocolLib {

    public static void setUpProtocolLib(Plugin plugin) {
        if (Config.protocollib) {
            addTileEntityDataListener(plugin);
            addMapChunkListener(plugin);
        }
    }

    public static void cleanUpProtocolLib(Plugin plugin) {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
                ProtocolLibrary.getProtocolManager().removePacketListeners(plugin);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addTileEntityDataListener(Plugin plugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.LOW, PacketType.Play.Server.TILE_ENTITY_DATA) {
            //PacketPlayOutTileEntityData -> ClientboundBlockEntityDataPacket
            @Override
            public void onPacketSending(PacketEvent event) {
                var player = event.getPlayer();
                var packet = event.getPacket();
                var blockPos = packet.getBlockPositionModifier().read(0);
                var block = player.getWorld().getBlockAt(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                if (!(block.getState() instanceof Sign)) return;
                var chatComponentArrays = packet.getChatComponentArrays();
                chatComponentArrays.write(0, onSignSend(event.getPlayer(), chatComponentArrays.read(0)));
            }
        });
    }

    public static void addMapChunkListener(Plugin plugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.LOW, PacketType.Play.Server.MAP_CHUNK) {
            //PacketPlayOutMapChunk - > ClientboundLevelChunkPacket -> ClientboundLevelChunkWithLightPacket
            @Override
            public void onPacketSending(PacketEvent event) {
                var player = event.getPlayer();
                PacketContainer packet = event.getPacket();
                List<InternalStructure> chunkData = packet.getStructures().read(0).getLists(InternalStructure.getConverter()).read(0);
                var chunkX = packet.getIntegers().read(0);
                var chunkZ = packet.getIntegers().read(1);
                var chunk = player.getWorld().getChunkAt(chunkX, chunkZ);
                for (InternalStructure struct : chunkData) {
                    var packedXZ = struct.getIntegers().read(0);
                    var y = struct.getIntegers().read(1);
                    var block = chunk.getBlock((packedXZ >> 4) & 15, y, ((packedXZ) & 15));
                    if (!(block.getState() instanceof Sign)) return;
                    var chatComponentArrays = struct.getChatComponentArrays();
                    chatComponentArrays.write(0, onSignSend(event.getPlayer(), chatComponentArrays.read(0)));
                }
            }
        });
    }

    public static WrappedChatComponent[] onSignSend(Player player, WrappedChatComponent[] chatComponent) {
        if (chatComponent.length == 0) return chatComponent;

        String raw_line1 = chatComponent[0].getJson();
        if (LocketteProAPI.isLockStringOrAdditionalString(Utils.getSignLineFromUnknown(raw_line1))) {
            // Private line
            String line1 = Utils.getSignLineFromUnknown(chatComponent[0].getJson());
            if (LocketteProAPI.isLineExpired(line1)) {
                chatComponent[0] = WrappedChatComponent.fromText(Config.getLockExpireString());
            } else {
                chatComponent[0] = WrappedChatComponent.fromText(Utils.StripSharpSign(line1));
            }
            // Other line
            for (int i = 1; i < chatComponent.length; i++) {
                String line = Utils.getSignLineFromUnknown(chatComponent[i].getJson());
                if (Utils.isUsernameUuidLine(line)) {
                    chatComponent[i] = WrappedChatComponent.fromText(Utils.getUsernameFromLine(line));
                }
            }
        }
        return chatComponent;
    }

}
