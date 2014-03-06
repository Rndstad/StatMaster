package net.amoebaman.statmaster;

import java.util.ArrayList;
import java.util.List;

import net.amoebaman.utils.CommandController.CommandHandler;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandListener{
	
	@CommandHandler(cmd = "stats")
	public String[] stats(CommandSender sender, String[] args){
		OfflinePlayer target = null;
		String focus = "default";
		
		if(args.length > 0){
			
			if(args[0].equalsIgnoreCase("help"))
				return new String[]{ ChatColor.ITALIC + "Available categories: " + StatMaster.getHandler().getRegisteredCategories() };
			
			else{
				target = Bukkit.getPlayer(args[0]);
				if(target == null){
					target = Bukkit.getOfflinePlayer(args[0]);
					if(!target.hasPlayedBefore())
						target = null;
				}	
				if(target == null)
					focus = args[0];
			}
		}
		
		if(sender instanceof Player && target == null)
			target = (Player) sender;
		
		if(target == null)
			return new String[]{ ChatColor.ITALIC + "Player could not be found" };
		
		if(args.length > 1)
			focus = args[1];
		String category = StatMaster.getHandler().getCategory(focus);
		
		if(category == null && args.length >= 1)
			return new String[]{
					ChatColor.ITALIC + focus + " is not a valid category",
					ChatColor.ITALIC + "Available categories: " + StatMaster.getHandler().getRegisteredCategories() };
		
		List<String> toReturn = new ArrayList<String>();
		toReturn.add(ChatColor.ITALIC + "Displaying " + focus + " stats for " + target.getName());
		for(Statistic stat : StatMaster.getHandler().getRegisteredStats())
			for(String each : stat.categories)
				if(each.equalsIgnoreCase(focus) || each.toLowerCase().contains(focus.toLowerCase())){
					double value = StatMaster.getHandler().getStat(target, stat.space_name);
					toReturn.add(ChatColor.ITALIC + "  " + stat.space_name + ": " + (Math.abs(value - (int) value) < 0.001 ? (int) value : value));
				}
		return toReturn.toArray(new String[0]);
	}
	
	@CommandHandler(cmd = "leaderboards")
	public void leaderboards(CommandSender sender, String[] args){
		if(args.length == 0){
			sender.sendMessage(ChatColor.ITALIC + "Available statistics: " + StatMaster.getHandler().getRegisteredStats());
			return;
		}
		Statistic stat = StatMaster.getHandler().getRegisteredStat(args[0].replace('_', ' '));
		if(stat == null){
			if(!args[0].equals("help"))
				sender.sendMessage(ChatColor.ITALIC + args[0]+ " is not a valid statistic");
			sender.sendMessage(ChatColor.ITALIC + "Available statistics: " + StatMaster.getHandler().getRegisteredStats());
			return;
		}
		int x = 10;
		if(args.length > 1){
			try{ x = Integer.parseInt(args[1]); }
			catch(NumberFormatException nfe){}
		}
		List<OfflinePlayer> top = StatMaster.getHandler().getTopX(stat.space_name, x, false);
		sender.sendMessage(ChatColor.ITALIC + "Displaying top " + x + ChatColor.ITALIC + " ranking players for " + stat.space_name);
		for(int i = 0; i < x && i < top.size(); i++){
			OfflinePlayer player = top.get(i);
			sender.sendMessage("  " + ChatColor.ITALIC + (i + 1) + ") " + player.getName() + " - " + StatMaster.getHandler().getStat(player, stat.space_name)); 
		}
	}
	
	@CommandHandler(cmd = "loserboards")
	public void loserboards(CommandSender sender, String[] args){
		if(args.length == 0){
			sender.sendMessage(ChatColor.ITALIC + "Available statistics: " + StatMaster.getHandler().getRegisteredStats());
			return;
		}
		Statistic stat = StatMaster.getHandler().getRegisteredStat(args[0].replace('_', ' '));
		if(stat == null){
			if(!args[0].equals("help"))
				sender.sendMessage(ChatColor.ITALIC + args[0]+ " is not a valid statistic");
			sender.sendMessage(ChatColor.ITALIC + "Available statistics: " + StatMaster.getHandler().getRegisteredStats());
			return;
		}
		int x = 10;
		if(args.length > 1){
			try{ x = Integer.parseInt(args[1]); }
			catch(NumberFormatException nfe){}
		}
		List<OfflinePlayer> bottom = StatMaster.getHandler().getTopX(stat.space_name, x, true);
		sender.sendMessage(ChatColor.ITALIC + "Displaying bottom " + ChatColor.BOLD + x + ChatColor.ITALIC + " ranking players for " + ChatColor.BOLD + stat.space_name);
		for(int i = 0; i < x && i < bottom.size(); i++){
			OfflinePlayer player = bottom.get(i);
			sender.sendMessage("  " + ChatColor.ITALIC + (i + 1) + ") " + player.getName() + " - " + StatMaster.getHandler().getStat(player, stat.space_name)); 
		}
	}
	
	@CommandHandler(cmd = "export-file-stats-to-sql")
	public void exportStats(CommandSender sender, String[] args){
		if(!(StatMaster.getHandler() instanceof SQLStatHandler)){
			sender.sendMessage("Not currently using an SQL-based stat handler");
			return;
		}
		YAMLStatHandler yaml = new YAMLStatHandler(StatMaster.statsFile);
		yaml.stats = StatMaster.getHandler().stats;
		boolean first = true;
		for(String name : yaml.base.getKeys(false)){
			if(!name.equals(yaml.community_section)){
				OfflinePlayer player = Bukkit.getOfflinePlayer(name);
				for(String statName : yaml.getSection(player).getKeys(false)){
					StatMaster.getHandler().updateStat(player, statName, yaml.getStat(player, statName));
					if(first)
						System.out.println(player + " -> " + statName + " -> " + yaml.getRegisteredStat(statName) + " -> " + yaml.getStat(player, statName));
				}
			}
			first = false;
		}
	}
	
}
