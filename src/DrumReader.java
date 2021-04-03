
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;


import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

public class DrumReader {

    private String outputPath;
    private static final int NOTE_ON = 0x90;
    private static final int NOTE_OFF = 0x80;
    private int filesRead = 0;
    private final File folder;
    private final HashMap<Integer, Integer>[] counts;
    private final HashMap<Integer, Integer>[] countsFromFile;
    private final HashMap<Integer, Double>[] probabilities;
    private ArrayList<File> inputFiles = new ArrayList<>();


    public DrumReader(String path, String outputPath) {
        this.outputPath = outputPath;
        counts = new HashMap[64];
        countsFromFile = new HashMap[64];
        probabilities = new HashMap[64];
        initCounts();
        initProbabilities();
        folder = new File(path);
        addFilesForFolder(folder);
    }

    public void work() {
        readFolder();
        writeCounts();
        readCounts();
        calculateProbabilities();
    }

    public void readFolder() {
        System.out.println("Starting to read folder...");
        for (File file : inputFiles) {
            System.out.println("Initializing read of " + file.getName());
            read(file);
            filesRead++;
        }
    }

    /*
        BASED ON CODE BY Sami Koivu: https://stackoverflow.com/questions/3850688/reading-midi-files-in-java
     */
    public void read(File sample) {
        Map<Integer, LinkedList<Integer>> queuedHits = new HashMap<Integer, LinkedList<Integer>>();
        try {
//            Sequence seq = MidiSystem.getSequence(inputFiles.get(0));
            System.out.println("Reading " + sample.getName());
            Sequence seq = MidiSystem.getSequence(sample);
            for (Track track : seq.getTracks()) {
                System.out.println(track.ticks());
//                System.out.println("Current track size: " + trackSize);
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage shortMessage = (ShortMessage) message;
                        if (shortMessage.getCommand() == NOTE_ON && shortMessage.getData2() != 0) {
                            int key = shortMessage.getData1();
                            long tick = event.getTick();
                            int sixteenth = (int) tick / 240;
//                            System.out.println("Read a " + key + " at " + tick);
                            LinkedList<Integer> queue = null;
                            if (queuedHits.containsKey(key)) {
                                queue = queuedHits.get(key);
                            } else {
                                queue = new LinkedList<Integer>();
                            }
                            queue.add(sixteenth);
//                            System.out.println("Adding sixteenth number " + sixteenth + " to queue for drum hit " + key);
                            queuedHits.put(key, queue);
                        } else if (shortMessage.getCommand() == NOTE_OFF ||
                                (shortMessage.getCommand() == NOTE_ON && shortMessage.getData2() == 0)) {
                            int key = shortMessage.getData1();
                            int sixteenth = queuedHits.get(key).removeFirst();
//                            System.out.println("Dequeued a hit for " + key + " at sixteenth number " + sixteenth);
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
            for (LinkedList<Integer> queue : queuedHits.values()) {
                System.out.println(queue.toString());
            }
            //for (int i = 0; i < counts.length; i++) {
            //    System.out.println("counts: " + counts[i].toString());
            //}

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void initCounts() {
        for (int i = 0; i < counts.length; i++) {
            counts[i] = new HashMap<Integer, Integer>();
        }
        for (int i = 0; i < countsFromFile.length; i++) {
            countsFromFile[i] = new HashMap<Integer, Integer>();
        }
    }

    private void initProbabilities() {
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] = new HashMap<Integer, Double>();
        }
    }

    public void calculateProbabilities() {
        for (int i = 0; i < countsFromFile.length; i++) {
            HashMap<Integer, Integer> current = countsFromFile[i];
            if (!current.isEmpty()) {
                for (Integer key : current.keySet()) {
                    int value = current.get(key);
                    probabilities[i].put(key, (double) value / filesRead); // Insert the percentage of samples in which this drum hit occurred on the current sixteenth note
                }
            }
        }
        for (int i = 0; i < probabilities.length; i++) {
            System.out.println(probabilities[i].toString());
        }
    }

    public HashMap<Integer, Double>[] getProbabilities() {
        return probabilities;
    }

    /*
        Method below stolen from stack overflow user rich:
        https://stackoverflow.com/questions/1844688/how-to-read-all-files-in-a-folder-from-java
    */
    private void addFilesForFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                addFilesForFolder(fileEntry);
            } else {
                System.out.println("Filename: " + fileEntry.getName());
                inputFiles.add(fileEntry);
            }
        }
    }

    private void writeCounts() {
        try {
            //Write to file
            File outputFolder = new File("HitCount" + "/" + outputPath);
            if (!outputFolder.exists()) {
                outputFolder.mkdir();
            }

            LocalDateTime now = LocalDateTime.now();
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
