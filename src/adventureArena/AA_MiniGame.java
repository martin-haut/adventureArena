package adventureArena;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

public class AA_MiniGame {


	private final int						id;
	private Vector							southEastMax			= null;
	private Vector							northWestMin			= null;
	private String							name					= "newMiniGame";
	private String							king					= null;
	private boolean							pvpDamage				= false;
	private ScoreMode						scoreMode				= ScoreMode.ScoreByCommand;
	private final Map<String, List<Vector>>	spawnPoints				= new HashMap<String, List<Vector>>();
	//private final List<ItemStack> spawnEquip = new ArrayList<ItemStack>();
	private final List<AA_SpawnEquip>		spawnEquipDefinitions	= new ArrayList<AA_SpawnEquip>();
	private final List<String>				allowedEditors			= new ArrayList<String>();
	private final List<Vector>				highScoreSignLocations	= new ArrayList<Vector>();
	private final List<AA_BlockTrigger>		rangedMonsterTriggers	= new ArrayList<AA_BlockTrigger>();
	private final List<AA_BlockTrigger>		startMonsterTriggers	= new ArrayList<AA_BlockTrigger>();

	private boolean							needsPersisting			= true;
	private boolean							needsEnvironmentBackup	= true;

	private boolean							lockedByEditSession		= false;
	private boolean							inProgress				= false;
	private World							world					= null;

	//play session.
	//private final HashMap<String, List<Player>> teamPlayerMappings = new HashMap<String, List<Player>>();
	private static Random					rnd						= new Random();
	private final HashMap<String, Integer>	initialJoiners			= new HashMap<String, Integer>();
	private boolean							isOver					= false;
	List<Player>							activePlayers			= new ArrayList<Player>();
	private Location						spectatorRespawnPoint	= null;



	//################## OBJECT LIFECYLCE ######################

	public AA_MiniGame(final int id, final World w) {
		this.id = id;
		world = w;
		if (w == null) {
			AA_MessageSystem.consoleError("world is NULL for " + this);
		}
	}

	public void persist() {
		set(AA_ConfigPaths.needsEnvironmentBackup, needsEnvironmentBackup);
		set(AA_ConfigPaths.lockedByEditSession, lockedByEditSession);
		set(AA_ConfigPaths.inProgress, inProgress);
		set(AA_ConfigPaths.worldName, world.getName());
		set(AA_ConfigPaths.southEastMax, getSouthEastMax());
		set(AA_ConfigPaths.northWestMin, getNorthWestMin());
		set(AA_ConfigPaths.name, name);
		set(AA_ConfigPaths.king, king);
		set(AA_ConfigPaths.pvpDamage, pvpDamage);
		set(AA_ConfigPaths.scoreMode, scoreMode.toString());
		//set(AA_ConfigPaths.spawnPoints, spawnPoints);
		for (String teamName: spawnPoints.keySet()) {
			set(AA_ConfigPaths.spawnPoints + "." + teamName, spawnPoints.get(teamName));
		}
		set(AA_ConfigPaths.spawnEquip, spawnEquipDefinitions);
		set(AA_ConfigPaths.rangedMonsterTriggers, rangedMonsterTriggers);
		set(AA_ConfigPaths.startMonsterTriggers, startMonsterTriggers);
		set(AA_ConfigPaths.allowedEditors, allowedEditors);
		set(AA_ConfigPaths.highScoreSignLocations, highScoreSignLocations);
		AA_MiniGameControl.saveMiniGameConfig();
		needsPersisting = false;
	}

	private void set(final String miniGameSubPath, final Object obj) {
		AA_MiniGameControl.getMiniGameConfig().set(id + "." + miniGameSubPath, obj);
		needsPersisting = true;
	}

