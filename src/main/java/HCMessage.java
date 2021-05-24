import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

public class HCMessage extends ListenerAdapter {

    private Properties properties;

    private Map<Integer, User> claimedAccounts = new HashMap<Integer, User>();
    private List<int[]> teams = new ArrayList<int[]>();
    private List<MatchUp> matches = new ArrayList<MatchUp>();
    private Map<User, Integer> roleHostTeam = new HashMap<User, Integer>();
    private List<Integer> loserPlayers = new ArrayList<Integer>();
    private List<Integer> winnerPlayers = new ArrayList<Integer>();

    private int playerFreePass = -1;

    private TextChannel GLOBAL_CHANNEL = null;

    private boolean hasStarted = false;

    public HCMessage(Properties properties) {
        super();
        this.properties = properties;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        User user = event.getAuthor();
        boolean isAdmin = isAdmin(user);
        if (event.getMessage() != null) {
            if (event.isFromType(ChannelType.PRIVATE)) {
                    switch (handleMessage(event.getMessage().getContentDisplay(), isAdmin)) {
                        case 1:
                            if (GLOBAL_CHANNEL != null) {
                                if(!hasStarted) {
                                    handleClaim(event, channel, user, event.getAuthor().getName());
                                } else {
                                    channel.sendMessage("Team matching is already complete. You can not claim an account now.").queue();
                                }
                            } else {
                                channel.sendMessage("Setup not complete.").queue();
                            }
                            break;
                        case 2:
                            if (GLOBAL_CHANNEL != null) {
                            //TODO: say
                            } else {
                                channel.sendMessage("Setup not complete.").queue();
                            }
                            break;
                        case 8:
                            handleAdd(event);
                        case 12:
                        	shareRoomKey(user, event.getMessage().getContentDisplay());
                        	break;
                        case 17:
                        	handleResult(user, event.getMessage().getContentDisplay());
                        	break;
                        default:
                            break;
                    }
            } else {
                switch (handleMessage(event.getMessage().getContentDisplay(), isAdmin)) {
                    case 3:
                        handleStart(event);
                        break;
                    case 9:
                        handleSetup(event);
                        break;
                    case 18:
                    	handleCorrectResult(event.getMessage().getContentDisplay());
                    default:
                        break;
                }
            }
        }
    }

    protected int handleMessage(String message, boolean isAdmin) {
        if (message.toLowerCase().startsWith("/claim")) {
            return 1;
        }
        if (message.toLowerCase().startsWith("/say")) {
            return 2;
        }
        if(message.toLowerCase().startsWith("/start")) {
            if(isAdmin) {
                return 3;
            }
        }
        if(message.toLowerCase().startsWith("/add")) {
            if(isAdmin) {
                return 8;
            }
        }
        if (message.toLowerCase().equals("/setup")) {
            if (isAdmin) {
                return 9;
            }
        }
        if (message.toLowerCase().startsWith("/send")) {
        	return 12;
        }
        if (message.toLowerCase().startsWith("/result")) {
        	return 17;
        }
        if (message.toLowerCase().startsWith("/correctresult")) {
        	if (isAdmin) {
        		return 18;
        	}
        }
        //DEFINE OTHER COMMANDS HERE

        return 0;
    }

    protected void handleStart(MessageReceivedEvent event) {
        String message = event.getMessage().getContentDisplay();
        String[] splitMessage = message.split(" ");
        if (!loserPlayers.isEmpty()) {
        	for (Integer i : loserPlayers) {
        		String userName = claimedAccounts.get(i).getName();
        		claimedAccounts.remove(i);
        		System.out.println("[REMOVED] - " + userName + "'s account YOINC_acc" + correctNumberFormate(i) 
        		+ "has been removed from claimedAccounts"); //DEBUG
        	}
        }
        teams = new ArrayList<int[]>();
        if(splitMessage.length == 2) {
            try {
                int teamSize = Integer.parseInt(splitMessage[1]);
                createTeams(teamSize);
            } catch (NumberFormatException ex) {
                //not a number
            }
        } else if(splitMessage.length == 1) {
            createTeams(1);
        }
        if (!teams.isEmpty()) {
        	playerFreePass = -1;
        	matches = new ArrayList<MatchUp>();
        	roleHostTeam = new HashMap<User, Integer>();
        	winnerPlayers = new ArrayList<Integer>();
        	loserPlayers = new ArrayList<Integer>();
        	generateMatchUp();
        	notifyRoleHost();
        	releaseMatchUp();
        }
    }

