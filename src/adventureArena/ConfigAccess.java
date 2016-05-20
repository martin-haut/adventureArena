package adventureArena;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;


public class ConfigAccess {

	private static final String			MINIGAMES_CONFIGNAME			= "miniGames.yml";
	private static File					minigamesConfigFile				= null;
	private static YamlConfiguration	minigamesConfig					= null;

	private static final String			HIGHSCORES_CONFIGNAME			= "highScores.yml";
	private static File					highScoreConfigFile				= null;
	private static YamlConfiguration	highScoreConfig					= null;

	private static final String			SAVED_INVENTORIES_CONFIG_NAME	= "savedPlayerInventories.yml";
	private static File					savedInventoriesConfigFile		= null;
	private static YamlConfiguration	savedInventoriesConfig			= null;



	public static FileConfiguration getPluginConfig() {
		return AdventureArena.getInstance().getConfig();
	}

	public static void savePluginConfig() {
		AdventureArena.getInstance().saveConfig();
	}



	public static FileConfiguration getMiniGameConfig() {
		if (minigamesConfig == null) {
			minigamesConfigFile = new File(getConfigFolder(), MINIGAMES_CONFIGNAME);
			minigamesConfig = YamlConfiguration.loadConfiguration(minigamesConfigFile);
		}
		return minigamesConfig;
	}

	public static void saveMiniGameConfig() {
		if (minigamesConfig != null) {
			try {
				minigamesConfig.save(minigamesConfigFile);
			}
			catch (IOException e) {
				MessageSystem.consoleError(MINIGAMES_CONFIGNAME + "cannot be overwritten or created");
				e.printStackTrace();
			}
		}
	}



	public static FileConfiguration getHighscoreConfig() {
		if (highScoreConfig == null) {
			highScoreConfigFile = new File(getConfigFolder(), HIGHSCORES_CONFIGNAME);
			highScoreConfig = YamlConfiguration.loadConfiguration(highScoreConfigFile);
		}
		return highScoreConfig;
	}

	public static void saveHighscoreConfig() {
		if (highScoreConfig != null) {
			try {
				highScoreConfig.save(highScoreConfigFile);
			}
			catch (IOException e) {
				MessageSystem.consoleError(HIGHSCORES_CONFIGNAME + "cannot be overwritten or created");
				e.printStackTrace();
			}
		}
	}



	public static FileConfiguration getSavedInventoriesConfig() {
		if (savedInventoriesConfig == null) {
			savedInventoriesConfigFile = new File(getConfigFolder(), SAVED_INVENTORIES_CONFIG_NAME);
			savedInventoriesConfig = YamlConfiguration.loadConfiguration(savedInventoriesConfigFile);
		}
		return savedInventoriesConfig;
	}


	public static void saveSavedInventoriesConfig() {
		if (savedInventoriesConfig != null) {
			try {
				savedInventoriesConfig.save(savedInventoriesConfigFile);
			}
			catch (IOException e) {
				MessageSystem.consoleError(SAVED_INVENTORIES_CONFIG_NAME + "cannot be overwritten or created");
				e.printStackTrace();
			}
		}
	}


	static File getConfigFolder() {
		File dataFolder = AdventureArena.getInstance().getDataFolder();
		if (!dataFolder.exists()) {
			dataFolder.mkdirs();
		}
		return dataFolder;
	}


}
