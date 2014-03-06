package net.amoebaman.statmaster;

import net.amoebaman.statmaster.events.KillingSpreeEvent;
import net.amoebaman.statmaster.events.MultiKillEvent;
import net.amoebaman.utils.ChatUtils;
import net.amoebaman.utils.ChatUtils.ColorScheme;
import net.amoebaman.utils.maps.PlayerMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class EventListener implements Listener {
	
	protected static final PlayerMap<Integer> killSpree = new PlayerMap<Integer>(0), multiKill = new PlayerMap<Integer>(0);
	protected static final PlayerMap<Long> lastKill = new PlayerMap<Long>(0L);
	
	public EventListener(){
		Bukkit.getScheduler().scheduleSyncRepeatingTask(StatMaster.getPlugin(StatMaster.class), new Runnable(){ public void run(){
			for(Player player : lastKill.playerKeySet())
				if(player != null && System.currentTimeMillis() - lastKill.get(player) > 5000){
					if(multiKill.get(player) >= 2){
						Bukkit.broadcastMessage(ChatUtils.format("[[" + player.getName() + "]] has earned a [[" + multiKill.get(player) + "x]] multikill", ColorScheme.HIGHLIGHT));
						if(multiKill.get(player) > StatMaster.getHandler().getStat(player, "highest multikill"))
							StatMaster.getHandler().updateStat(player, "highest multikill", multiKill.get(player));
						new MultiKillEvent(player, multiKill.get(player)).callEvent();
					}
					multiKill.put(player, 0);
				}
		}}, 0, 20);
	}
	
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
		
		StatMaster.getHandler().incrementStat(killer, "kills");
		StatMaster.getHandler().incrementStat(victim, "deaths");
		StatMaster.getHandler().incrementCommunityStat("kills");
		StatMaster.getHandler().updateStat(killer, "ktd", StatMaster.getHandler().getStat(killer, "kills") / StatMaster.getHandler().getStat(killer, "deaths"));
		StatMaster.getHandler().updateStat(victim, "ktd", StatMaster.getHandler().getStat(victim, "kills") / StatMaster.getHandler().getStat(victim, "deaths"));
		
		double eloChange = (double) (1.0 * StatMaster.getHandler().getStat(victim, "elo") / StatMaster.getHandler().getStat(killer, "elo") * 15);
		StatMaster.getHandler().adjustStat(victim, "elo_skill", -eloChange);
		StatMaster.getHandler().adjustStat(killer, "elo_skill", eloChange);
		StatMaster.getHandler().adjustCommunityStat("elo_change", eloChange);
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(StatMaster.getPlugin(StatMaster.class), new Runnable(){ public void run(){
			
			killSpree.put(killer, killSpree.get(killer) + 1);
			multiKill.put(killer, multiKill.get(killer) + 1);
			lastKill.put(killer, System.currentTimeMillis());
			
			if(killSpree.get(killer) >= 3){
				Bukkit.broadcastMessage(ChatUtils.format(" --> " + killer.getName() + " is on a spree of [[" + killSpree.get(killer) + "]] kills", ColorScheme.NORMAL));
				if(killSpree.get(killer) > StatMaster.getHandler().getStat(killer, "longest spree"))
					StatMaster.getHandler().updateStat(killer, "longest spree", killSpree.get(killer));
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