	public static AA_MiniGame loadFromConfig(final int id) {
		FileConfiguration cfg = AA_MiniGameControl.getMiniGameConfig();
		String miniGameRootPath = String.valueOf(id);
		if (!cfg.contains(miniGameRootPath)) return null;
		AA_MiniGame mg = new AA_MiniGame(id, Bukkit.getWorld(cfg.getString(miniGameRootPath + "." + AA_ConfigPaths.worldName, "3gd")));
		mg.needsEnvironmentBackup = cfg.getBoolean(miniGameRootPath + "." + AA_ConfigPaths.needsEnvironmentBackup, true);
		mg.lockedByEditSession = cfg.getBoolean(miniGameRootPath + "." + AA_ConfigPaths.lockedByEditSession, false);
		mg.inProgress = cfg.getBoolean(miniGameRootPath + "." + AA_ConfigPaths.inProgress, false);
		mg.setSouthEastMax(cfg.getVector(miniGameRootPath + "." + AA_ConfigPaths.southEastMax, null));
		mg.setNorthWestMin(cfg.getVector(miniGameRootPath + "." + AA_ConfigPaths.northWestMin, null));
		mg.name = cfg.getString(miniGameRootPath + "." + AA_ConfigPaths.name, "newMiniGame");
		mg.king = cfg.getString(miniGameRootPath + "." + AA_ConfigPaths.king, null);
		mg.pvpDamage = cfg.getBoolean(miniGameRootPath + "." + AA_ConfigPaths.pvpDamage, false);
		mg.scoreMode = ScoreMode.valueOf(cfg.getString(miniGameRootPath + "." + AA_ConfigPaths.scoreMode, ScoreMode.ScoreByCommand.toString()));
		//fillSpawnPointMap(miniGameRootPath + "." + AA_ConfigPaths.spawnPoints, mg);
		ConfigurationSection spawns = cfg.getConfigurationSection(miniGameRootPath + "." + AA_ConfigPaths.spawnPoints);
		if (spawns != null) {
			for (String teamPath: spawns.getKeys(false)) {
				List<Vector> ts = new ArrayList<Vector>();
				fillCollection(miniGameRootPath + "." + AA_ConfigPaths.spawnPoints + "." + teamPath, ts);
				mg.spawnPoints.put(teamPath, ts);
			}
		}
		fillCollection(miniGameRootPath + "." + AA_ConfigPaths.spawnEquip, mg.getSpawnEquipDefinitions());
		fillCollection(miniGameRootPath + "." + AA_ConfigPaths.rangedMonsterTriggers, mg.rangedMonsterTriggers);
		fillCollection(miniGameRootPath + "." + AA_ConfigPaths.startMonsterTriggers, mg.startMonsterTriggers);
		fillCollection(miniGameRootPath + "." + AA_ConfigPaths.allowedEditors, mg.allowedEditors);
		fillCollection(miniGameRootPath + "." + AA_ConfigPaths.highScoreSignLocations, mg.highScoreSignLocations);
		mg.needsPersisting = false;
		return mg;
	}

	private static <T> void fillCollection(final String path, final Collection<T> collection) {
		List<?> cfgCollection = AA_MiniGameControl.getMiniGameConfig().getList(path);
		if (cfgCollection instanceof Collection<?>) {
			for (Object cfgElement: (Collection<?>) cfgCollection) {
				try {
					@SuppressWarnings ("unchecked") T element = (T) cfgElement;
					collection.add(element);
				}
				catch (ClassCastException e) {
					e.printStackTrace();
				}
			}
		}
	}



	//################## SETTER GETTER ######################

	public void setSouthEastMax(final Vector southEastMax) {
		this.southEastMax = southEastMax;
		calcSpectatorRespawnPoint();
		needsPersisting = true;
	}

	public void setNorthWestMin(final Vector northWestMin) {
		this.northWestMin = northWestMin;
		calcSpectatorRespawnPoint();
		needsPersisting = true;
	}

