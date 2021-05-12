import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HCMessage extends ListenerAdapter {

    private Properties properties;
    private Map<Integer, String> claimedAccounts = new HashMap<Integer, String>();
    private TextChannel GLOBAL_CHANNEL = null;

    public HCMessage(Properties properties) {
        super();
        this.properties = properties;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        String user = event.getAuthor().getId();
        boolean isAdmin = false;
        if (user.equals(properties.getProperty("bot.admin"))) {
            isAdmin = true;
        }
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
                        default:
                            break;
                    }
            } else {
                switch (handleMessage(event.getMessage().getContentDisplay(), isAdmin)) {
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
        if (message.toLowerCase().startsWith("/setup")) {
            if (isAdmin) {
                return 9;
            }
        }
        //DEFINE OTHER COMMANDS HERE

        return 0;
    }

    protected void handleSetup(MessageReceivedEvent event) {
        GLOBAL_CHANNEL = event.getGuild().getTextChannelById(event.getChannel().getId());
        GLOBAL_CHANNEL.sendMessage("Setup complete. Using ``" + GLOBAL_CHANNEL.getName() + "`` for updates.").queue();
    }

    protected void handleClaim(MessageReceivedEvent event, MessageChannel channel, String user, String userName) {
        int possibleClaim = handleClaimMessage(event.getMessage().getContentDisplay());
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

    protected int handleClaimMessage(String message) {
        String[] messageParts = message.split(" ");
        if (messageParts.length == 2) {
            try {
                int returnValue = Integer.parseInt(messageParts[1]);
                if (returnValue <= 0) {
                    return 0;
                } else {
                    return returnValue;
                }
            } catch (NumberFormatException ex) {
                return -1;
            }
        } else {
            return -2;
        }
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
