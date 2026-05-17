package io.github.theflysong.user;

import io.github.theflysong.level.GameMap;

import com.google.gson.JsonObject;

/**
 * 用户实体 — 包含身份信息和存档数据
 *
 * @author norbe
 * @date 2026年5月07日
 */
public class User {

    private String username;
    private String passwordHash;
    private int uid;
    private int score;
    private boolean isGuest;
    private GameMap gameMap;
    private String savedLevelId;
    private int savedEnergy;
    private long lastSaveTime;
    private long savedTime;
    private static int Uid = 1;


    public User(String username, String passwordHash, boolean isGuest) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.uid = Uid++;
        this.isGuest = isGuest;
    }

    public User(String username, boolean isGuest) {
        this.username = username;
        this.isGuest = isGuest;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public boolean isGuest() {
        return isGuest;
    }

    public void setGuest(boolean guest) {
        isGuest = guest;
    }

    public GameMap getGameMap() {
        return gameMap;
    }

    public void setGameMap(GameMap gameMap) {
        this.gameMap = gameMap;
    }

    public String getSavedLevelId() {
        return savedLevelId;
    }

    public void setSavedLevelId(String savedLevelId) {
        this.savedLevelId = savedLevelId;
    }

    public int getSavedEnergy() {
        return savedEnergy;
    }

    public void setSavedEnergy(int savedEnergy) {
        this.savedEnergy = savedEnergy;
    }

    public long getLastSaveTime() {
        return lastSaveTime;
    }

    public void setLastSaveTime(long lastSaveTime) {
        this.lastSaveTime = lastSaveTime;
    }

    public boolean hasSave() {
        return gameMap != null;
    }

    public static int getUidCounter() {
        return Uid;
    }
    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
    public static void setUidCounter(int value) {
        Uid = value;
    }


    public long getSavedTime() {
        return savedTime;
    }

    public void setSavedTime(long savedTime) {
        this.savedTime = savedTime;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("username", username);
        obj.addProperty("passwordHash", passwordHash != null ? passwordHash : "");
        obj.addProperty("uid", uid);
        obj.addProperty("score", score);
        obj.addProperty("isGuest", isGuest);
        obj.addProperty("savedLevelId", savedLevelId != null ? savedLevelId : "");
        obj.addProperty("savedEnergy", savedEnergy);
        obj.addProperty("lastSaveTime", lastSaveTime);
        obj.addProperty("savedTime", savedTime);
        if (gameMap != null) {
            obj.add("gameMap", gameMap.toJson());
        }
        return obj;
    }

    public static User fromJson(JsonObject obj) {
        String username = obj.get("username").getAsString();
        String passwordHash = obj.get("passwordHash").getAsString();
        boolean isGuest = obj.get("isGuest").getAsBoolean();
        User user = new User(username, isGuest);
        user.setPasswordHash(passwordHash);
        user.setUid(obj.get("uid").getAsInt());
        user.setScore(obj.get("score").getAsInt());
        if (obj.has("savedLevelId")) {
            user.setSavedLevelId(obj.get("savedLevelId").getAsString());
        }
        user.setSavedEnergy(obj.get("savedEnergy").getAsInt());
        user.setLastSaveTime(obj.get("lastSaveTime").getAsLong());
        user.setSavedTime(obj.get("savedTime").getAsLong());
        if (obj.has("gameMap") && !obj.get("gameMap").isJsonNull()) {
            user.setGameMap(GameMap.fromJson(obj.getAsJsonObject("gameMap")));
        }
        return user;
    }
}