	private void calcSpectatorRespawnPoint() {
		if (getSouthEastMax() != null && getNorthWestMin() != null) {
			spectatorRespawnPoint = new Location(world,
					(getSouthEastMax().getX() + getNorthWestMin().getX()) / 2,
					getSouthEastMax().getY() - 1,
					(getSouthEastMax().getZ() + getNorthWestMin().getZ()) / 2
				);
		}
	}

	public Location getSpectatorRespawnPoint() {
		if (spectatorRespawnPoint == null) {
			calcSpectatorRespawnPoint();
		}
		if (spectatorRespawnPoint == null) {
			AA_MessageSystem.consoleError("can't calc spectatorRespawnPoint for '" + name + "', id: " + id);
		}
		return spectatorRespawnPoint;
	}

	public Vector getNorthWestMin() {
		return northWestMin;
	}

	public Vector getSouthEastMax() {
		return southEastMax;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
		needsPersisting = true;
	}

	public String getKing() {
		return king;
	}

	public void setKing(final String k) {
		king = k;
		persist();
	}

	public boolean isPvpDamage() {
		return pvpDamage;
	}

	public void setPvpDamage(final boolean pvpDamage) {
		this.pvpDamage = pvpDamage;
		needsPersisting = true;
	}

	public ScoreMode getScoreMode() {
		return scoreMode;
	}

	public void setScoreMode(final ScoreMode scoreMode) {
		this.scoreMode = scoreMode;
		needsPersisting = true;
	}

	public List<Vector> getSpawnPoints(final String team) {
		return spawnPoints.get(team);
	}

	public void addSpawnPoint(final String teamName, final Location loc) {
		addSpawnPoint(teamName, loc.toVector());
	}

	public void addSpawnPoint(final String teamName, final Vector loc) {
		if (!spawnPoints.containsKey(teamName)) {
			spawnPoints.put(teamName, new ArrayList<Vector>());
		}
		spawnPoints.get(teamName).add(loc);
		needsPersisting = true;
	}

	public void removeSpawnPoint(final String teamName, final Location loc) {
		removeSpawnPoint(teamName, loc.toVector());
	}

	public void removeSpawnPoint(final String teamName, final Vector loc) {
		if (!spawnPoints.containsKey(teamName)) {
			AA_MessageSystem.consoleError("failed to remove spawnpoint " + loc.toString() + " from " + this + ", team not found: " + teamName);
			return;
		}
		spawnPoints.get(teamName).remove(loc);
		needsPersisting = true;
	}

	public void addMonsterTrigger(final AA_BlockTrigger monsterTrigger) {
		if (monsterTrigger.isSpawnTrigger()) {
			startMonsterTriggers.add(monsterTrigger);
		}
		else {
			rangedMonsterTriggers.add(monsterTrigger);
		}
		needsPersisting = true;
	}

	public void removeMonsterTriggerBySignPos(final Vector monsterTriggerSignPos) {
		for (Iterator<AA_BlockTrigger> iter = startMonsterTriggers.iterator(); iter.hasNext();) {
			AA_BlockTrigger mt = iter.next();
			if (monsterTriggerSignPos.equals(mt.getSignPos())) {
				iter.remove();
				needsPersisting = true;
			}
		}
		for (Iterator<AA_BlockTrigger> iter = rangedMonsterTriggers.iterator(); iter.hasNext();) {
			AA_BlockTrigger mt = iter.next();
			if (monsterTriggerSignPos.equals(mt.getSignPos())) {
				iter.remove();
				needsPersisting = true;
			}
		}
	}

	public List<AA_BlockTrigger> getRangedMonsterTriggers() {
		return rangedMonsterTriggers;
	}

	public List<AA_BlockTrigger> getStartMonsterTriggers() {
		return startMonsterTriggers;
	}

	public List<AA_SpawnEquip> getSpawnEquipDefinitions() {
		return spawnEquipDefinitions;
	}

	public void addSpawnEquipDefinition(final AA_SpawnEquip item) {
		spawnEquipDefinitions.add(item);
		needsPersisting = true;
	}

