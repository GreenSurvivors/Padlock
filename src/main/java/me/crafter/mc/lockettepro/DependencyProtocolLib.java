package me.crafter.mc.lockettepro;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

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
            plugin.getLogger().log(Level.WARNING, "exception in cleaning up protocol lib!", e);
        }
    }

    public static String[] getSignRawLines(NbtCompound compound) {
        String[] rawLines = new String[4];
        for (int i = 0; i < 4; i++) {
            String line = compound.getString("Text" + (i + 1));
            rawLines[i] = Objects.requireNonNullElse(line, "");
        }
        return rawLines;
    }

    public static void setNbtSignText(NbtCompound compound, String[] rawLines) {
        for (int i = 0; i < 4; i++) {
            compound.put("Text" + (i + 1), rawLines[i]);
        }
    }

    public static void addTileEntityDataListener(Plugin plugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.LOW, PacketType.Play.Server.TILE_ENTITY_DATA) {
            //PacketPlayOutTileEntityData -> ClientboundBlockEntityDataPacket
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();

                NbtCompound signData = (NbtCompound) packet.getNbtModifier().read(0);

                if (signData == null || !signData.containsKey("Text1") || !signData.containsKey("Text2") || !signData.containsKey("Text3") || !signData.containsKey("Text4")) {
                    return;
                }

                String[] rawLines = getSignRawLines(signData);

                if (onSignSend(rawLines)) {
                    PacketContainer outgoingPacket = event.getPacket().shallowClone();

                    NbtCompound outgoingSignData = replaceSignData(signData, rawLines);

                    outgoingPacket.getNbtModifier().write(0, outgoingSignData);

                    event.setPacket(outgoingPacket);
                }
            }
        });
    }

    public static void addMapChunkListener(Plugin plugin) {
        EquivalentConverter<InternalStructure> converter = null;
        try {
            Field field = InternalStructure.class.getDeclaredField("CONVERTER");
            field.setAccessible(true);
            converter = (EquivalentConverter<InternalStructure>) field.get(null);
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().log(Level.WARNING, "exception in adding chunk listener!", e);
        }

        EquivalentConverter<InternalStructure> finalConverter = converter;

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.LOW, PacketType.Play.Server.MAP_CHUNK) {
            //PacketPlayOutMapChunk - > ClientboundLevelChunkPacket
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();

                // Only replace the outgoing packet if needed:
                PacketContainer outgoingPacket = null;
                List<InternalStructure> outgoingTileEntitiesInfo = null;

                List<InternalStructure> tileEntitiesInfo = packet.getStructures().read(0).getLists(finalConverter).read(0);
                for (int index = 0, size = tileEntitiesInfo.size(); index < size; index++) {
                    InternalStructure tileEntityInfo = tileEntitiesInfo.get(index);

                    NbtBase<?> compound = tileEntityInfo.getNbtModifier().read(0);
                    NbtCompound tileEntityData = compound == null ? null : NbtFactory.asCompound(compound);

                    if (tileEntityData == null) {
                        continue;
                    }

                    // Check if the tile entity is a sign:
                    if (!tileEntityData.containsKey("Text1") || !tileEntityData.containsKey("Text2") || !tileEntityData.containsKey("Text3") || !tileEntityData.containsKey("Text4")) {
                        continue;
                    }

                    String[] rawLines = getSignRawLines(tileEntityData);

                    if (onSignSend(rawLines)) {
                        if (outgoingPacket == null) {
                            outgoingPacket = packet.shallowClone();
                            outgoingTileEntitiesInfo = new ArrayList<>(tileEntitiesInfo);
                        }

                        NbtCompound outgoingSignData = replaceSignData(tileEntityData, rawLines);

                        Constructor<?> constructor = null;

                        for (Constructor<?> con : tileEntityInfo.getHandle().getClass().getDeclaredConstructors()) {
                            // We are looking for the only constructor with 4 parameters:
                            if (con.getParameterCount() == 4) {
                                con.setAccessible(true);
                                constructor = con;
                                break;
                            }
                        }
                        if (constructor == null) {
                            throw new RuntimeException("Could not find constructor in addMapChunkListener!");
                        }

                        Object constInstance = null;

                        try {
                            constInstance = constructor.newInstance(
                                    tileEntityInfo.getIntegers().read(0),
                                    tileEntityInfo.getIntegers().read(1),
                                    tileEntityInfo.getModifier().read(2),
                                    outgoingSignData.getHandle()
                            );
                        } catch (ReflectiveOperationException e) {
                            plugin.getLogger().log(Level.WARNING, "exception in chunk sending!", e);
                        }

                        InternalStructure newTileEntityInfo = finalConverter.getSpecific(constInstance);
                        outgoingTileEntitiesInfo.set(index, newTileEntityInfo);
                    }
                }

                if (outgoingPacket != null) {
                    packet.getStructures().read(0).getLists(finalConverter).write(0, outgoingTileEntitiesInfo);

                    event.setPacket(outgoingPacket);
                }
            }
        });
    }

    private static NbtCompound replaceSignData(NbtCompound previousSignData, String[] newSignText) {
        NbtCompound newSignData = NbtFactory.ofCompound(previousSignData.getName());

        // Copy the previous tile entity data (shallow copy):
        for (String key : previousSignData.getKeys()) {
            newSignData.put(key, previousSignData.getValue(key));
        }

        // Replace the sign text:
        setNbtSignText(newSignData, newSignText);

        return newSignData;
    }

    public static boolean onSignSend(String[] rawLines) {
        if (LocketteProAPI.isLockStringOrAdditionalString(Utils.getSignLineFromUnknown(rawLines[0]))) {
            // Private line
            String line1 = Utils.getSignLineFromUnknown(rawLines[0]);
            if (LocketteProAPI.isLineExpired(line1)) {
                rawLines[0] = "{\"text\":\"" + Config.getLockExpireString() + "\"}";
            } else {
                rawLines[0] = "{\"text\":\"" + Utils.StripSharpSign(line1) + "\"}";
            }
            // Other line
            for (int i = 1; i < 4; i++) {
                String line = Utils.getSignLineFromUnknown(rawLines[i]);
                if (Utils.isUsernameUuidLine(line)) {
                    rawLines[i] = "{\"text\":\"" + Utils.getUsernameFromLine(line) + "\"}";
                }
            }
            return true;
        }
        return false;
    }
}
