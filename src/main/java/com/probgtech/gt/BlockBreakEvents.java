package com.gimmecraft.gimmetrees;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakEvents implements Listener {
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e){
		Player p = e.getPlayer();
		if (Utils.isAxe(p.getItemInHand())){
			if (Utils.isLog(e.getBlock().getState())){
				if (p.isSneaking()){
					if (p.hasPermission("gimmetrees.use")){
						Utils utils = new Utils();
						utils.processTreeFeller(e.getBlock().getState(), p);
					} else {
						p.sendMessage(GimmeTrees.instance.getPrefix() + ChatColor.translateAlternateColorCodes('&', "&cYou do not have permission."));
					}
				} 
			}
		}
	}
}
