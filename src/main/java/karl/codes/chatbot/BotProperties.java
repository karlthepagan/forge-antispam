package karl.codes.chatbot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Created by karl on 9/1/16.
 */
@Configuration
@ConfigurationProperties(prefix="irc-bot")
public class BotProperties {
    private String url = "wss://irc-ws.chat.twitch.tv";
    private String name;
    private String chatToken;
    private boolean connectOnStartup = false;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChatToken() {
        return chatToken;
    }

    public void setChatToken(String chatToken) {
        this.chatToken = chatToken.replaceAll("oauth:","");
    }

    public boolean isConnectOnStartup() {
        return connectOnStartup;
    }

    public void setConnectOnStartup(boolean connectOnStartup) {
        this.connectOnStartup = connectOnStartup;
    }
}
