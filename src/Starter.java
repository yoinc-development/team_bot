import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;

public class Starter implements EventListener {

    private static final String TOKEN = "";

    public static void main(String[] args) {
        try {
            JDA jda = JDABuilder.createDefault(TOKEN)
                    .addEventListeners(new Starter())
                    .build();

            jda.awaitReady();
        } catch (LoginException ex) {
            System.out.println("Nice. Login failed.");
        } catch (InterruptedException ex) {
            System.out.println("Nice. Something interrupted the connection.");
        }
    }

    @Override
    public void onEvent(@Nonnull GenericEvent genericEvent) {

    }
}
