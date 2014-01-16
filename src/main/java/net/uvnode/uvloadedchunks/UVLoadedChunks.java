package net.uvnode.uvloadedchunks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jcornwell
 */
public final class UVLoadedChunks extends JavaPlugin implements Listener {
    List<Chunk> _loadedChunks;
    Plugin _dynmap;
    DynmapAPI api;
    MarkerAPI _markerapi;
    MarkerSet _markerSet;
    Boolean _dynmapEnabled;
    double _borderOpacity = 0.5;
    int _borderWeight = 1;
    double _fillOpacity = 0.25;
    int _normalColor = 0x009900;
    
    @Override
    public void onEnable() {
        _loadedChunks = new ArrayList<>();

        try {
            PluginManager pm = getServer().getPluginManager();
            _dynmap = pm.getPlugin("dynmap");
            if (_dynmap == null) {
                _dynmapEnabled = false;
            }
            api = (DynmapAPI) _dynmap;
            
            if (_dynmap.isEnabled()) {
                _dynmapEnabled = true;
                activate();
            } else {
                _dynmapEnabled = false;
            }
        } catch (Exception e) {
            _dynmapEnabled = false;
        }
        
    }
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("uvloadedchunks")) {
            reset();
            sender.sendMessage("There are " + _loadedChunks.size() + " chunks loaded.");
            return true;
        }
        return false;
    }

    private String createChunkKey(Chunk chunk) {
        return String.format("%s.%d.%d", chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }
    @EventHandler
    private void onChunkLoadEvent(ChunkLoadEvent event) {
        _loadedChunks.add(event.getChunk());
        createMarker(event.getChunk(), createChunkKey(event.getChunk()));
        //getLogger().info("Loaded chunk at " + event.getChunk().getX() + ", " + event.getChunk().getZ());
    }
        
    @EventHandler
    private void onChunkUnloadEvent(ChunkUnloadEvent event) {
        if (event.isCancelled()) return;
        _loadedChunks.remove(event.getChunk());
        deleteMarker(createChunkKey(event.getChunk()));
        //getLogger().info("Unloaded chunk at " + event.getChunk().getX() + ", " + event.getChunk().getZ());
    }

    private void activate() {

        try {
            // Load marker API
            _markerapi = api.getMarkerAPI();
            if (_markerapi == null) {
                getLogger().severe("Cannot load marker API!");
                return;
            }
        } catch (Exception e) {
            getLogger().severe(e.getMessage());
                return;
        }


        // Get marker set
        _markerSet = _markerapi.getMarkerSet("uv.loadedchunks");

        // Create marker set if it doesn't exist, set label if it does
        if (_markerSet == null) {
            _markerSet = _markerapi.createMarkerSet("uv.loadedchunks", "Loaded Chunks", null, false);
            _markerSet.setHideByDefault(true);
        } else {
            _markerSet.setMarkerSetLabel("Loaded Chunks");
            _markerSet.getMarkers().clear();
        }

        // If creating failed, drop out. 
        if (_markerSet == null) {
            getLogger().severe("Error creating marker set.");
            return;
        }

        // Start listening for events.
        getServer().getPluginManager().registerEvents(this, this);
    }
    private void reset() {
        for (Chunk c : _loadedChunks) {
            deleteMarker(createChunkKey(c));
        }
        _loadedChunks.clear();
        
        for(World world : getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                _loadedChunks.add(chunk);
                createMarker(chunk, createChunkKey(chunk));
            }
        }
    }
    private void deleteMarker(String key) {
        // Retrieve the marker
        AreaMarker marker = _markerSet.findAreaMarker(key);
        // If the marker is found, delete it
        if (marker != null) {
            marker.deleteMarker();
        }
    }
    private void createMarker(Chunk chunk, String key) {
        
        double[] x = {(double)(chunk.getX()*16), (double)((chunk.getX()+1)*16)};
        double[] z = {(double)(chunk.getZ()*16), (double)((chunk.getZ()+1)*16)};

        // Create marker
        String label = "";
        AreaMarker marker = _markerSet.createAreaMarker(key, label, true, chunk.getWorld().getName(), x, z, false);
        if(marker == null) {
            getLogger().info("Error adding area marker " + key);
            return;
        }
        marker.setLineStyle(_borderWeight, _borderOpacity, _normalColor);
        marker.setFillStyle(_fillOpacity, _normalColor);
    }
}
