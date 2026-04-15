package io.github.theflysong;

import io.github.theflysong.client.ClientApp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {
    public static final Logger LOGGER = LogManager.getLogger(App.class);

    public static ClientApp clientApp;
    public static final String APPID = "linklink";

    public static void main(String[] args) {
        LOGGER.info("Application starting, appId={}", APPID);
        clientApp = new ClientApp();
        clientApp.run();
        LOGGER.info("Application exited");
    }
}