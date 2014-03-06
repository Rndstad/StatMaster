package net.amoebaman.statmaster;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class SQLStatHandler extends StatHandler{
	
	Connection conn = null;
	String url, user, pass;
	
	final String db_name = "statmaster";
	final String player_table = "player_stats";
	final String player_column = "player";
	final String community_table = "community_stats";
	final String community_column = "community";
	
	final String player_macro = "%player%";
	final String stat_macro = "%stat%";
	final String value_macro = "%value%";
	
	final String create_database = "CREATE DATABASE IF NOT EXISTS " + db_name;
	final String use_database = "USE " + db_name;
	
	final String create_player_table =
			"CREATE TABLE IF NOT EXISTS " + player_table + "( " +
					player_column + " VARCHAR(16) NOT NULL, " +
					"PRIMARY KEY (player) " +
					")";
	
	final String create_community_table =
			"CREATE TABLE IF NOT EXISTS " + community_table + "(dummy SMALLINT NOT NULL DEFAULT 0)";
	
	final String create_community_entry =
			"INSERT INTO " + community_table + "(dummy) VALUES(\"0\") " +
					"ON DUPLICATE KEY UPDATE dummy = \"0\"";
	
	final String add_player_stat =
			"ALTER TABLE " + player_table + " " +
					"ADD COLUMN " + stat_macro + " DECIMAL(64,3) NOT NULL DEFAULT " + value_macro;
	
	final String add_community_stat =
			"ALTER TABLE " + community_table + " " +
					"ADD COLUMN " + stat_macro + " DECIMAL(64,3) NOT NULL DEFAULT " + value_macro;
	
	final String add_player =
			"INSERT INTO " + player_table + "(" + player_column + ") " +
					"VALUES(\"" + player_macro + "\")";
	
	final String update_player_stat =
			"UPDATE " + player_table + " " +
					"SET " + stat_macro + " = \"" + value_macro + "\" " +
					"WHERE " + player_column + " = \"" + player_macro + "\"";
	
	final String update_community_stat =
			"UPDATE " + community_table + " " +
					"SET " + stat_macro + " = \"" + value_macro + "\"";
	
	final String get_player_stat =
			"SELECT " + stat_macro + " FROM " + player_table + " " +
					"WHERE " + player_column + " = \"" + player_macro + "\"";
	
	final String get_community_stat = 
			"SELECT " + stat_macro + " FROM " + community_table;
	
	final String get_all_ordered_by_stat =
			"SELECT " + player_column + ", " + stat_macro + " " +
					"FROM " + player_table + " " +
					"ORDER BY " + stat_macro + " DESC " +
					"LIMIT " + value_macro;
	
	public SQLStatHandler(String url, String user, String pass){
		
		this.url = url;
		this.user = user;
		this.pass = pass;
		
		try{
			Class.forName("com.mysql.jdbc.Driver");
			establishConnection();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public void save() {}
	
	public void validateConnection(){
		try {
			if(!conn.isValid(5000))
				establishConnection();
		}
		catch (SQLException e) { e.printStackTrace(); }
	}
	
	public void establishConnection(){
		try {
			conn = DriverManager.getConnection("jdbc:mysql://" + url + "/", user, pass);
			
			conn.prepareStatement(create_database).execute();
			conn.prepareStatement(use_database).execute();
			conn.prepareStatement(create_player_table).execute();
			conn.prepareStatement(create_community_table).execute();
			conn.prepareStatement(create_community_entry).execute();
		}
		catch (SQLException e) { e.printStackTrace(); }
	}
	
	public void registerStat(Statistic newStat) {
		validateConnection();
		if(!stats.contains(newStat)){
			stats.add(newStat);
			for(String category : newStat.categories)
				if(!categories.contains(category.toLowerCase()))
					categories.add(category.toLowerCase());
		}
		try{
			conn.prepareStatement(add_player_stat.replace(stat_macro, newStat.underscore_name).replace(value_macro, "" + newStat.defaultValue)).execute();
		}
		catch (SQLException e) {
			if(!e.getMessage().toLowerCase().contains("duplicate"))
				e.printStackTrace();
		}
	}
	
	@Override
	public void registerCommunityStat(Statistic newStat) {
		validateConnection();
		if(!communityStats.contains(newStat))
			communityStats.add(newStat);
		try{
			conn.prepareStatement(add_community_stat.replace(stat_macro, newStat.underscore_name).replace(value_macro, "" + newStat.defaultValue)).execute();
		}
		catch (SQLException e) {
			if(!e.getMessage().toLowerCase().contains("duplicate"))
				e.printStackTrace();
		}
	}
	
	@Override
	public double getStat(OfflinePlayer player, String statName) {
		validateConnection();
		Statistic stat = getRegisteredStat(statName);
		if(stat == null)
			return 0.0;
		try{
			ResultSet existsTest = conn.prepareStatement(get_player_stat.replace(player_macro, player.getName()).replace(stat_macro, "*")).executeQuery();
			if(!existsTest.first())
				conn.prepareStatement(add_player.replace(player_macro, player.getName())).execute();
			ResultSet result = conn.prepareStatement(get_player_stat.replace(player_macro, player.getName()).replace(stat_macro, stat.underscore_name)).executeQuery();
			if(result.first())
				return result.getDouble(stat.underscore_name);
			else
				return stat.defaultValue;
		}
		catch (SQLException e) {
			e.printStackTrace();
			return stat.defaultValue;
		}
	}
	
	public List<OfflinePlayer> getTopX(String statName, int x, boolean invert){
		validateConnection();
		List<OfflinePlayer> players = new ArrayList<OfflinePlayer>();
		Statistic stat = getRegisteredStat(statName);
		if(stat == null)
			return players;
		try{
			ResultSet result = conn.prepareStatement(get_all_ordered_by_stat.replace(stat_macro, stat.underscore_name).replace(value_macro, "" + x).replace(" DESC ", invert ? " ASC " : " DESC ")).executeQuery();
			while(result.next())
				players.add(Bukkit.getOfflinePlayer(result.getString(player_column)));
		}
		catch (SQLException e) { e.printStackTrace(); }
		
		return players;
	}
	
	@Override
	public double getCommunityStat(String statName) {
		validateConnection();
		Statistic stat = getRegisteredCommunityStat(statName);
		if(stat == null)
			return 0.0;
		try{
			ResultSet result = conn.prepareStatement(get_community_stat.replace(stat_macro, stat.underscore_name)).executeQuery();
			if(result.first())
				return result.getDouble(stat.underscore_name);
			else
				return stat.defaultValue;
		}
		catch (SQLException e) {
			e.printStackTrace();
			return stat.defaultValue;
		}
	}
	
	@Override
	public void updateStat(OfflinePlayer player, String statName, double newValue) {
		validateConnection();
		Statistic stat = getRegisteredStat(statName);
		if(stat == null)
			return;
		if(newValue == Double.NaN || newValue == Double.POSITIVE_INFINITY || newValue == Double.NEGATIVE_INFINITY)
			newValue = stat.defaultValue;
		try{
			ResultSet existsTest = conn.prepareStatement(get_player_stat.replace(player_macro, player.getName()).replace(stat_macro, "*")).executeQuery();
			if(!existsTest.first())
				conn.prepareStatement(add_player.replace(player_macro, player.getName())).execute();
			conn.prepareStatement(update_player_stat.replace(player_macro, player.getName()).replace(stat_macro, stat.underscore_name).replace(value_macro, "" + newValue)).executeUpdate();
		}
		catch (SQLException e) { e.printStackTrace(); }
	}
	
	@Override
	public void updateCommunityStat(String statName, double newValue) {
		validateConnection();
		Statistic stat = getRegisteredCommunityStat(statName);
		if(stat == null)
			return;
		if(newValue == Double.NaN || newValue == Double.POSITIVE_INFINITY || newValue == Double.NEGATIVE_INFINITY)
			newValue = stat.defaultValue;
		try{
			conn.prepareStatement(update_community_stat.replace(stat_macro, stat.underscore_name).replace(value_macro, "" + newValue)).executeUpdate();
		}
		catch (SQLException e) { e.printStackTrace(); }
	}
	
}
