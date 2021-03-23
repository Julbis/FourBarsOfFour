
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

// TODO: IMPLEMENTERA LÄSNING AV EN HEL MAPP MED FILER
// TODO: SKRIV UT RÅA COUNTS TILL FIL
// TODO: GENERERA SANNOLIKHETER FRÅN COUNTS ISTÄLLET FÖR DIREKT VID LÄSNING
public class DrumReader {

    private static final int NOTE_ON = 0x90;
    private static final int NOTE_OFF = 0x80;
    private int readFiles = 0;
    private HashMap<Integer, Double>[] probabilities = new HashMap[64];

    public DrumReader() {
        initProbabilities();
    }

    public void read(String path) {
        Map<Integer, LinkedList<Integer>> queuedHits = new HashMap<Integer, LinkedList<Integer>>();
        try {
            Sequence seq = MidiSystem.getSequence(new File(path));
            readFiles++;
            int countOfNoteOns = 0;
            int trackSize = 0;
            for (Track track : seq.getTracks()) {
                System.out.println(track.ticks());
                trackSize = track.size();
//                System.out.println("Current track size: " + trackSize);
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage shortMessage = (ShortMessage) message;
                        if (shortMessage.getCommand() == NOTE_ON && shortMessage.getData2() != 0) {
                            countOfNoteOns++;
                            int key = shortMessage.getData1();
                            long tick = event.getTick();
                            int sixteenth = (int) tick / 240;
                            System.out.println("Read a " + key + " at " + tick);
                            LinkedList<Integer> queue = null;
                            if (queuedHits.containsKey(key)) {
                                queue = queuedHits.get(key);
                            } else {
                                queue = new LinkedList<Integer>();
                            }
                            queue.add(sixteenth);
                            System.out.println("Adding sixteenth number " + sixteenth + " to queue for drum hit " + key);
                            queuedHits.put(key, queue);

//                            if (probabilities[sixteenth].get(key) != null) {
//                                probabilities[sixteenth].put(key, (probabilities[sixteenth].get(key) + 1) / readFiles);
//                            } else {
//                                probabilities[sixteenth].put(key, (double)(1 / readFiles));
//                            }

                        } else {
                            int key = shortMessage.getData1();
                            int sixteenth = queuedHits.get(key).removeFirst();
                            System.out.println("Dequeued a hit for " + key + " at sixteenth number "+ sixteenth);
                            if (probabilities[sixteenth].get(key) != null) {
                                probabilities[sixteenth].put(key, (probabilities[sixteenth].get(key) + 1) / readFiles);
                            } else {
                                probabilities[sixteenth].put(key, (double) (1 / readFiles));
                            }
                        }
                    }
                }

            }
            for (LinkedList<Integer> queue : queuedHits.values()) {
                System.out.println(queue.toString());
            }
            for (int i = 0; i < probabilities.length; i++) {
                System.out.println(probabilities[i].toString());
            }
            System.out.println("Note on events: " + countOfNoteOns);
            System.out.println("Total events: " + trackSize);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void initProbabilities() {
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] = new HashMap<Integer, Double>();
        }
    }

    public HashMap<Integer, Double>[] getProbabilities() {
        return probabilities;
    }


// STULEN KOD NEDAN (från: https://stackoverflow.com/questions/3850688/reading-midi-files-in-java) :
//
//    public static final int NOTE_ON = 0x90;
//    public static final int NOTE_OFF = 0x80;
//    public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
//
//    public static void main(String[] args) throws Exception {
//        Sequence sequence = MidiSystem.getSequence(new File("test.mid"));
//
//        int trackNumber = 0;
//        for (Track track :  sequence.getTracks()) {
//            trackNumber++;
//            System.out.println("Track " + trackNumber + ": size = " + track.size());
//            System.out.println();
//            for (int i=0; i < track.size(); i++) {
//                MidiEvent event = track.get(i);
//                System.out.print("@" + event.getTick() + " ");
//                MidiMessage message = event.getMessage();
//                if (message instanceof ShortMessage) {
//                    ShortMessage sm = (ShortMessage) message;
//                    System.out.print("Channel: " + sm.getChannel() + " ");
//                    if (sm.getCommand() == NOTE_ON) {
//                        int key = sm.getData1();
//                        int octave = (key / 12)-1;
//                        int note = key % 12;
//                        String noteName = NOTE_NAMES[note];
//                        int velocity = sm.getData2();
//                        System.out.println("Note on, " + noteName + octave + " key=" + key + " velocity: " + velocity);
//                    } else if (sm.getCommand() == NOTE_OFF) {
//                        int key = sm.getData1();
//                        int octave = (key / 12)-1;
//                        int note = key % 12;
//                        String noteName = NOTE_NAMES[note];
//                        int velocity = sm.getData2();
//                        System.out.println("Note off, " + noteName + octave + " key=" + key + " velocity: " + velocity);
//                    } else {
//                        System.out.println("Command:" + sm.getCommand());
//                    }
//                } else {
//                    System.out.println("Other message: " + message.getClass());
//                }
//            }
//
//            System.out.println();
//        }
//
//    }

}