    protected void handleSetup(MessageReceivedEvent event) {
        claimedAccounts = new HashMap<Integer, User>();
        teams = new ArrayList<int[]>();
        matches = new ArrayList<MatchUp>();
    	roleHostTeam = new HashMap<User, Integer>();
    	winnerPlayers = new ArrayList<Integer>();
    	loserPlayers = new ArrayList<Integer>();
        playerFreePass = -1;
        hasStarted = false;

        GLOBAL_CHANNEL = event.getGuild().getTextChannelById(event.getChannel().getId());
        GLOBAL_CHANNEL.sendMessage("Setup complete. Using ``" + GLOBAL_CHANNEL.getName() + "`` for updates.").queue();
        //Additional Setup Methods required
    }

    protected void handleClaim(MessageReceivedEvent event, MessageChannel channel, User user, String userName) {
        int possibleClaim = (int) returnNumber(event.getMessage().getContentDisplay(), 0);
        if (possibleClaim <= 0 || possibleClaim > 14) {
            switch (possibleClaim) {
                case 0:
                    channel.sendMessage("That's not a valid account.").queue();
                    break;
                case -1:
                    channel.sendMessage("That's not a valid number.").queue();
                    break;
                case -2:
                    channel.sendMessage("Please send the message in the right format ``/claim <number>``.").queue();
                    break;
                default:
                    channel.sendMessage("Your number is too high. The max. you can claim is 14.").queue();
                    break;
            }
        } else {
            if (checkIfFree(possibleClaim)) {
                if (checkIfUserDidntClaimed(user)) {
                    claimedAccounts.put(possibleClaim, user);
                    channel.sendMessage("You successfully claimed ``YOINC_acc" + correctNumberFormate(possibleClaim) + "``.\n\n" +
                            "Please login into your Steam client using the following data:\n" +
                            "Username: ``YOINC_acc" + correctNumberFormate(possibleClaim) + "``\n" +
                            "Password: ``" + properties.getProperty("password.yoinc_acc" + correctNumberFormate(possibleClaim)) + "``\n\n" +
                            "Please follow the guide on the website to set up family sharing.").queue();
                    GLOBAL_CHANNEL.sendMessage("``YOINC_acc" + correctNumberFormate(possibleClaim) + "`` claimed.").queue();
                    System.out.println("[CLAIM] - " + userName + " claimed YOINC_acc" + correctNumberFormate(possibleClaim)); //DEBUG
                } else {
                    channel.sendMessage("You already claimed an account.").queue();
                }
            } else {
                channel.sendMessage("Account already claimed. Please try claiming another.").queue();
            }
        }
    }

    protected void handleAdd(MessageReceivedEvent event) {
        long newAdmin = (long) returnNumber(event.getMessage().getContentDisplay(), 1);
        if(String.valueOf(newAdmin).length() == 17 || String.valueOf(newAdmin).length() == 18) {
            properties.setProperty("bot.admin", properties.getProperty("bot.admin") + "," + newAdmin);
        }
    }
    
    private void sendDirectMessage(User user, String content) {
    	if (user != null) {
    		user.openPrivateChannel()
        	.flatMap(channel -> channel.sendMessage(content))
        	.queue();
    	}
    }

    protected Object returnNumber(String message, int type) {
        String[] messageParts = message.split(" ");
        if (messageParts.length == 2) {
            try {
                switch (type) {
                    case 0:
                        int returnIntValue = Integer.parseInt(messageParts[1]);
                        if (returnIntValue <= 0) {
                            return 0;
                        } else {
                            return returnIntValue;
                        }
                    case 1:
                        long returnLongValue = Long.parseLong(messageParts[1]);
                        if (returnLongValue <= 0) {
                            return 0;
                        } else {
                            return returnLongValue;
                        }
                    default:
                        //This will never happen.
                        return -3;
                }
            } catch (NumberFormatException ex) {
                return -1;
            }
        } else {
            return -2;
        }
    }

