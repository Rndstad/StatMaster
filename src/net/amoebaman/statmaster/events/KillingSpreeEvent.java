package net.amoebaman.statmaster.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class KillingSpreeEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
	
	private Player player;
	private int amount;
	private boolean ending;

	public KillingSpreeEvent(Player player, int amount, boolean ending){
		this.player = player;
		this.amount = amount;
		this.ending = ending;
	}
	
	public Player getPlayer(){ return player; }
	public int getSpree(){ return amount; }
	public boolean isEnded(){ return ending; }
	
	public void callEvent(){ Bukkit.getServer().getPluginManager().callEvent(this); }
	
}
