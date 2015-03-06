package adventureArena;

import java.util.ArrayList;

import org.bukkit.entity.Player;

public class AA_TeamChallenge {

	private final String teamName;
	final ArrayList<Player> players;

	public AA_TeamChallenge(final String teamName) {
		this.teamName = teamName;
		players = new ArrayList<Player>();
	}




	public String getTeamName() {
		return teamName;
	}


	public ArrayList<Player> getPlayers() {
		return players;
	}


	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof AA_TeamChallenge)) return false;
		return teamName.equals(((AA_TeamChallenge)obj).teamName);
	}

	@Override
	public String toString() {
		return "[AA_Team: " + teamName + " (" + players.size() + " players)]";
	}



}