    protected boolean isAdmin(User user) {
        final String ADMIN = properties.getProperty("bot.admin");
        if(ADMIN != null && ADMIN.contains(",")) {
            String[] adminList = ADMIN.split(",");
            for(String admin : adminList) {
                if(user.getId().equals(admin)) {
                    return true;
                }
            }
        } else {
            if (user.getId().equals(ADMIN)) {
                return true;
            }
        }
        return false;
    }

    //admin should consider teamSize such that
    //(playerAcc.size() / teamSize) % 2 == 0
    protected void createTeams(int teamSize) {
    	Set<Integer> playerAcc = claimedAccounts.keySet().stream().collect(Collectors.toSet());
    	Random rand = new Random();
    	if (playerAcc.size() % teamSize == 1) { 
    		int randomIndex = rand.nextInt(playerAcc.size());
    		playerFreePass = getPlayerAcc(playerAcc, randomIndex);
    		playerAcc.remove(playerFreePass);
    	}  
    	if ((playerAcc.size() / teamSize) % 2 != 0) {
    		GLOBAL_CHANNEL.sendMessage("Please reconsider the team size.\n" +
                    "Teams have no been created.").queue();
    		return;
    	}
    	int teamAmount = playerAcc.size() / teamSize;
    	for (int i = 0; i < teamAmount; i++) {
    	    String builtMessage = "";
    		int[] playerTeam = new int[teamSize];
    		for (int j = 0; j < teamSize; j++) {
    			int teamMate = getPlayerAcc(playerAcc, rand.nextInt(playerAcc.size()));
    			if(builtMessage.isEmpty()) {
                    builtMessage = "``YOINC_acc" + correctNumberFormate(teamMate) + "``";
                } else {
                    builtMessage = builtMessage + " & ``YOINC_acc" + correctNumberFormate(teamMate) + "``";
                }
    			playerTeam[j] = teamMate;
    			playerAcc.remove(teamMate);
    		}
    		teams.add(playerTeam);
    		GLOBAL_CHANNEL.sendMessage("__Team " + (i + 1) + ":__\n" +
                    builtMessage + "\n").queue();
    	}
    	if(playerFreePass != -1) {
            GLOBAL_CHANNEL.sendMessage("Player YOINC_acc" + correctNumberFormate(playerFreePass) + " gets a free pass.").queue();
        }
    	hasStarted = true;
    }
    
    private int getPlayerAcc(Set<Integer> players, int index) {
    	Iterator<Integer> itr = players.iterator();
    	int idx = 0;
    	while (itr.hasNext()) {
    		int player = itr.next();
    		if (idx == index) {
    			return player;
    		}
    		idx++;
    	}
    	return -1;
    }
    
    protected void generateMatchUp() {
    	List<int[]> teamsClone = teams.stream().collect(Collectors.toList());
    	int matchesAmount = teamsClone.size() / 2;
    	Random rand = new Random();
    	
    	for (int i = 0; i < matchesAmount; i++) {
    		int team1Index = rand.nextInt(teamsClone.size());
    		int[] team1 = teamsClone.remove(team1Index);
    		int team2Index = rand.nextInt(teamsClone.size());
    		int[] team2 = teamsClone.remove(team2Index);
    		User roleHost = claimedAccounts.get(team1[0]);
    		MatchUp match = new MatchUp(team1, team2, roleHost);
    		matches.add(match);
    		roleHostTeam.put(roleHost, matches.size()-1);
    	}
    	
    }
    
    protected void notifyRoleHost() {
    	Set<User> roleHostSet = roleHostTeam.keySet();
    	Iterator<User> itr = roleHostSet.iterator();
    	String msg = "**Please create a lobby and send the invite URL in this channel using ``/send <URL>``**";
    	while (itr.hasNext()) {
    		User roleHost = itr.next();
    		sendDirectMessage(roleHost, msg);
    	}
    }
    
    protected void releaseMatchUp() {
    	String buildMessage = " \n";
    	for (int i = 0; i < matches.size(); i++) {
    		MatchUp match = matches.get(i);
    		buildMessage += "__Match " + (i + 1) + ":__ \n";
    		buildMessage += match.teamToString(1) + " vs " + match.teamToString(2) + "\n";
    	}
    	buildMessage += "**A Host for each match has been selected and notified to create a lobby.** \n"
    			+ "**Players will be receiving the Invite URL shortly.**";
    	GLOBAL_CHANNEL.sendMessage(buildMessage).queue();
    }
    
