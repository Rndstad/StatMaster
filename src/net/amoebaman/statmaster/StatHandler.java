package net.amoebaman.statmaster;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bukkit.OfflinePlayer;

public abstract class StatHandler {
	
	List<Statistic> stats = new ArrayList<Statistic>();
	List<Statistic> communityStats = new ArrayList<Statistic>();
	List<String> categories = new ArrayList<String>();
	
	public void registerDefaultStats(){
		registerStat(new Statistic("Player Kills", 0, "default", "pvp", "combat", "kills"));
		registerStat(new Statistic("Player Deaths", 0, "default", "pvp", "combat", "deaths"));
		registerStat(new Statistic("KTD Ratio", 1, "default", "pvp"));
		registerStat(new Statistic("ELO Skill", 1000, "default", "pvp"));
		registerStat(new Statistic("Highest Multikill", 0, "default", "pvp"));
		registerStat(new Statistic("Longest Spree", 0, "default", "pvp"));
		
		registerStat(new Statistic("Monster Kills", 0, "pve", "combat", "kills"));
		registerStat(new Statistic("Animal Kills", 0, "pve", "combat", "kills"));
		registerStat(new Statistic("Monster Deaths", 0, "pve", "combat", "deaths"));
		registerStat(new Statistic("Natural Deaths", 0, "pve", "combat", "deaths"));
		
		registerStat(new Statistic("Blocks Mined", 0, "blocks", "tools"));
		registerStat(new Statistic("Blocks Chopped", 0, "blocks", "tools"));
		registerStat(new Statistic("Blocks Dug", 0, "blocks", "tools"));
		registerStat(new Statistic("Blocks Torched", 0, "blocks", "tools"));
		
		registerStat(new Statistic("Fish Caught", 0, "food", "tools"));
		registerStat(new Statistic("Cakes Baked", 0, "food", "crafting"));
		registerStat(new Statistic("Notch Apples Crafted", 0, "food", "crafting"));
		registerStat(new Statistic("Food Eaten", 0, "food", "survival"));
		
		registerCommunityStat(new Statistic("Player Kills", 0));
		registerCommunityStat(new Statistic("ELO Change", 0));
	}
	
	public abstract void save();
	
	public abstract void registerStat(Statistic newStat);
	
	public abstract void registerCommunityStat(Statistic newStat);
	
	public final Statistic getRegisteredStat(String statName){
		statName = statName.toLowerCase();
		for(Statistic each : stats)
			if(each.space_name.toLowerCase().equals(statName) || each.underscore_name.toLowerCase().equals(statName))
				return each;
		for(Statistic each : stats)
			if(each.space_name.toLowerCase().contains(statName) || each.underscore_name.toLowerCase().contains(statName))
				return each;
		return null;
	}
	
	public final Statistic getRegisteredCommunityStat(String statName){
		statName = statName.toLowerCase();
		for(Statistic each : communityStats)
			if(each.space_name.toLowerCase().equals(statName) || each.underscore_name.toLowerCase().equals(statName))
				return each;
		for(Statistic each : communityStats)
			if(each.space_name.toLowerCase().contains(statName) || each.underscore_name.toLowerCase().contains(statName))
				return each;
		return null;
	}
	
	public final List<Statistic> getRegisteredStats(){
		return stats;
	}
	
	public final String getCategory(String str){
		for(String category : categories)
			if(category.contains(str.toLowerCase()))
				return category;
		return null;
	}
	
	public final List<String> getRegisteredCategories(){
		return categories;
	}
	
	public abstract double getStat(OfflinePlayer player, String statName);
	
	public abstract List<OfflinePlayer> getTopX(String statName, int x, boolean invert);
	
	public abstract double getCommunityStat(String statName);
	
	public abstract void updateStat(OfflinePlayer player, String statName, double newValue);
	
	public abstract void updateCommunityStat(String statName, double newValue);
	
	public final void adjustStat(OfflinePlayer player, String statName, double adjustment){
		updateStat(player, statName, getStat(player, statName) + adjustment);
	}
	
	public final void adjustCommunityStat(String statName, double adjustment){
		updateCommunityStat(statName, getCommunityStat(statName) + adjustment);
	}
	
	public final void incrementStat(OfflinePlayer player, String statName){
		updateStat(player, statName, getStat(player, statName) + 1);
	}
	
	public final void incrementCommunityStat(String statName){
		updateCommunityStat(statName, getCommunityStat(statName) + 1);
	}
	
	public Comparator<OfflinePlayer> getComparator(String statName){
		return new StatBasedComparator(getRegisteredStat(statName), false);
	}
	
	public Comparator<OfflinePlayer> getInvertedComparator(String statName){
		return new StatBasedComparator(getRegisteredStat(statName), true);
	}
	
	public List<OfflinePlayer> purgeNewbies(List<OfflinePlayer> list){
		List<OfflinePlayer> toReturn = new ArrayList<OfflinePlayer>();
		for(OfflinePlayer player : list)
			if(getStat(player, "kills") >= 50)
				toReturn.add(player);
		return toReturn;
	}
	
	public class StatBasedComparator implements Comparator<OfflinePlayer>{
		
		private Statistic stat;
		private boolean invert;
		
		public StatBasedComparator(Statistic stat, boolean invert){
			this.stat = stat;
			if(stat == null)
				stat = getRegisteredStat("elo skill");
			this.invert = invert;
		}
		
		public int compare(OfflinePlayer p1, OfflinePlayer p2) {
			double s1 = getStat(p1, stat.space_name);
			double s2 = getStat(p2, stat.space_name);
			s1 = getStat(p1, "kills") < 50 ? 0 : s1;
			s2 = getStat(p2, "kills") < 50 ? 0 : s2;
			if(s1 < s2)
				return invert ? -1 : 1;
			if(s1 > s2)
				return invert ? 1 : -1;
			return 0;
		}
		
	}
	
}
