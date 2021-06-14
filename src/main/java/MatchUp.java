import net.dv8tion.jda.api.entities.User;

public class MatchUp {
	
	private int[] team1;
	private int[] team2;
	
	private User roleHost;
	private String roomKey;
	
	public MatchUp(int[] team1, int[] team2, User roleHost) {
		this.team1 = team1;
		this.team2 = team2;
		this.roleHost = roleHost;
	}

	protected void setRoomKey(String roomKey) {
		this.roomKey = roomKey;
	}

	protected int[] getTeam1() {
		return team1;
	}

	protected int[] getTeam2() {
		return team2;
	}

	protected User getRoleHost() {
		return roleHost;
	}

	protected String getRoomKey() {
		return roomKey;
	}
	
	protected String teamToString(int teamIndex) {
		String result = "";
		if (teamIndex == 1) {
			result += "``YOINC_acc" + correctNumberFormate(team1[0]) + "``";
			for (int i = 1; i < team1.length; i++) {
				result += " & ``YOINC_acc" + correctNumberFormate(team1[i]) + "``";
			}
			return result;
		} else {
			result += "``YOINC_acc" + correctNumberFormate(team2[0]) + "``";
			for (int i = 1; i < team2.length; i++) {
				result += " & ``YOINC_acc" + correctNumberFormate(team2[i]) + "``";
			}
			return result;
		}
	}
	
	protected String correctNumberFormate(int playerIndex) {
		if (playerIndex >= 1 && playerIndex <= 9) {
			return "0" + playerIndex;
		} else {
			return "" + playerIndex;
		}
	}
}
