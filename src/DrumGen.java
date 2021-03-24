
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.sound.midi.*;

/**
 * Mycket stulet från http://www.automatic-pilot.com/midifile.html
 *
 * Good shit: https://www.midi.org/specifications-old/item/table-2-expanded-messages-list-status-bytes
 *            https://www.midi.org/specifications-old/item/gm-level-1-sound-set
 *            https://www.recordingblogs.com/wiki/midi-set-tempo-meta-message
 *
 * @author Julius
 *
 */
public class DrumGen {
    private String section;
    private Sequence seq;
    private Track track;
    private Random rand = new Random();
    private HashMap<String, Double>[] probs = new HashMap[65]; // First element is empty to simulate starting at 1, easier logic.
    private HashMap<Integer, Double>[] probabilities = new HashMap[64];
    private DrumReader reader;
    private static final int NOTE_ON = 0x99; // Channel 10, note on
    private static final int NOTE_OFF = 0x89; // Channel 10, note off

    public DrumGen(String section) {
        this.section = section;
        initTrack();
//      initProbs();
        read();
        initProbabilities();
    }

    private void read() {
        reader = new DrumReader("Corpus/" + section, section);
//        reader.read();
        reader.work();
    }

    private void initProbabilities() {
        probabilities = reader.getProbabilities();
    }

