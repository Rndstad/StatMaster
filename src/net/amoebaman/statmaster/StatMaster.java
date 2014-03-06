package net.amoebaman.statmaster;

import java.io.File;

import net.amoebaman.utils.ChatUtils;
import net.amoebaman.utils.ChatUtils.ColorScheme;
import net.amoebaman.utils.maps.PlayerMap;
import net.amoebaman.utils.CommandController;
import net.amoebaman.statmaster.events.KillingSpreeEvent;
import net.amoebaman.statmaster.events.MultiKillEvent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.java.JavaPlugin;

public class StatMaster extends JavaPlugin implements Listener{
	
	protected static String mainDirectory;
	protected static File configFile, statsFile;
	protected static PlayerMap<Integer> killSpree, multiKill;
	protected static PlayerMap<Long> lastKill;
	
	private static StatHandler handler;
	
	public void onEnable(){
		
		getDataFolder().mkdirs();
		mainDirectory = getDataFolder().getPath();
		configFile = new File(mainDirectory + "/config.yml");
		statsFile = new File(mainDirectory + "/stats.yml");
		
		try{
			if(!configFile.exists())
				configFile.createNewFile();
			getConfig().load(configFile);
			getConfig().options().copyDefaults(true);
			getConfig().save(configFile);
			
			if(getConfig().getBoolean("use-mysql-database", false))
				handler = new SQLStatHandler(
						getConfig().getString("database-url", "localhost"),
						getConfig().getString("database-username", "root"),
						getConfig().getString("database-password", "null"));
			else{
				if(!statsFile.exists())
					statsFile.createNewFile();
				handler = new YAMLStatHandler(statsFile);
			}
		}
		catch(Exception e){ e.printStackTrace(); }
		
		handler.registerDefaultStats();
		
		killSpree = new PlayerMap<Integer>(0);
		multiKill = new PlayerMap<Integer>(0);
		lastKill = new PlayerMap<Long>(0L);
		
		Bukkit.getPluginManager().registerEvents(this, this);
		CommandController.registerCommands(new CommandListener());
		
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){ public void run(){
			for(Player player : lastKill.playerKeySet())
				if(player != null && System.currentTimeMillis() - lastKill.get(player) > 5000){
					if(multiKill.get(player) >= 2){
						Bukkit.broadcastMessage(ChatUtils.format("[[" + player.getName() + "]] has earned a [[" + multiKill.get(player) + "x]] multikill", ColorScheme.HIGHLIGHT));
						if(multiKill.get(player) > handler.getStat(player, "highest multikill"))
							handler.updateStat(player, "highest multikill", multiKill.get(player));
						new MultiKillEvent(player, multiKill.get(player)).callEvent();
					}
					multiKill.put(player, 0);
				}
		}}, 0, 20);
		
		if(handler instanceof YAMLStatHandler)
			Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){ public void run(){
				getLogger().info("Autosaving stats");
				handler.save();
			}}, 0, (int) (getConfig().getDouble("autosave-interval-minutes", 1) * 60 * 20));
		
	}
	
	public void onDisable(){
		handler.save();
	}
	
	public static StatHandler getHandler(){ return handler; }
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event){
		final Player victim = event.getEntity();
		if(victim == null)
			return;
		Player tempKiller = victim.getKiller();
		if(tempKiller == null){
			try{ tempKiller = (Player) ((Wolf)((EntityDamageByEntityEvent) victim.getLastDamageCause()).getDamager()).getOwner(); }
			catch(ClassCastException cce){}
			catch(NullPointerException npe){}
		}
		if(tempKiller == null && victim.getLastDamageCause() instanceof EntityDamageByEntityEvent)
			if(((EntityDamageByEntityEvent) victim.getLastDamageCause()).getCause() == DamageCause.CUSTOM){
				try{ tempKiller = (Player) ((EntityDamageByEntityEvent) victim.getLastDamageCause()).getDamager(); }
				catch(ClassCastException e){}
			}
		if(victim == null || tempKiller == null)
			return;
		final Player killer = tempKiller;
		
		handler.incrementStat(killer, "kills");
		handler.incrementStat(victim, "deaths");
		handler.incrementCommunityStat("kills");
		handler.updateStat(killer, "ktd", handler.getStat(killer, "kills") / handler.getStat(killer, "deaths"));
		handler.updateStat(victim, "ktd", handler.getStat(victim, "kills") / handler.getStat(victim, "deaths"));
		
		double eloChange = (double) (1.0 * handler.getStat(victim, "elo") / handler.getStat(killer, "elo") * 15);
		handler.adjustStat(victim, "elo_skill", -eloChange);
		handler.adjustStat(killer, "elo_skill", eloChange);
		handler.adjustCommunityStat("elo_change", eloChange);
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){ public void run(){
			
			killSpree.put(killer, killSpree.get(killer) + 1);
			multiKill.put(killer, multiKill.get(killer) + 1);
			lastKill.put(killer, System.currentTimeMillis());
			
			if(killSpree.get(killer) >= 3){
				Bukkit.broadcastMessage(ChatUtils.format(" --> " + killer.getName() + " is on a spree of [[" + killSpree.get(killer) + "]] kills", ColorScheme.NORMAL));
				if(killSpree.get(killer) > handler.getStat(killer, "longest spree"))
					handler.updateStat(killer, "longest spree", killSpree.get(killer));
				new KillingSpreeEvent(killer, killSpree.get(killer), false).callEvent();
			}
			
			if(killSpree.get(victim) >= 3){
				Bukkit.broadcastMessage(ChatUtils.format(" --> Ended [[" + victim.getName() + "]]'s spree at [[" + killSpree.get(victim) + "]] kills", ColorScheme.NORMAL));
				new KillingSpreeEvent(victim, killSpree.get(victim), true).callEvent();
			}
			killSpree.put(victim, 0);
			
		}});
	}
	
}
