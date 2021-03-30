import javax.sound.midi.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Random;

/**
 * Mycket stulet från http://www.automatic-pilot.com/midifile.html
 *
 * Good shit: https://www.midi.org/specifications-old/item/table-2-expanded-messages-list-status-bytes
 *            https://www.midi.org/specifications-old/item/gm-level-1-sound-set
 *            https://www.recordingblogs.com/wiki/midi-set-tempo-meta-message
 *
 * @author Carl Schmidt
 * @author Julius Boström
 *
 */
public class DrumGenerator {
    private final String section;
    private Sequence seq;
    private Track track;
    private Random rand = new Random();
    private HashMap<Integer, Double>[] probabilities = new HashMap[64];
    private static final int NOTE_ON = 0x99; // Channel 10, note on
    private static final int NOTE_OFF = 0x89; // Channel 10, note off

    public DrumGenerator(String section) {
        this.section = section;
        initTrack();
    }

    public void generateTrack() {
        readProbabilities();
        makeDrumLoop();
    }

    /*
        BASED ON: http://www.automatic-pilot.com/midifile.html
     */
    private void initTrack() {
        try {
            // Init track
            seq = new Sequence(Sequence.PPQ, 24);
            track = seq.createTrack();

            // Init MIDI sound set
            byte[] bytes = {(byte) 0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte) 0xF7}; //???
            SysexMessage sysexMessage = new SysexMessage();
            sysexMessage.setMessage(bytes, 6);
            MidiEvent event = new MidiEvent(sysexMessage, 0);
            track.add(event);

            //Set tempo to 120 bpm
            MetaMessage metaMessage = new MetaMessage();
            bytes = new byte[]{0x07, (byte) 0xA1, 0x20}; // Här är byte-arrayen som ger tempo 120 bpm.
            metaMessage.setMessage(0x51, bytes, 3);
            event = new MidiEvent(metaMessage, 0);
            track.add(event);

            // Set track name
            metaMessage = new MetaMessage();
            String name = "drum track";
            metaMessage.setMessage(0x03, name.getBytes(), name.length());
            event = new MidiEvent(metaMessage, 0);
            track.add(event);

            // Set omni on
            ShortMessage shortMessage = new ShortMessage();
            shortMessage.setMessage(0xB0, 0x7D,0x00);
            event = new MidiEvent(shortMessage,(long)0);
            track.add(event);

            // Set poly on
            shortMessage = new ShortMessage();
            shortMessage.setMessage(0xB0, 0x7F, 0x00);
            event = new MidiEvent(shortMessage, 0);
            track.add(event);

            // Set instrument to drums
            shortMessage = new ShortMessage();
            shortMessage.setMessage(0xC9, 0x0A, 0x0A);
            event = new MidiEvent(shortMessage, 0);
            track.add(event);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void readProbabilities() {
        int x = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader("Probabilities/" + section + "/probabilities.txt"));
            String line = reader.readLine();
            // read the key value pairs (hit type / hit count) for each sixteenth note and insert into hashmap 'counts'
            while (line != null) {

                probabilities[x] = new HashMap<Integer, Double>(); // Initialize table while reading

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
                        double value = Double.parseDouble(pairs[1]);
                        if (x < 64) {
                            probabilities[x].put(key, value);
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

    private void makeDrumLoop() {
        long currentPulses = 0;
        try {
            for (int i = 0; i < probabilities.length; i++) {
                /* value of 'key' determines which part of the drum kit is played */
                for (Integer key : probabilities[i].keySet()) {
//                    double randNum = 1.0 / (rand.nextInt(10) + 1); // If hit occurs in at least 50% of cases, hit is almost always printed. Produces good results.
//                    double randNum = rand.nextDouble(); // Fine-grained steps of ???%

                    /* Varying degrees of """creativity""" for random generators that are easy to measure */
//                    double randNum = (rand.nextInt(34) + 1) * 0.01;
//                    double randNum = (rand.nextInt(50) + 1) * 0.01;
//                    double randNum = (rand.nextInt(60) + 1) * 0.01;
//                    double randNum = (rand.nextInt(75) + 1) * 0.01;
//                    double randNum = (rand.nextInt(85) + 1) * 0.01;
//                    double randNum = (rand.nextInt(95) + 1) * 0.01;
                    double randNum = (rand.nextInt(100) + 1) * 0.01; // Steps of 1%, between 0.01 and 1.0

//                    double randNum = (rand.nextInt(50) + 1) * 0.02;
//                    double randNum = (rand.nextInt(40) + 1) * 0.025;
//                    double randNum = (rand.nextInt(38) + 1) * (1.0 / 38.0);
//                    double randNum = (rand.nextInt(37) + 1) * (1.0 / 37.0);
//                    double randNum = (rand.nextInt(35) + 1) * (1.0 / 35.0);
//                    double randNum = (rand.nextInt(30) + 1) * (1.0 / 30.0);

                    /* Varying degrees of """creativity""" for random generators using multiplicative inverse
                    *  which applies "weight" to the drums with higher relative frequency */
//                    double randNum = 1.0 / (rand.nextInt(20) + 1);
//                    double randNum = 1.0 / (rand.nextInt(15) + 1);
//                    double randNum = 1.0 / (rand.nextInt(9) + 1);
//                    double randNum = 1.0 / (rand.nextInt(8) + 1);
//                    double randNum = 1.0 / (rand.nextInt(7) + 1);
//                    double randNum = 1.0 / (rand.nextInt(6) + 1);
//                    double randNum = 1.0 / (rand.nextInt(5) + 1);

                    if (shouldBeOutput(key, i, randNum)) {
                        ShortMessage drumHit = new ShortMessage();
                        drumHit.setMessage(NOTE_ON, key, 64);
                        MidiEvent event = new MidiEvent(drumHit, currentPulses);
                        track.add(event);

                        drumHit = new ShortMessage();
                        drumHit.setMessage(NOTE_OFF, key, /*0x40*/ 64);
                        event = new MidiEvent(drumHit, currentPulses + 6);
                        track.add(event);
                    }
                }
                currentPulses += 6; // Move ahead a sixteenth note
            }
           MetaMessage metaMessage = new MetaMessage();
           byte[] bet = {}; // empty array
           metaMessage.setMessage(0x2F,bet,0x00); // End track
           MidiEvent event = new MidiEvent(metaMessage, currentPulses);
           track.add(event);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private boolean shouldBeOutput(int drum, int index, double generatedNum) {
        return generatedNum <= probabilities[index].get(drum);
    }

    public String getSection() {
        return this.section;
    }

    public Sequence getSequence() {
        return this.seq;
    }

    public Track getTrack() {
        return this.track;
    }

    public void printMidiFile(String name) {
        File outputFolder = new File("Output/" + getSection() + "/");
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        LocalDateTime now = LocalDateTime.now();
        String formattedDateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
        File out = new File(outputFolder + "/" + name + "-" + formattedDateTime + ".mid");
        try {
            MidiSystem.write(seq, 1, out);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        DrumPatternReader reader = new DrumPatternReader("Chorus");
        reader.work();
        reader = new DrumPatternReader("Verse");
        reader.work();
        DrumGenerator gen = new DrumGenerator("Chorus");
        gen.generateTrack();
        gen.printMidiFile("chorus-loop");
        gen = new DrumGenerator("Verse");
        gen.generateTrack();
        gen.printMidiFile("verse-loop");
    }

}



