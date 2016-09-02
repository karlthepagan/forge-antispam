package karl.codes.commandline;

import karl.codes.chatbot.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

/**
 * Created by karl on 9/1/16.
 */
@Service
public class CommandlineService {
    @Autowired
    private BotService bot;

    @Async
    public void runConsole() throws IOException, URISyntaxException, InterruptedException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String msg = console.readLine();
            if (msg == null) {
                continue;
            }

            switch(msg) {
                case "break":
                    break;

                case "connect":
                    bot.connect();
                    break;

                case "bye":
                    bot.disconnect();
                    return;

                case "wsping":
                    bot.wsPing();
                    break;

                default:
                    bot.text(msg);
                    break;
            }
        }
    }
}