	public void removeSpawnEquipBySignPos(final Vector spawnEquipSignPos) {
		for (Iterator<AA_SpawnEquip> iter = spawnEquipDefinitions.iterator(); iter.hasNext();) {
			AA_SpawnEquip se = iter.next();
			if (spawnEquipSignPos.equals(se.getSignPos())) {
				iter.remove();
				needsPersisting = true;
			}
		}
	}

	public ItemStack[] getSpawnEquip() {
		ItemStack[] items = new ItemStack[getSpawnEquipDefinitions().size()];
		for (int i = 0; i < items.length; i++) {
			items[i] = getSpawnEquipDefinitions().get(i).toItemStack();
		}
		return items;
	}

	public List<Player> getAllowedEditors() {
		ArrayList<Player> allowedEditors = new ArrayList<Player>();
		for (String name: this.allowedEditors) {
			Player p = Bukkit.getPlayer(name);
			if (p != null) {
				allowedEditors.add(p);
			}
		}
		return allowedEditors;
	}

	public void addAllowedEditor(final String name) {
		if (!allowedEditors.contains(name)) {
			allowedEditors.add(name);
			needsPersisting = true;
		}
	}

	public void removeAllowedEditor(final String name) {
		allowedEditors.remove(name);
		needsPersisting = true;
	}

	public boolean isEditableByPlayer(final Player player) {
		return allowedEditors.contains(player.getName()) || player.isOp();
	}

	public int getID() {
		return id;
	}


	public void registerHighScoreSignLocation(final Location location) {
		if (!highScoreSignLocations.contains(location.toVector())) {
			highScoreSignLocations.add(location.toVector());
			needsPersisting = true;
		}
		final AA_MiniGame mg = this;
		AdventureArena.executeDelayed(0.1, new Runnable() {

			@Override
			public void run() {
				AA_ScoreManager.updateHighScoreList(mg);
			}
		});
	}

	public void unRegisterHighScoreSignLocation(final Location location) {
		highScoreSignLocations.remove(location.toVector());
		needsPersisting = true;
	}

	public void removeHighScoreSignLocation(final Vector v) {
		highScoreSignLocations.remove(v);
		needsPersisting = true;
	}

	public List<Vector> getHighScoreSignLocations() {
		return highScoreSignLocations;
	}

	public World getWorld() {
		return world;
	}

	//################## DIRTY & LOCK FLAGS ######################

	public boolean needsPersisting() {
		return needsPersisting;
	}

	public boolean needsEnvironmentBackup() {
		return needsEnvironmentBackup;
	}

	public boolean isLockedByEditSession() {
		return lockedByEditSession;
	}

	public boolean isInProgress() {
		return inProgress;
	}



	public void setLockedByEditSession(final boolean locked) {
		lockedByEditSession = locked;
		if (locked) {
			needsEnvironmentBackup = true;
		}
		persist();
	}

	public void setInProgress(final boolean inProgress) {
		this.inProgress = inProgress;
		persist();
	}



	//################## Environment ######################

	public boolean doEnvironmentBackup() {
		AA_MessageSystem.sideNoteForGroup("creating environment backup from " + name + " (id:" + id + ")", getPlayersInArea());
		if (AA_TerrainHelper.saveMiniGameToSchematic(getNorthWestMin(), getSouthEastMax(), id, world)) {
			needsEnvironmentBackup = false;
			persist();
			return true;
		}
		else {
			AA_MessageSystem.errorForGroup("...backup failed!", getAllowedEditors());
			return false;
		}

	}

	public boolean restoreEnvironmentBackup() {
		AA_MessageSystem.sideNoteForGroup("restoring environment backup from " + name + " (id:" + id + ")", getPlayersInArea());
		if (AA_TerrainHelper.loadMinigameFromSchematic(id, world)) {
			needsEnvironmentBackup = false;
			persist();
			return true;
		}
		else {
			AA_MessageSystem.errorForGroup("...restore failed!", getAllowedEditors());
			return false;
		}
	}



