package net.amoebaman.statmaster.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MultiKillEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
	
	private Player player;
	private int amount;

	public MultiKillEvent(Player player, int amount){
		this.player = player;
		this.amount = amount;
	}
	
	public Player getPlayer(){ return player; }
	public int getAmount(){ return amount; }
	
	public void callEvent(){ Bukkit.getServer().getPluginManager().callEvent(this); }
}
