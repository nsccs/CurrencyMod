package org.northcs.selling;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.northcs.CurrencyMod;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;

public class ReceiverQueue {
    /**
     * The number of people who can receive sold items.
     */
    public static final int QUEUE_LENGTH = 20;

    /**
     * Old values are stored at the start, new values are at the end.
     */
    private static ArrayList<UUID> queue;

    private static final Type QUEUE_TYPE = (new TypeToken<ArrayList<UUID>>() {}).getType();

    /**
     * Initializes the queue and reads data from the disk.
     */
    public static void init() {
        var receiversFile = receiversFile();
        if (receiversFile.exists()) {
            try {
                queue = new Gson().fromJson(new FileReader(receiversFile), QUEUE_TYPE);
            } catch (Exception e) {
                // This data isn't critical, so it's fine to reset it.
                queue = new ArrayList<>();
                CurrencyMod.LOGGER.error(e.toString());
            }
        } else {
            queue = new ArrayList<>();
        }

        // Downsize the queue if it's too big.
        fit();
    }

    private static File receiversFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("currency-receivers.json").toFile();
    }

    /**
     * Removes items from the beginning until the queue is the correct size.
     */
    private static void fit() {
        if (queue.size() > QUEUE_LENGTH) {
            var amountToRemove = queue.size() - QUEUE_LENGTH;
            queue.subList(0, amountToRemove).clear();
        }
    }

    /**
     * Push a UUID to the end of the queue.
     * If it is already in the queue, it will be moved to the end.
     * If there is not enough space, the first element will be removed.
     * The new queue will be saved to a file.
     *
     * @param uuid the UUID to push.
     */
    public static void push(UUID uuid) {
        // Remove the UUID from the queue if it's already there.
        queue.remove(uuid);

        queue.add(uuid);

        // If there isn't enough room, remove the first element to make room.
        fit();

        write();
    }

    /**
     * Writes the queue to a file.
     */
    private static void write() {
        var receiversFile = receiversFile();

        try {
            Files.write(receiversFile.toPath(), new Gson().toJson(queue, QUEUE_TYPE).getBytes());
        } catch (Exception e) {
            // An error here isn't critical.
            CurrencyMod.LOGGER.error(e.toString());
        }
    }
}
