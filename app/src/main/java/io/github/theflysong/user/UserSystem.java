package io.github.theflysong.user;

import static io.github.theflysong.App.LOGGER;

import io.github.theflysong.client.gui.GuiAnchor;
import io.github.theflysong.client.gui.GuiTextComponent;
import io.github.theflysong.client.gui.TextStyle;
import io.github.theflysong.level.GameMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.joml.Vector4f;

/**
 * 用户系统 — 管理注册、登录、游客模式及存档读写
 *
 * @author norbe
 * @date 2026年5月10日
 */
public class UserSystem {
    private static final String DATA_DIR = System.getProperty("user.home") + "/LinkLink-Game/userdata";
    private static final String DATA_FILE = "userdata.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path dataPath;
    private Map<String, User> users;
    private User currentUser;
    private boolean loadCorrupted;

    public UserSystem() {
        this.dataPath = Paths.get(DATA_DIR, DATA_FILE);
        load();
    }

    public boolean register(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }
        if (users.containsKey(username)) {
            return false;
        }
        String passwordHash = MD5.MD5hash(password);
        User user = new User(username, passwordHash, false);
        users.put(username, user);
        currentUser = user;
        persist();
        LOGGER.info("User registered: {} (uid={})", username, user.getUid());
        return true;
    }

    public boolean login(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        User user = users.get(username);
        if (user == null) {
            return false;
        }
        String hash = MD5.MD5hash(password);
        if (!hash.equals(user.getPasswordHash())) {
            return false;
        }
        currentUser = user;
        LOGGER.info("User logged in: {} (uid={})", username, user.getUid());
        return true;
    }

    public void loginAsGuest() {
        String guestName = "Guest_" + System.currentTimeMillis() % 100000;
        currentUser = new User(guestName,  true);
        LOGGER.info("Guest logged in: {}", guestName);
    }

    public void logout() {
        if (currentUser != null) {
            LOGGER.info("User logged out: {}", currentUser.getUsername());
            currentUser = null;
        }
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isGuest() {
        return currentUser != null && currentUser.isGuest();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void saveGame(GameMap gameMap, String levelId, int energy, int sorce, long elapsedTime) {
        if (currentUser == null || currentUser.isGuest()) {
            return;
        }
        currentUser.setGameMap(gameMap.copy());
        currentUser.setSavedLevelId(levelId);
        currentUser.setSavedEnergy(energy);
        currentUser.setLastSaveTime(System.currentTimeMillis());
        currentUser.setScore(sorce);
        currentUser.setSavedTime(elapsedTime);
        users.put(currentUser.getUsername(), currentUser);
        persist();
        LOGGER.info("Game saved for user: {}", currentUser.getUsername());
    }

    public GameMap getSavedGameMap() {
        if (currentUser != null && currentUser.hasSave()) {
            return currentUser.getGameMap().copy();
        }
        return null;
    }

    public String getSavedLevelId() {
        return currentUser != null ? currentUser.getSavedLevelId() : null;
    }

    public int getSavedEnergy() {
        return currentUser != null ? currentUser.getSavedEnergy() : 0;
    }

    public int getSavedScore() {
        return currentUser != null ? currentUser.getScore() : 0;
    }

    public long getSavedTime() {
        return currentUser != null ? currentUser.getSavedTime() : 0L;
    }

    public boolean hasSave() {
        return currentUser != null && currentUser.hasSave();
    }

    public void clearSave() {
        if (currentUser != null && currentUser.hasSave()) {
            currentUser.setGameMap(null);
            currentUser.setSavedLevelId(null);
            currentUser.setSavedEnergy(0);
            currentUser.setLastSaveTime(0);
            currentUser.setScore(0);
            currentUser.setSavedTime(0);
            users.put(currentUser.getUsername(), currentUser);
            persist();
            LOGGER.info("Save cleared for user: {}", currentUser.getUsername());
        }
    }

    public boolean isUsernameTaken(String username) {
        return users.containsKey(username);
    }

    public boolean isLoadCorrupted() {
        return loadCorrupted;
    }

    private void load() {
        try {
            Files.createDirectories(dataPath.getParent());
        } catch (IOException e) {
            LOGGER.error("Failed to create data directory", e);
        }
        if (Files.exists(dataPath)) {
            try {
                String json = Files.readString(dataPath, StandardCharsets.UTF_8);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                UserDataFile dataFile = UserDataFile.fromJson(root);
                users = dataFile.getUsers();
                User.setUidCounter(dataFile.getUid());
                LOGGER.info("Loaded {} users from {} (nextUid={})", users.size(), dataPath, dataFile.getUid());
                for (Map.Entry<String, User> entry : users.entrySet()) {
                    User u = entry.getValue();
                    LOGGER.info("  User '{}' (uid={}): hasSave={}, gameMap={}, score={}",
                        u.getUsername(), u.getUid(), u.hasSave(),
                        u.getGameMap() != null ? u.getGameMap().getClass().getSimpleName() : "null");
                }
                return;
            } catch (Exception e) {
                loadCorrupted = true;
                LOGGER.error("Failed to load user data, starting fresh", e);
            }
        } else {
            LOGGER.info("No user data file found, starting fresh");
        }
        users = new HashMap<>();
        User.setUidCounter(1);
    }

    private void persist() {
        try {
            System.out.println("Persisting user data to " + dataPath);
            Files.createDirectories(dataPath.getParent());
            UserDataFile dataFile = new UserDataFile();
            dataFile.setUsers(users);
            dataFile.setUid(User.getUidCounter());
            int savedCount = 0;
            for (User u : users.values()) {
                if (u.hasSave()) savedCount++;
            }
            LOGGER.info("Persisting {} users ({} with saves, nextUid={}) to {}",
                users.size(), savedCount, User.getUidCounter(), dataPath);
            String json = GSON.toJson(dataFile.toJson());
            Files.writeString(dataPath, json, StandardCharsets.UTF_8);
            LOGGER.info("Persisted {} bytes successfully", json.length());
        } catch (Exception e) {
            LOGGER.error("Failed to persist user data: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        }
    }
}
