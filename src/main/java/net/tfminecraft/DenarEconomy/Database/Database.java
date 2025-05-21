package net.tfminecraft.DenarEconomy.Database;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.tfminecraft.DenarEconomy.Data.PlayerData;

public class Database {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final File dataDir = new File("plugins/DenarEconomy/PlayerData");

    public static void savePlayerData(PlayerData data) {
        if (!dataDir.exists()) dataDir.mkdirs();

        File file = new File(dataDir, data.getId().toString() + ".json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PlayerData loadPlayerData(UUID player) {
        File file = new File(dataDir, player.toString() + ".json");
        if (!file.exists()) {
            return new PlayerData(player);
        }

        try (Reader reader = new FileReader(file)) {
            PlayerData data = gson.fromJson(reader, PlayerData.class);
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return new PlayerData(player); // Fallback
        }
    }

    public static boolean hasPlayerData(UUID id) {
        File file = new File(dataDir, id.toString() + ".json");
        return file.exists();
    }

}