    protected void shareRoomKey(User roleHost, String msg) {
    	String[] messageParts = msg.split(" ");
    	if ((messageParts.length == 2) && (messageParts[1].startsWith("aoe2de://"))) {
    		int matchUpIndex = roleHostTeam.get(roleHost);
			MatchUp match = matches.get(matchUpIndex);
			match.setRoomKey(messageParts[1]);
			int[] team1 = match.getTeam1();
			int[] team2 = match.getTeam2();
			String buildMessage = "**Please join the lobby by enter the following URL into your browser: ``"
					+ match.getRoomKey()+ "``**";
			for (int i = 1; i < team1.length; i++) {
				User player = claimedAccounts.get(team1[i]);
				sendDirectMessage(player, buildMessage);
			}
			for (int i = 0; i < team2.length; i++) {
				User player = claimedAccounts.get(team2[i]);
				sendDirectMessage(player, buildMessage);
			}
			String globalMessage = " \n \n **__Match Starting!:__** \n" 
			+ match.teamToString(1) + " vs " + match.teamToString(2) + " starting now: \n"
					+ "Spectators may join the Room using ``" + match.getRoomKey() + "``\n"
							+ "GLHF 30!";
			GLOBAL_CHANNEL.sendMessage(globalMessage).queue();
			
    	} else {
			sendDirectMessage(roleHost, "**Invite URL wrong. It should look like this: ``aoe2de://_/_____``** \n" +
					"**Please resend the Invite URL by using ``/send <URL>``**");
			return;
		}
    }
    
    protected void handleResult(User user, String msg) {
    	String[] messageParts = msg.split(" ");
    	if ((messageParts.length == 2) && (messageParts[1].equals("won") || messageParts[1].equals("lost"))) {
    		if (roleHostTeam.containsKey(user)) {
        		MatchUp match = matches.get(roleHostTeam.get(user));
        		String winnerTeamStr = match.teamToString(1);
        		int[] winnerTeam = match.getTeam1();
        		int[] loserTeam = match.getTeam2();
        		if (messageParts[1].equals("lost")) {
        			winnerTeamStr = match.teamToString(2);
        			winnerTeam = match.getTeam2();
        			loserTeam = match.getTeam1();
        		}
        		for (int i = 0; i < loserTeam.length; i++) {
        			loserPlayers.add(loserTeam[i]);
        		}
        		for (int i = 0; i < winnerTeam.length; i++) {
        			int winner = winnerTeam[i];
        			winnerPlayers.add(winner);
        			String messageContent = "Congratulations on the win! Please wait patiently for the next round to start!";
        			if (isGrandFinal()) {
        				messageContent = "Congratulations on winning the tournament! You are the champion!";
        			}
        			sendDirectMessage(claimedAccounts.get(winner), messageContent);
        		}
        		GLOBAL_CHANNEL.sendMessage("Match " + (1 + roleHostTeam.get(user)) + " finished! Winner: " + winnerTeamStr).queue();
        		if ((matches.size() * winnerTeam.length) == winnerPlayers.size()) {
        			printRoundEndStatus();
        		}
        	} else {
        		sendDirectMessage(user, "**You are not the assigned Host. Please let the Host send the result of the match!**");
        	}
    	} else {
    		sendDirectMessage(user, "**Please send the message in the right format ``/result <won/lost>``.**");
    	}
    }
    