	public void wipeEntities() {
		for (Entity e: world.getEntities()) {
			if (isInsideBounds(e.getLocation()) && e.getType() != EntityType.PLAYER) {
				//AA_MessageSystem.consoleWarn("WIPE: " + e.getName());
				e.remove();
			}
		}
	}



	//################## UTIL ######################

	boolean isInsideBounds(final Location loc) {
		if (getSouthEastMax() == null || getNorthWestMin() == null) return false;
		return getSouthEastMax().getX() + 1 >= loc.getX() && getNorthWestMin().getX() <= loc.getX()
			&& getSouthEastMax().getY() + 1 >= loc.getY() && getNorthWestMin().getY() <= loc.getY()
			&& getSouthEastMax().getZ() + 1 >= loc.getZ() && getNorthWestMin().getZ() <= loc.getZ();
	}

	public int getNumberOfSpawnPoints() {
		int n = 0;
		for (String team: spawnPoints.keySet()) {
			n += spawnPoints.get(team).size();
		}
		return n;
	}

	@Override
	public String toString() {
		return "[id=" + id + ", southEastMax=" + getSouthEastMax()
			+ ", northWestMin=" + getNorthWestMin() + ", name=" + name
			+ ", spectatorRespawnPoint=" + getSpectatorRespawnPoint().toVector()
			+ ", pvpDamage=" + pvpDamage + ", scoreMode=" + scoreMode
			+ ", #spawnPoints:" + getNumberOfSpawnPoints() + ", #spawnEquip:" + spawnEquipDefinitions.size()
			+ ", allowedEditors=" + allowedEditors.toString()
			+ ", #highScoreSignLocations: " + highScoreSignLocations.size()
			+ ", #startTriggers: " + startMonsterTriggers.size()
			+ ", #distanceTriggers: " + rangedMonsterTriggers.size()
			+ ", needsEnvironmentBackup: " + needsEnvironmentBackup
			+ ", inProgress: " + inProgress
			+ ", lockedByEditSession: " + lockedByEditSession
			+ ", isSoloPlayable: " + isSoloPlayable()
			+ ", numberOfPlayersRemaining: " + getNumberOfPlayersRemaining()
			+ ", initialJoiners: " + initialJoiners.toString()
			+ ", remainingTeams: " + getTeamsAsMap().toString()
			+ "]";
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof AA_MiniGame)) return false;
		return id == ((AA_MiniGame) obj).getID();
	}



	public int getInitialNumberOfPlayers() {
		int initialNumberOfPlayers = 0;
		for (int n: initialJoiners.values()) {
			initialNumberOfPlayers += n;
		}
		return initialNumberOfPlayers;
	}

	public int getInitialNumberOfTeams() {
		return initialJoiners.keySet().size();
	}


	void addPlayer(final String teamName, final Player p) {
		activePlayers.add(p);
		Team t = AA_TeamManager.getTeam(id + ":" + teamName, this);
		if (t.hasPlayer(p)) return;
		t.addPlayer(p);
		if (initialJoiners.containsKey(teamName)) {
			initialJoiners.put(teamName, initialJoiners.get(teamName) + 1);
		}
		else {
			initialJoiners.put(teamName, 1);
		}
	}

	void removePlayer(final Player p) {
		activePlayers.remove(p);
		Team t = AA_TeamManager.getTeam(p);
		if (t == null) return;
		t.removePlayer(p);
		if (t.getSize() == 0) {
			t.unregister();
		}
	}

	public void wipePlaySession() {
		initialJoiners.clear();
		activePlayers.clear();
		isOver = false;
		for (Team t: getTeams()) {
			AA_MessageSystem.consoleWarn("cleaning up team: " + t.getName());
			t.unregister();
		}
		setInProgress(false);
		for (AA_BlockTrigger mt: startMonsterTriggers) {
			mt.reset();
		}
		for (AA_BlockTrigger mt: rangedMonsterTriggers) {
			mt.reset();
		}
	}


	public int getNumberOfPlayersRemaining() {
		//		int totalPlayers = 0;
		//		for (Team t: getTeams()) {
		//			totalPlayers += t.getSize();
		//		}
		//		return totalPlayers;
		return activePlayers.size();
	}

	public List<Player> getPlayersRemaining() {
		return activePlayers;
	}

	//	public int numberOfPlayersLeft(final Team t) {
	//		if (t==null) return 0;
	//		return t.getSize();
	//	}
	public int getNumberOfEnemiesRemaining(final Team team) {
		int totalEnemies = 0;
		if (isTeamPlayModeActive()) {
			for (Team t: getTeams()) {
				if (!t.equals(team)) {
					totalEnemies += t.getSize();
				}
			}
		}
		else {
			totalEnemies = getNumberOfPlayersRemaining();
		}
		return totalEnemies;
	}



	public boolean isTeamPlayModeActive() {
		return getInitialNumberOfTeams() > 1;
	}

	public boolean isSoloPlayable() {
		return scoreMode == ScoreMode.ScoreByCommand || scoreMode == ScoreMode.KillsPerDeath && !pvpDamage;
	}

	public List<Team> getTeams() {
		String teamNameIdPrefix = String.valueOf(id) + ":";
		List<Team> miniGameTeams = new ArrayList<Team>();

		for (Team t: AA_TeamManager.scoreBoard.getTeams()) {
			if (t.getName().startsWith(teamNameIdPrefix)) {
				miniGameTeams.add(t);
			}
		}
		return miniGameTeams;
	}

	public HashMap<String, Integer> getTeamsAsMap() {
		HashMap<String, Integer> teams = new HashMap<String, Integer>();

		for (Team t: getTeams()) {
			teams.put(t.getName(), t.getSize());
		}
		return teams;
	}

	public boolean isVictory() {
		boolean soloPlayable = isSoloPlayable();
		boolean teamPlayModeActive = isTeamPlayModeActive();
		int numberOfPlayersRemaining = getNumberOfPlayersRemaining();
		return numberOfPlayersRemaining == 0 || !soloPlayable && (!teamPlayModeActive && numberOfPlayersRemaining <= 1 || teamPlayModeActive && getTeams().size() <= 1);
	}



	public boolean isOver() {
		return isOver;
	}

	public void setOver() {
		isOver = true;
	}

	public Player getRandomPlayer() {
		return activePlayers.size() == 0 ? null : activePlayers.get(rnd.nextInt(activePlayers.size()));
	}

	public List<Player> getSpectators() {
		List<Player> spectators = new ArrayList<Player>();
		for (Player p: Bukkit.getOnlinePlayers()) {
			if (AA_MiniGameControl.isWatchingMiniGames(p)) {
				if (equals(AA_MiniGameControl.getMiniGameForPlayer(p))) {
					spectators.add(p);
				}
			}
		}
		return spectators;
	}

	/**
	 * @return everybody inside minigame AABB (playing, editing, watching players)
	 */
	private Collection<Player> getPlayersInArea() {
		List<Player> playersInArea = new ArrayList<Player>();
		for (Player p: Bukkit.getOnlinePlayers()) {
			if (isInsideBounds(p.getLocation())) {
				playersInArea.add(p);
			}
		}
		return playersInArea;
	}


	public void roomReset() {
		name = "newMiniGame";
		king = null;
		pvpDamage = false;
		scoreMode = ScoreMode.ScoreByCommand;
		spawnPoints.clear();
		spawnEquipDefinitions.clear();
		allowedEditors.clear();
		rangedMonsterTriggers.clear();
		startMonsterTriggers.clear();
		needsPersisting = true;
		needsEnvironmentBackup = true;
		lockedByEditSession = false;
		inProgress = false;
		persist();
		AA_TerrainHelper.resetMiniGameRoom(this);
		doEnvironmentBackup();
	}


}
