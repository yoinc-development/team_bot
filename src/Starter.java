import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Starter implements EventListener {

    public static void main(String[] args) {
        try {
            InputStream input = Starter.class.getClassLoader().getResourceAsStream("config.properties");
            Properties properties = new Properties();
            properties.load(input);

            JDA jda = JDABuilder.createDefault(properties.getProperty("api.token"))
                    .addEventListeners(new Starter())
                    .addEventListeners(new HCMessage(properties))
                    .build();

            jda.awaitReady();

        } catch (LoginException ex) {
            System.out.println("Nice. Login failed.");
        } catch (InterruptedException ex) {
            System.out.println("Nice. Something interrupted the connection.");
        } catch (IOException ex) {
            System.out.println("Nice. Problems with that property.");
        }
    }

    @Override
    public void onEvent(@Nonnull GenericEvent genericEvent) {
        if(genericEvent instanceof ReadyEvent) {
            System.out.println("Ready.");
        }
    }
}
