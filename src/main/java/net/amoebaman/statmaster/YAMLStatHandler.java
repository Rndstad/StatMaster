package net.amoebaman.statmaster;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.collect.Lists;

public class YAMLStatHandler extends StatHandler{
	
	final String community_section = "Community_Stats";
	
	File file;
	YamlConfiguration base;
	
	public YAMLStatHandler(File file){
		this.file = file;
		base = YamlConfiguration.loadConfiguration(file);
	}
	
	public void save(){
		try{ base.save(file); }
		catch(Exception e){ e.printStackTrace(); }
	}
	
	public void registerStat(Statistic newStat){
		if(!stats.contains(newStat)){
			stats.add(newStat);
			for(String category : newStat.categories)
				if(!categories.contains(category.toLowerCase()))
					categories.add(category.toLowerCase());
		}
	}
	
	public void registerCommunityStat(Statistic newStat){
		if(!communityStats.contains(newStat))
			communityStats.add(newStat);
	}
	
	public ConfigurationSection getSection(OfflinePlayer player){
		if(base.isConfigurationSection(player.getName()))
			return base.getConfigurationSection(player.getName());
		else
			return base.createSection(player.getName());
	}
	
	public ConfigurationSection getCommunitySection(){
		if(base.isConfigurationSection(community_section))
			return base.getConfigurationSection(community_section);
		else
			return base.createSection(community_section);
	}
	
	public double getStat(OfflinePlayer player, String statName){
		Statistic stat = getRegisteredStat(statName);
		if(stat == null)
			return 0;
		return getSection(player).getDouble(stat.underscore_name, stat.defaultValue);
	}

    public List<OfflinePlayer> getTopX(String statName, int x, boolean invert) {
		List<OfflinePlayer> players = new ArrayList<OfflinePlayer>();
		if(getRegisteredStat(statName) == null)
			return players;
		List<OfflinePlayer> all = Lists.newArrayList(Bukkit.getOfflinePlayers());
		Collections.sort(all, new StatBasedComparator(getRegisteredStat(statName), invert));
		for(int i = 0; i < (x < all.size() ? x : all.size()); i++)
			players.add(all.get(i));
		return players;
    }
    
	public double getCommunityStat(String statName){
		Statistic stat = getRegisteredCommunityStat(statName);
		if(stat == null)
			return 0;
		return getCommunitySection().getDouble(stat.underscore_name, stat.defaultValue);
		
	}
	
	public void updateStat(OfflinePlayer player, String statName, double newValue){
		Statistic stat = getRegisteredStat(statName);
		if(stat != null){
			int safety = (int) (newValue * 1000);
			newValue = ((float) safety) / 1000f;
			getSection(player).set(stat.underscore_name, newValue);
		}
	}
	
	public void updateCommunityStat(String statName, double newValue){
		Statistic stat = getRegisteredCommunityStat(statName);
		if(stat != null){
			int safety = (int) (newValue * 1000);
			newValue = ((float) safety) / 1000f;
			getCommunitySection().set(stat.underscore_name, newValue);
		}
	}


	
}
