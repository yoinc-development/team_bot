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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

public class HCMessage extends ListenerAdapter {

    private Properties properties;
    private User serverAdmin = null;

    private Map<Integer, User> claimedAccounts = new HashMap<Integer, User>();
    private List<int[]> teams = new ArrayList<int[]>();
    private List<MatchUp> matches = new ArrayList<MatchUp>();
    private Map<User, Integer> roleHostTeam = new HashMap<User, Integer>();

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
//                        case 2:
//                            if (GLOBAL_CHANNEL != null) {
//                            //TODO: say
//                            } else {
//                                channel.sendMessage("Setup not complete.").queue();
//                            }
//                            break;
                        case 8:
                            handleAdd(event);
                        case 12:
                        	shareRoomKey(user, event.getMessage().getContentDisplay());
                        	break;
                        case 18:
                        	handleSay(user, event.getMessage().getContentDisplay());
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
//        if (message.toLowerCase().startsWith("/say")) {
//            return 2;
//        }
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
        if (message.toLowerCase().startsWith("/say")) {
        	return 18;
        }
        //DEFINE OTHER COMMANDS HERE

        return 0;
    }

    protected void handleStart(MessageReceivedEvent event) {
        String message = event.getMessage().getContentDisplay();
        String[] splitMessage = message.split(" ");
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
        	generateMatchUp();
        	notifyRoleHost();
        	releaseMatchUp();
        }
    }

    protected void handleSetup(MessageReceivedEvent event) {
        claimedAccounts = new HashMap<Integer, User>();
        teams = new ArrayList<int[]>();
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
                    System.out.println("[CLAIM] - " + userName + " claimed YOINC_acc" + correctNumberFormate(possibleClaim));
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
    
    protected void handleSay(User user, String msg) {
    	String[] msgParts = msg.split(" ");
    	String userName = "``yoinc_" + correctNumberFormate(getKeyByValue(claimedAccounts, user)) + "``"; 
    	if (msgParts.length >= 3) {	
    		if (msgParts[1].equals("admin")) {
    			String chatMsg = msgParts[2];
    			for (int i = 3; i < msgParts.length; i++) {
    				chatMsg += " " + msgParts[i];
    			}
    			sendDirectMessage(serverAdmin, "[ADMIN-CHAT] - " + userName + " says: _" + chatMsg + "_");
    			return;
    		} else if (msgParts[1].equals("asAdmin") && isAdmin(user)) {
    			String chatMsg = msgParts[3];
    			for (int i = 4; i < msgParts.length; i++) {
    				chatMsg += " " + msgParts[i];
    			}
    			helperHandleSay(user, "admin", msgParts[2], chatMsg, true);
    		} else {
    			String chatMsg = msgParts[2];
    			for (int i = 3; i < msgParts.length; i++) {
    				chatMsg += " " + msgParts[i];
    			}
    			helperHandleSay(user, userName, msgParts[1], chatMsg, false);
    		}
    	} else {
    		sendDirectMessage(user, "**Please send the message in the right format ``/say <AccountID> <Message>``, "
    				+ "where the AccountID is the number of recipient's account (e.g 9 for ``yoinc_09`` or admin for ``admin``)**");
    	}
    }
    
    private void helperHandleSay(User user, String userName ,String recipString, String msg, boolean asAdmin) {
    	int recipId = -1;
    	try {
			recipId = Integer.parseInt(recipString);
		} catch (NumberFormatException ex) {
			sendDirectMessage(user, "**Please send the message in the right format ``/say <AccountID> <Message>``, "
    				+ "where the AccountID is the number of recipient's account (e.g 9 for ``yoinc_09`` or admin for ``admin``)**");
			return;
		}
		if (recipId > 0 && recipId <= 14 && claimedAccounts.containsKey(recipId)) {
			User recipUser = claimedAccounts.get(recipId);
			if (asAdmin) {
				sendDirectMessage(recipUser, "[ADMIN-CHAT] - admin says: _" + msg + "_");
			} else {
				sendDirectMessage(recipUser, "[CHAT] - " + userName + " says: _" + msg + "_");
			}
		} else {
			sendDirectMessage(user, "**USER NOT FOUND**");
			return;
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
                	if (serverAdmin == null) {
                		serverAdmin = user;
                	}
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
    	if (!roleHostTeam.containsKey(roleHost)) {
    		sendDirectMessage(roleHost, "**You are not allowed to send an invite URL!** \n"
    				+ "Please wait until the selected Host has created a lobby!");
    	}
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
			String globalMessage = " \n**__Match Starting!:__** \n" 
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
    
    protected String correctNumberFormate(int playerIndex) {
		if (playerIndex >= 1 && playerIndex <= 9) {
			return "0" + playerIndex;
		} else {
			return "" + playerIndex;
		}
	}
    
    private static <T, E> T getKeyByValue(Map<T, E> map, E value) {
    	for (Entry<T, E> entry : map.entrySet()) {
    		if (Objects.equals(value, entry.getValue())) {
    			return entry.getKey();
    		}
    	}
    	return null;
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