    private void initTrack() {
        try {
            // Init track
            seq = new Sequence(javax.sound.midi.Sequence.PPQ, 24);
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

//    private void initProbs() {
//        for (int i = 1; i < probs.length; i++) {
//            probs[i] = new HashMap<String, Double>();
//        }
//        for (int i = 1; i < probs.length; i++) {
//            double prob = 0.75;
//            double probMod = 0.50 + rand.nextDouble() * (1.25 - 0.50); //Random multiplier;
//            if (i % 2 != 0) {
//                probs[i].put("kick", prob * probMod);
//                prob = 0.25;
//                probMod = 0.12 + rand.nextDouble() * (1.25 - 0.50);
//                probs[i].put("snare", prob * probMod);
//            } else {
//                probs[i].put("snare", prob * probMod);
//                prob = 0.25;
//                probMod = 0.12 + rand.nextDouble() * (1.25 - 0.50);
//                probs[i].put("kick", prob * probMod);
//            }
//            prob = 0.5;
//            probMod = 0.35 + rand.nextDouble() * (1.25 - 0.50); //Random multiplier
//            probs[i].put("hi-hat", prob * probMod);
//        }
//
//    }

    public Sequence getSequence() {
        return seq;
    }

    public void generateTrack() {
        long currentTick = 0;
        try {
            for (int i = 0; i < probabilities.length; i++) {
                double randNum = rand.nextDouble();
                /* value of 'key' determines which part of the drum kit is played */
                for (Integer key : probabilities[i].keySet()) {
                    if (shouldBeOutput(key, i, randNum)) {
                        ShortMessage drumHit = new ShortMessage();
                        drumHit.setMessage(NOTE_ON, key, 64);
                        MidiEvent event = new MidiEvent(drumHit, currentTick);
                        track.add(event);

                        drumHit = new ShortMessage();
                        drumHit.setMessage(NOTE_OFF, key, /*0x40*/ 64);
                        event = new MidiEvent(drumHit, currentTick + 6);
                        track.add(event);
                    }
                }
                currentTick += 6;
            }
           MetaMessage metaMessage = new MetaMessage();
           byte[] bet = {}; // empty array
           metaMessage.setMessage(0x2F,bet,0x00);
           MidiEvent event = new MidiEvent(metaMessage, currentTick);
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

//    public void generateTrack() {
//        long currentTick = 0;
//        for (int i = 1; i < probs.length; i++) {
//            double coinFlip = rand.nextDouble();
//            for (String key : probs[i].keySet()) {
//                if (shouldBeOutput(key, i, coinFlip)) {
//                  System.out.println("Going to print " + key + " to track.");
//                    addToTrack(key, currentTick);
//                }
//            }
//            currentTick += 6;
//        }
//        try {
//            MetaMessage metaMessage = new MetaMessage();
//            byte[] bet = {}; // empty array
//            metaMessage.setMessage(0x2F,bet,0x00);
//            MidiEvent event = new MidiEvent(metaMessage, currentTick);
//            track.add(event);
//        } catch (Exception e) {
//            System.out.println("Error caught during ending.");
//        }
//    }

//    private boolean shouldBeOutput(String drum, int index, double generatedNum) {
//        return generatedNum <= probs[index].get(drum);
//    }

//    private void addToTrack(String drum, long tick) {
//        int drumNote;
//        switch (drum) {
//            case "kick":
//                drumNote = 36;
//                break;
//            case "snare":
//                drumNote = 38;
//                break;
//            case "hi-hat":
//                drumNote = 42;
//                break;
//            default:
//                drumNote = -1;
//                break;
//        }
//        if (drumNote > 0) {
//            try {
////              System.out.println("Printing " + drumNote + " to track.");
//                ShortMessage drumMessage = new ShortMessage();
//                drumMessage.setMessage(0x99, drumNote, /*0x60*/ 64); // Channel 10, note on
//                MidiEvent event = new MidiEvent(drumMessage, tick);
//                track.add(event);
//
//                drumMessage = new ShortMessage();
//                drumMessage.setMessage(0x89, drumNote, /*0x40*/ 64); // Channel 10, note off
//                event = new MidiEvent(drumMessage, tick + 6);
//                track.add(event);
//            } catch (Exception e) {
//                System.out.println(e.getMessage());
//            }
//        }
//    }

    public static void main(String[] args) {
        DrumGen drLoop1 = new DrumGen("Verse");
        drLoop1.generateTrack();
        Sequence s1 = drLoop1.getSequence();
        File outputFolder1 = new File("Output/" + drLoop1.getSection());
        if (!outputFolder1.exists()) {
            outputFolder1.mkdir();
        }
        DrumGen drLoop2 = new DrumGen("Chorus");
        drLoop2.generateTrack();
        Sequence s2 = drLoop2.getSequence();
        File outputFolder2 = new File("Output/" + drLoop2.getSection());
        if (!outputFolder2.exists()) {
            outputFolder2.mkdir();
        }
        try {
            //Write to files
            LocalDateTime now = LocalDateTime.now();
            String fileName1 = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
            File f1 = new File(outputFolder1.getParent() + "/" + outputFolder1.getName() + "/" + fileName1 + ".mid");
            System.out.println();
            MidiSystem.write(s1,1,f1);
            System.out.println("Finished generating \"" + fileName1 + ".mid\".");
            String fileName2 = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
            File f2 = new File(outputFolder2.getParent() + "/" + outputFolder2.getName() + "/" + fileName2 + ".mid");
            MidiSystem.write(s2,1,f2);
            System.out.println("Finished generating \"" + fileName2 + ".mid\".");
        } catch (IOException e) {
            System.out.println("Couldn't write to file.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}




/**
 * Mycket stulet från http://www.automatic-pilot.com/midifile.html
 *
 * @author Julius
 *
 */

//public class MidiGenOperations {
//
//    private Sequence s;
//    private Track t;
//    private Random rand = new Random();
//
//    public MidiGenOperations() {
//        t = initTrack();
//    }
//
//    private Track initTrack() {
//        try {
//            s = new Sequence(javax.sound.midi.Sequence.PPQ,24);
//            t = s.createTrack();
////****  General MIDI sysex -- turn on General MIDI sound set  ****
//            byte[] b = {(byte)0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte)0xF7};
//            SysexMessage sm = new SysexMessage();
//            sm.setMessage(b, 6);
//            MidiEvent me = new MidiEvent(sm,(long)0);
//            t.add(me);
////****  set tempo (meta event)  ****
//            MetaMessage mt = new MetaMessage();
//            byte[] bt = {0x02, (byte)0x00, 0x00};
//            mt.setMessage(0x51 ,bt, 3);
//            me = new MidiEvent(mt,(long)0);
//            t.add(me);
////****  set track name (meta event)  ****
//            mt = new MetaMessage();
//            String TrackName = new String("midifile track");
//            mt.setMessage(0x03 ,TrackName.getBytes(), TrackName.length());
//            me = new MidiEvent(mt,(long)0);
//            t.add(me);
////****  set omni on  ****
//            ShortMessage mm = new ShortMessage();
//            mm.setMessage(0xB0, 0x7D,0x00);
//            me = new MidiEvent(mm,(long)0);
//            t.add(me);
////****  set poly on  ****
//            mm = new ShortMessage();
//            mm.setMessage(0xB0, 0x7F,0x00);
//            me = new MidiEvent(mm,(long)0);
//            t.add(me);
////****  set instrument to Piano  ****
//            mm = new ShortMessage();
//            mm.setMessage(0xC0, 0x00, 0x00);
//            me = new MidiEvent(mm,(long)0);
//            t.add(me);
//            return t;
//        } catch (Exception e) {
//            System.out.println("Error caught: " + e);
//            return null;
//        }
//    }
//
//    public Track randomize() {
//        int cap = 192;
//        int summedDuration = 0;
//        ShortMessage mm;
//        MidiEvent me;
//        MetaMessage mt;
//        do {
//            int randomNote = rand.nextInt(12) + 60;
//            int randomDuration = rand.nextInt(24) + 1;
//            if (randomNote > 64 && (randomNote % 2 == 0) && randomNote < 71) {
//                randomNote++;
//            } else if (randomNote == 61 || randomNote == 63) {
//                randomNote++;
//            }
//            try {
//                mm = new ShortMessage();
//                mm.setMessage(0x90,randomNote,0x60);
//                me = new MidiEvent(mm,(long)summedDuration);
//                t.add(me);
//
//                mm = new ShortMessage();
//                mm.setMessage(0x80,randomNote,0x40);
//                me = new MidiEvent(mm,(long)summedDuration + randomDuration);
//                t.add(me);
//                summedDuration += randomDuration;
//            } catch (Exception e){
//                System.out.println("Error caught during randomization.");
//            }
//        } while(summedDuration < cap);
//        try {
////**** Empty note to fill out for 60 ticks ****
//            mm = new ShortMessage();
//            mm.setMessage(0xFA, 0, 0);
//            me = new MidiEvent(mm, (long) 391);
//            t.add(me);
//            mm = new ShortMessage();
//            mm.setMessage(0xFC, 0, 0);
//            me = new MidiEvent(mm, (long) 381);
//            t.add(me);
//
////****  set end of track (meta event) 19 ticks later  ****
//            mt = new MetaMessage();
//            byte[] bet = {}; // empty array
//            mt.setMessage(0x2F,bet,0x00);
//            me = new MidiEvent(mt, (long) 381000);
//            t.add(me);
//            return t;
//        } catch (Exception e) {
//            System.out.println("Error caught during ending.");
//        }
//        return null;
//    }
//
//    public void outputMidiTrack() {
//        try {
////****  write the MIDI sequence to a MIDI file  ****
//            File f = new File("midifile.mid");
//            MidiSystem.write(s,1,f);
//        } catch (IOException e) {
//            System.out.println("Couldn't write to file.");
//        } catch (Exception e) {
//            System.out.println("lol");
//        }
//    }
//
//}