    protected void handleCorrectResult(String msg) {
    	String[] messageParts = msg.split(" ");
    	if (messageParts.length == 3 && isNumeric(messageParts[1]) &&(messageParts[2].equals("won") || messageParts[2].equals("lost"))) {
    		User user = claimedAccounts.get(Integer.parseInt(messageParts[1]));
    		if (roleHostTeam.containsKey(user)) {
    			MatchUp match = matches.get(roleHostTeam.get(user));
    			String winnerTeamStr = match.teamToString(1);
    			int[] loserTeam = match.getTeam2();
    			int[] winnerTeam = match.getTeam1();
    			if(messageParts[2].equals("lost")) {
    				winnerTeamStr = match.teamToString(2);
    				loserTeam = match.getTeam1();
    				winnerTeam = match.getTeam2();
    			}
    			if (winnerPlayers.contains(winnerTeam[0])) {
    				GLOBAL_CHANNEL.sendMessage("**Match result is already correct!** No changes made.").queue();
    				return;
    			}
    			for (int i = 0; i < loserTeam.length; i++) {
    				Integer player = loserTeam[i];
    				winnerPlayers.remove(player);
    				loserPlayers.add(loserTeam[i]);
    			}
    			for (int i = 0; i< winnerTeam.length; i++) {
    				Integer player = winnerTeam[i];
    				loserPlayers.remove(player);
    				winnerPlayers.add(winnerTeam[i]);
    			}
    			GLOBAL_CHANNEL.sendMessage("**CORRECTION: ** Match " + (1 + roleHostTeam.get(user)) 
    			+ " corrected! New Winner: " + winnerTeamStr).queue();
    			printRoundEndStatus();
    		} else {
    			GLOBAL_CHANNEL.sendMessage("``YOINC_acc" + (correctNumberFormate(Integer.parseInt(messageParts[1])) 
    					+ "`` is not a Host or the match has yet to be created. Please try again later or try using the correct Host")).queue();
    		}
    	} else {
    		GLOBAL_CHANNEL.sendMessage("**Please send the message in the right format ``/correctResult <Host ID> <won/lost>``.**").queue();
    	}
    }
    
    protected void printRoundEndStatus() {
    	GLOBAL_CHANNEL.sendMessage("**__All Matches finished!__** \n").queue();
    	if (isGrandFinal()) {
    		GLOBAL_CHANNEL.sendMessage("**CONGRATULATIONS TO OUR CHAMPION ** ``YOINC_acc"
    				+ correctNumberFormate(winnerPlayers.get(0)) + "`` FOR WINNING THIS TOURNAMENT!!!").queue();
    		return;
    	}
    	String loserMsg = "Players who dropped out this round: \n";
    	loserMsg += "``YOINC_acc" + correctNumberFormate(loserPlayers.get(0)) + "``";
    	for (int i = 1; i < loserPlayers.size(); i++) {
    		int player = loserPlayers.get(i);
    		loserMsg += ", ``YOINC_acc" + correctNumberFormate(player) + "``";
    	}
    	GLOBAL_CHANNEL.sendMessage(loserMsg).queue();
    	String winnerMsg = "Players who advance to the next round: \n";
    	winnerMsg += "``YOINC_acc" + correctNumberFormate(winnerPlayers.get(0)) + "``";
    	for (int i = 1; i < winnerPlayers.size(); i++) {
    		int player = winnerPlayers.get(i);
    		winnerMsg += ", ``YOINC_acc" + correctNumberFormate(player) + "``";
    	}
    	if (playerFreePass != -1) {
    		winnerMsg += ", ``YOINC_acc" + correctNumberFormate(playerFreePass) + "``";
    	}
    	GLOBAL_CHANNEL.sendMessage(winnerMsg).queue();
    }
    
    protected String correctNumberFormate(int playerIndex) {
		if (playerIndex >= 1 && playerIndex <= 9) {
			return "0" + playerIndex;
		} else {
			return "" + playerIndex;
		}
	}
    
    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            int d = Integer.parseInt(strNum);
            if (d < 1 || d > 14) {
            	return false;
            }
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
    
    protected boolean isGrandFinal() {
    	if (matches.size() == 1 && matches.get(0).getTeam1().length == 1 && playerFreePass == -1) {
    		return true;
    	}
    	return false;
    }

    protected boolean checkIfFree(int possibleClaim) {
        if (claimedAccounts.get(possibleClaim) == null) {
            return true;
        }
        return false;
    }

    protected boolean checkIfUserDidntClaimed(User user) {
        if (claimedAccounts.isEmpty()) {
            return true;
        } else {
            for (Entry<Integer, User> entry : claimedAccounts.entrySet()) {
                if (equalsUser(entry.getValue(), user)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private boolean equalsUser(User user1, User user2) {
    	if (user1.getId().equals(user2.getId()))
    		return true;
    	
    	return false;
    }
    
}