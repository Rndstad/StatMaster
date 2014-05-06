package net.amoebaman.statmaster.events;

import net.amoebaman.statmaster.Statistic;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class StatChangeEvent extends Event implements Cancellable{

	private static final HandlerList handlers = new HandlerList();
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
	
	private Player player;
	private Statistic stat;
	private double oldValue, newValue;
	
	private boolean cancelled = false;

	public StatChangeEvent(Player player, Statistic stat, double oldValue, double newValue){
		this.player = player;
		this.stat = stat;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}
	
	public Player getPlayer(){ return player; }
	public Statistic getStat(){ return stat; }
	public double getOldValue(){ return oldValue; }
	public double getNewValue(){ return newValue; }
	public void setNewValue(double newValue){ this.newValue = newValue; } 
	
	public boolean isCancelled(){ return cancelled; }
	public void setCancelled(boolean cancelled){ this.cancelled = cancelled; }
	
	public void callEvent(){ Bukkit.getServer().getPluginManager().callEvent(this); }
	
}
