package me.crafter.mc.lockettepro;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class DependencyProtocolLib {

	public static void setUpProtocolLib(Plugin plugin){
		switch (LockettePro.getBukkitVersion()){
		case v1_13_R1:
			addTileEntityDataListener(plugin);
			addMapChunkListener(plugin);
			break;
		case v1_13_R2:
			addTileEntityDataListener(plugin);
			addMapChunkListener(plugin);
			break;
		case UNKNOWN:
		default:
			addTileEntityDataListener(plugin);
			addMapChunkListener(plugin);
			break;
		}
	}
	
	public static void cleanUpProtocolLib(Plugin plugin){
		try {
			if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null){
		    	ProtocolLibrary.getProtocolManager().removePacketListeners(plugin);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void addTileEntityDataListener(Plugin plugin){
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.LOW, PacketType.Play.Server.TILE_ENTITY_DATA) {
			@Override
			public void onPacketSending(PacketEvent event) {
				PacketContainer packet = event.getPacket();
				if (packet.getIntegers().read(0) != 9) return;
				NbtCompound nbtcompound = (NbtCompound) packet.getNbtModifier().read(0);
				String[] liness = new String[4];
				for (int i = 0; i < 4; i++){
					liness[i] = nbtcompound.getString("Text" + (i+1));
				}
				SignSendEvent signsendevent = new SignSendEvent(event.getPlayer(), liness);
				Bukkit.getPluginManager().callEvent(signsendevent);
				if (signsendevent.isModified()){
					for (int i = 0; i < 4; i++){
						nbtcompound.put("Text" + (i+1), signsendevent.getLine(i));
					}
				}
			}
		});
	}
	
	public static void addMapChunkListener(Plugin plugin){
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.LOW, PacketType.Play.Server.MAP_CHUNK) {
			@Override
			public void onPacketSending(PacketEvent event) {
				PacketContainer packet = event.getPacket();
				List<?> tileentitydatas = packet.getSpecificModifier(List.class).read(0);
				for (Object tileentitydata : tileentitydatas) {
					NbtCompound nbtcompound = NbtFactory.fromNMSCompound(tileentitydata);
					if (!"minecraft:sign".equals(nbtcompound.getString("id"))) continue;
					String[] liness = new String[4];
					for (int i = 0; i < 4; i++){
						liness[i] = nbtcompound.getString("Text" + (i+1));
					}
					SignSendEvent signsendevent = new SignSendEvent(event.getPlayer(), liness);
					Bukkit.getPluginManager().callEvent(signsendevent);
					if (signsendevent.isModified()){
						for (int i = 0; i < 4; i++){
							nbtcompound.put("Text" + (i+1), signsendevent.getLine(i));
						}
					}
				}
			}
		});
	}
	
	
	
}
