package net.amoebaman.statmaster;

import java.io.File;

import net.amoebaman.amoebautils.CommandController;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class StatMaster extends JavaPlugin {
	
	protected static String mainDirectory;
	protected static File configFile, statsFile;
	
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
		
		Bukkit.getPluginManager().registerEvents(new EventListener(), this);
		CommandController.registerCommands(new CommandListener());
		
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
	
}
