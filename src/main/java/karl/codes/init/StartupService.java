package karl.codes.init;

import karl.codes.commandline.CommandlineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by karl on 9/2/2016.
 */
@Component
public class StartupService {
    @Autowired
    private CommandlineService cmd;

    @PostConstruct
    public void init() throws InterruptedException, IOException, URISyntaxException {
        cmd.runConsole();
    }
}
