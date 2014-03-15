package net.amoebaman.statmaster;

import net.amoebaman.statmaster.events.KillingSpreeEvent;
import net.amoebaman.statmaster.events.MultiKillEvent;
import net.amoebaman.utils.GenUtil;
import net.amoebaman.utils.maps.PlayerMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;

public class EventListener implements Listener {
	
	protected static final PlayerMap<Integer> killSpree = new PlayerMap<Integer>(0), multiKill = new PlayerMap<Integer>(0);
	protected static final PlayerMap<Long> lastKill = new PlayerMap<Long>(0L);
	
	public EventListener(){
		Bukkit.getScheduler().scheduleSyncRepeatingTask(StatMaster.getPlugin(StatMaster.class), new Runnable(){ public void run(){
			for(Player player : lastKill.playerKeySet())
				if(player != null && System.currentTimeMillis() - lastKill.get(player) > 5000){
					new MultiKillEvent(player, multiKill.get(player)).callEvent();
					if(multiKill.get(player) >= 2 && multiKill.get(player) > StatMaster.getHandler().getStat(player, "highest multikill"))
						StatMaster.getHandler().updateStat(player, "highest multikill", multiKill.get(player));
					multiKill.put(player, 0);
				}
		}}, 0, 20);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void playerKillStatistics(PlayerDeathEvent event){
		final Player victim = event.getEntity();
		if(victim == null)
			return;
		if(victim.getLastDamageCause() instanceof EntityDamageByEntityEvent){
			LivingEntity culprit = GenUtil.getTrueCulprit((EntityDamageByEntityEvent) victim.getLastDamageCause());
			if(culprit instanceof Player){
				final Player killer = (Player) culprit;
				
				StatMaster.getHandler().incrementStat(killer, "player kills");
				StatMaster.getHandler().incrementStat(victim, "player deaths");
				StatMaster.getHandler().incrementCommunityStat("player kills");
				StatMaster.getHandler().updateStat(killer, "ktd", StatMaster.getHandler().getStat(killer, "player kills") / StatMaster.getHandler().getStat(killer, "player deaths"));
				StatMaster.getHandler().updateStat(victim, "ktd", StatMaster.getHandler().getStat(victim, "player kills") / StatMaster.getHandler().getStat(victim, "player deaths"));
				
				double eloChange = (double) (1.0 * StatMaster.getHandler().getStat(victim, "elo") / StatMaster.getHandler().getStat(killer, "elo") * 15);
				StatMaster.getHandler().adjustStat(victim, "elo_skill", -eloChange);
				StatMaster.getHandler().adjustStat(killer, "elo_skill", eloChange);
				StatMaster.getHandler().adjustCommunityStat("elo_change", eloChange);
				
				Bukkit.getScheduler().scheduleSyncDelayedTask(StatMaster.getPlugin(StatMaster.class), new Runnable(){ public void run(){
					
					killSpree.put(killer, killSpree.get(killer) + 1);
					multiKill.put(killer, multiKill.get(killer) + 1);
					lastKill.put(killer, System.currentTimeMillis());
					
					new KillingSpreeEvent(victim, killSpree.get(victim), true).callEvent();
					new KillingSpreeEvent(killer, killSpree.get(killer), false).callEvent();
					if(killSpree.get(killer) >= 3)
						if(killSpree.get(killer) > StatMaster.getHandler().getStat(killer, "longest spree"))
							StatMaster.getHandler().updateStat(killer, "longest spree", killSpree.get(killer));
					killSpree.put(victim, 0);
					
				}});
			}
			else if(culprit != null)
				StatMaster.getHandler().incrementStat(victim, "monster deaths");
		}
		else
			StatMaster.getHandler().incrementStat(victim, "natural deaths");
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void mobKillStatistics(EntityDeathEvent event){
		LivingEntity victim = event.getEntity();
		if(victim == null || victim instanceof Player)
			return;
		if(victim.getLastDamageCause() instanceof EntityDamageByEntityEvent){
			LivingEntity culprit = GenUtil.getTrueCulprit((EntityDamageByEntityEvent) victim.getLastDamageCause());
			if(culprit instanceof Player){
				if(victim instanceof Animals)
					StatMaster.getHandler().incrementStat((Player) culprit, "animal kills");
				if(victim instanceof Monster)
					StatMaster.getHandler().incrementStat((Player) culprit, "monster kills");
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockBreakStatistics(BlockBreakEvent event){
		Player player = event.getPlayer();
		Material tool = player.getItemInHand().getType();
		if(tool.name().contains("_PICKAXE"))
			StatMaster.getHandler().incrementStat(player, "blocks mined");
		else if(tool.name().contains("_AXE"))
			StatMaster.getHandler().incrementStat(player, "blocks mined");
		else if(tool.name().contains("_SPADE"))
			StatMaster.getHandler().incrementStat(player, "blocks dug");
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void pyromaniacStatistic(BlockIgniteEvent event){
		if(event.getCause() == IgniteCause.FLINT_AND_STEEL && event.getIgnitingEntity() != null)
			StatMaster.getHandler().incrementStat((Player) event.getIgnitingEntity(), "blocks torched");
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void fishingStatistic(PlayerFishEvent event){
		if(event.getState() == State.CAUGHT_FISH)
			StatMaster.getHandler().incrementStat(event.getPlayer(), "fish caught");
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void craftingStatistics(InventoryClickEvent event){
		if(event.getSlotType() == SlotType.RESULT){
			if(event.getCurrentItem().getType() == Material.CAKE)
				StatMaster.getHandler().incrementStat((Player) event.getWhoClicked(), "cakes baked");
			if(event.getCurrentItem().getType() == Material.GOLDEN_APPLE && event.getCurrentItem().getDurability() == 1)
				StatMaster.getHandler().adjustStat((Player) event.getWhoClicked(), "notch apples crafted", 1);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void foodStatistic(FoodLevelChangeEvent event){
		Player player = (Player) event.getEntity();
		if(event.getFoodLevel() > player.getFoodLevel())
			StatMaster.getHandler().adjustStat(player, "food eaten", event.getFoodLevel() - player.getFoodLevel());
	}

	
}
