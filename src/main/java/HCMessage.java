import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

public class HCMessage extends ListenerAdapter {

    private Properties properties;
    private Map<Integer, String> claimedAccounts = new HashMap<Integer, String>();
    private List<int[]> teams = new ArrayList<int[]>();
    private int playerFreePass = -1;
    private TextChannel GLOBAL_CHANNEL = null;

    public HCMessage(Properties properties) {
        super();
        this.properties = properties;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        String user = event.getAuthor().getId();
        boolean isAdmin = isAdmin(user);
        if (event.getMessage() != null) {
            if (event.isFromType(ChannelType.PRIVATE)) {
                    switch (handleMessage(event.getMessage().getContentDisplay(), isAdmin)) {
                        case 1:
                            if (GLOBAL_CHANNEL != null) {
                                handleClaim(event, channel, user, event.getAuthor().getName());
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
    }

    protected void handleSetup(MessageReceivedEvent event) {
        claimedAccounts = new HashMap<Integer, String>();
        teams = new ArrayList<int[]>();
        playerFreePass = -1;

        GLOBAL_CHANNEL = event.getGuild().getTextChannelById(event.getChannel().getId());
        GLOBAL_CHANNEL.sendMessage("Setup complete. Using ``" + GLOBAL_CHANNEL.getName() + "`` for updates.").queue();
        //Additional Setup Methods required
    }

    protected void handleClaim(MessageReceivedEvent event, MessageChannel channel, String user, String userName) {
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
                    channel.sendMessage("You successfully claimed ``YOINC_acc0" + possibleClaim + "``.\n" +
                            "Please follow the guide on the website to set up family sharing.").queue();
                    GLOBAL_CHANNEL.sendMessage("``YOINC_acc0" + possibleClaim + "`` claimed.").queue();
                    System.out.println("[CLAIM] - " + userName + " claimed YOINC_acc0" + possibleClaim);
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

    protected boolean isAdmin(String user) {
        final String ADMIN = properties.getProperty("bot.admin");
        if(ADMIN != null && ADMIN.contains(",")) {
            String[] adminList = ADMIN.split(",");
            for(String admin : adminList) {
                if(user.equals(admin)) {
                    return true;
                }
            }
        } else {
            if (user.equals(ADMIN)) {
                return true;
            }
        }
        return false;
    }
    //admin should consider teamSize such that
    //(playerAcc.size() / teamSize) % 2 == 0
    protected void createTeams(int teamSize) {
    	Set<Integer> playerAcc = claimedAccounts.keySet();
    	Random rand = new Random();
    	if (playerAcc.size() % teamSize == 1) { 
    		int randomIndex = rand.nextInt(playerAcc.size());
    		playerFreePass = getPlayerAcc(playerAcc, randomIndex);
    		playerAcc.remove(playerFreePass);
    	}  
    	if ((playerAcc.size() / teamSize) % 2 != 0) {
    		GLOBAL_CHANNEL.sendMessage("Please reconsider the Team Size!").queue();
    		return;
    	}
    	int teamAmount = playerAcc.size() / teamSize;
    	for (int i = 0; i < teamAmount; i++) {
    	    String builtMessage = "";
    		int[] playerTeam = new int[teamSize];
    		for (int j = 0; j < teamSize; j++) {
    			int teamMate = getPlayerAcc(playerAcc, rand.nextInt(playerAcc.size()));
    			if(builtMessage.isEmpty()) {
                    builtMessage = "**YOINC_acc0" + teamMate + "**";
                } else {
                    builtMessage = builtMessage + " & YOINC_acc0" + teamMate;
                }
    			playerTeam[j] = teamMate;
    			playerAcc.remove(teamMate);
    		}
    		teams.add(playerTeam);
    		GLOBAL_CHANNEL.sendMessage("__Team " + (i+1) + ":__\n" +
                    builtMessage).queue();
    	}
    	if(playerFreePass != -1) {
            GLOBAL_CHANNEL.sendMessage("Player YOINC_acc0" + playerFreePass + " gets a free pass.").queue();
        }
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

    protected boolean checkIfFree(int possibleClaim) {
        if (claimedAccounts.get(possibleClaim) == null) {
            return true;
        }
        return false;
    }

    protected boolean checkIfUserDidntClaimed(String user) {
        if (claimedAccounts.isEmpty()) {
            return true;
        } else {
            for (Map.Entry<Integer, String> entry : claimedAccounts.entrySet()) {
                if (entry.getValue().equals(user)) {
                    return false;
                }
            }
        }
        return true;
    }
}
