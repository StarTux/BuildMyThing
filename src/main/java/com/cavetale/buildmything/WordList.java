package com.cavetale.buildmything;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class WordList {
    private final BuildMyThingPlugin plugin;
    private List<String> wordList;
    private final Random random = new Random();

    public void load() {
        File dir = new File(plugin.getDataFolder(), "words");
        dir.mkdirs();
        final Map<String, String> map = new HashMap<>();
        for (File file : dir.listFiles()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = "";
                do {
                    line = br.readLine();
                    if (line == null) break;
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (line.length() < 5) continue;
                    map.put(line.toLowerCase(), line);
                } while (line != null);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        this.wordList = new ArrayList<>(map.values());
    }

    public String randomWord() {
        return wordList.get(random.nextInt(wordList.size()));
    }
}
