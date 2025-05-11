package net.tfminecraft.DenarEconomy.Database;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.tfminecraft.DenarEconomy.Data.PlayerData;

public class Database {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final File dataDir = new File("plugins/DenarEconomy/PlayerData");

    public static void savePlayerData(PlayerData data) {
        if (!dataDir.exists()) dataDir.mkdirs();

        File file = new File(dataDir, data.getPlayer().getUniqueId().toString() + ".json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PlayerData loadPlayerData(Player player) {
        File file = new File(dataDir, player.getUniqueId().toString() + ".json");
        if (!file.exists()) {
            return new PlayerData(player);
        }

        try (Reader reader = new FileReader(file)) {
            PlayerData data = gson.fromJson(reader, PlayerData.class);
            data.setPlayer(player); // Restore transient field
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return new PlayerData(player); // Fallback
        }
    }

    public static void updateBankBalance(UUID id, double amountToAdd) {
        File file = new File(dataDir, id.toString() + ".json");
        if (!file.exists()) {
            System.out.println("No player data found for UUID: " + id);
            return;
        }

        try (Reader reader = new FileReader(file)) {
            // Load existing data
            PlayerData data = gson.fromJson(reader, PlayerData.class);

            // Apply the change to the bank
            data.getBank().change(amountToAdd);

            // Save back to file
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(data, writer);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean hasPlayerData(UUID id) {
        File file = new File(dataDir, id.toString() + ".json");
        return file.exists();
    }

    public static double getBankBalance(UUID id) {
    File file = new File(dataDir, id.toString() + ".json");
    if (!file.exists()) {
        return 0.0; // or throw an error if preferred
    }

    try (Reader reader = new FileReader(file)) {
        PlayerData data = gson.fromJson(reader, PlayerData.class);
        return data.getBank().getBal();
    } catch (IOException e) {
        e.printStackTrace();
        return 0.0;
    }
}

}
