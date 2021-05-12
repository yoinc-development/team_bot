import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Properties;

public class HCMessage extends ListenerAdapter {

    private Properties properties;

    public HCMessage(Properties properties) {
        super();
        this.properties = properties;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {

    }
}
