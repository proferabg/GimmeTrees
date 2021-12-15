package com.gimmecraft.gimmetrees;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


public class GimmeTrees extends JavaPlugin {
	
	static PluginDescriptionFile pluginyml;
	Logger logger;
	public static GimmeTrees instance;
	public static FileConfiguration cfg;
	public int threadID;
	Map<Player, Integer> plist = new HashMap<Player, Integer>();
	
	public void onEnable() {
		//Get logger before anything else
		logger = Logger.getLogger("Minecraft");
		
		//save instance
		instance = this;
		
		//get description
		pluginyml = getDescription();

		//get config
		cfg = getConfig();
		cfg.options().copyDefaults(true);
		saveConfig();
		configThread();
		
		//log authors
		logger.info(ChatColor.stripColor(getPrefix()) + "Plugin by proferabg");
		
		//register command listener
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new BlockBreakEvents(), this);
    
    //metrics
    new Metrics(this, 13603);

	}

	public void onDisable() {
		Bukkit.getScheduler().cancelTasks(this);
		saveConfig();
	}
	
	public String getPrefix(){
		return ChatColor.translateAlternateColorCodes('&', "&7[&5GimmeTrees&7] ");
	}
	
	public static GimmeTrees getInstance(){
		return instance;
	}
	
	public void configThread(){
		threadID = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				saveConfig();
				logger.info(ChatColor.stripColor(getPrefix()) + "Saving config.");
			}
		}, 0, 300 * 20L);
	}
}
