import javax.sound.midi.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author Carl Schmidt
 * @author Julius Boström
 */
public class DrumPatternReader {

    private String section;
    private static final int NOTE_ON = 0x90;
    private static final int NOTE_OFF = 0x80;
    private final HashMap<Integer, Integer>[] counts;
    private final HashMap<Integer, Integer>[] countsFromFile;
    private final HashMap<Integer, Double>[] probabilities;
    private ArrayList<File> inputFiles = new ArrayList<>();
    private int filesRead = 0;


    public DrumPatternReader(String section) {
        this.section = section;
        counts = new HashMap[64];
        countsFromFile = new HashMap[64];
        probabilities = new HashMap[64];
        initCounts();
    }

    public void work() {
        File folder = new File("Corpus/" + section);
        readFolder(folder);
        writeCounts();
        calculateProbabilities();
        writeProbabilities();
    }

    /*
        Inspired by: https://stackoverflow.com/questions/1844688/how-to-read-all-files-in-a-folder-from-java
     */
    private void readFolder(File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                readFolder(fileEntry);
            } else {
                System.out.println("Filename: " + fileEntry.getName());
                read(fileEntry);
                filesRead++;
            }
        }
    }

    /*
        HEAVILY INSPIRED BY: https://stackoverflow.com/questions/3850688/reading-midi-files-in-java
     */
    private void read(File sample) {
        Map<Integer, LinkedList<Integer>> queuedHits = new HashMap<Integer, LinkedList<Integer>>();
        try {
            System.out.println("Reading " + sample.getName());
            Sequence seq = MidiSystem.getSequence(sample);
            for (Track track : seq.getTracks()) {
                System.out.println(track.ticks());
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage shortMessage = (ShortMessage) message;
                        if (shortMessage.getCommand() == NOTE_ON && shortMessage.getData2() != 0) {
                            int key = shortMessage.getData1();
                            long tick = event.getTick();
                            int sixteenth = (int) tick / 240;
                            LinkedList<Integer> queue = null;
                            if (queuedHits.containsKey(key)) {
                                queue = queuedHits.get(key);
                            } else {
                                queue = new LinkedList<Integer>();
                            }
                            queue.add(sixteenth);
                            queuedHits.put(key, queue);
                        } else if (shortMessage.getCommand() == NOTE_OFF ||
                                (shortMessage.getCommand() == NOTE_ON && shortMessage.getData2() == 0)) {
                            int key = shortMessage.getData1();
                            int sixteenth = queuedHits.get(key).removeFirst();
                            Integer countAtSixteenth = counts[sixteenth].get(key);
                            if (countAtSixteenth != null) {
                                counts[sixteenth].put(key, countAtSixteenth + 1);
                            } else {
                                counts[sixteenth].put(key, 1);
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void initCounts() {
        for (int i = 0; i < counts.length; i++) {
            counts[i] = new HashMap<Integer, Integer>();
        }
    }

    public void calculateProbabilities() {
        for (int i = 0; i < counts.length; i++) {
            probabilities[i] = new HashMap<Integer, Double>();
            HashMap<Integer, Integer> current = counts[i];
            if (!current.isEmpty()) {
                for (Integer key : current.keySet()) {
                    int value = current.get(key);
                    probabilities[i].put(key, (double) value / filesRead); // Insert the percentage of samples in which this drum hit occurred on the current sixteenth note
                }
            }
        }
    }

    public HashMap<Integer, Double>[] getProbabilities() {
        return probabilities;
    }

    private void writeProbabilities() {
        try {
            //Write to file
            File outputFolder = new File("Probabilities" + "/" + section);
            if (!outputFolder.exists()) {
                outputFolder.mkdirs();
            }

            String fileName = "probabilities";
            File file = new File(outputFolder + "/" + fileName + ".txt");
            PrintWriter writer = new PrintWriter(file);

            for (int i = 0; i < probabilities.length; i++) {
                writer.println(probabilities[i].toString());
            }

            writer.close();
            System.out.println("Finished generating \"" + fileName + ".txt\".");
        } catch (IOException e) {
            System.out.println("Couldn't write to file.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void writeCounts() {
        try {
            //Write to file
            File outputFolder = new File("HitCount" + "/" + section);
            if (!outputFolder.exists()) {
                outputFolder.mkdirs();
            }

            String fileName = "counts";
            File file = new File(outputFolder + "/" + fileName + ".txt");
            PrintWriter writer = new PrintWriter(file);

            for (int i = 0; i < counts.length; i++) {
                writer.println(counts[i].toString());
            }

            writer.close();
            System.out.println("Finished generating \"" + fileName + ".txt\".");
        } catch (IOException e) {
            System.out.println("Couldn't write to file.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void readCounts() {
        int x = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader("HitCount/counts.txt"));
            String line = reader.readLine();
            // read the key value pairs (hit type / hit count) for each sixteenth note and insert into hashmap 'counts'
            while (line != null) {
                // formatting
                line = line.replaceAll("\\{", "");
                line = line.replaceAll("\\}", "");
                line = line.replaceAll(" ", "");

                if (!line.trim().isEmpty()) {
                    //System.out.println("file line: " + line);

                    String[] split = line.split(",");

                    for (String s : split) {
                        //System.out.println("file splits: " + s);
                        String[] pairs = s.split("=");
                        int key = Integer.parseInt(pairs[0]);
                        int value = Integer.parseInt(pairs[1]);
                        if (x < 64) {
                            countsFromFile[x].put(key, value);
                        }
                    }
                }
                x++;
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
