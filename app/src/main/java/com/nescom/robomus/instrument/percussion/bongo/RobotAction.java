package com.nescom.robomus.instrument.percussion.bongo;

import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import android.view.View;


import com.nescom.robomus.communication.arduino.UsbService;
import com.nescom.robomus.communication.illposed.osc.OSCMessage;
import com.nescom.robomus.util.Note;
import com.nescom.robomus_bongo.R;

import java.util.Random;

/**
 * Created by Higor on 10/11/2016.
 */
public abstract class RobotAction extends Thread{

    private static final int MAX_ARDUINO_BUFFER = 64;

    private UsbService usbService;
    private Bongo bongo;
    protected int nBytes;
    protected int lastBytes;
    protected long timeFirstMessage;
    private int color;

    public RobotAction(Bongo bongo) {
        this.usbService = bongo.getUsbService();
        this.bongo = bongo;
        this.nBytes = 0;
        this.lastBytes = 0;

        this.timeFirstMessage = -1;
    }

    public int getBufferArduino(){
        return MAX_ARDUINO_BUFFER - nBytes;
    }


    /*
    * Method to put a robot in initial position. tone bar raised and in fret 0
    * Format OSC= [id]
    * Message to Arduino:  action Arduino code (XX)
    */
    public void initialRobotPosition() {
        Log.i("initialRobotPosition", "-");
    }

    /*
    * Method to play a Bongo Bass
    * Format OSC= [id, RT, dur, descent angle, rise angle]
    * Message to Arduino:  action Arduino code (30), string
    */
    public void playBongoDef1(OSCMessage oscMessage) {
        Log.i("RobotAction", "playBongoDef1() - inicio");

        Long idMessage = Long.parseLong(oscMessage.getArguments().get(0).toString());
        Integer relativeTime = Integer.parseInt(oscMessage.getArguments().get(1).toString());
        Short duration = Short.parseShort(oscMessage.getArguments().get(2).toString());


        byte low2RelativeTime =  (byte)(relativeTime&0xFF);
        byte lowRelativeTime =  (byte)((relativeTime>>8)&0xFF);
        byte high2RelativeTime =  (byte)((relativeTime>>16)&0xFF);
        byte highRelativeTime = (byte)(relativeTime>>24);

        byte lowDuration = (byte)(duration&0xFF);
        byte highDuration = (byte)(duration>>8);


        byte idMsgArduino = convertId( idMessage );
        byte[] data = { 10, idMsgArduino, highRelativeTime, high2RelativeTime, lowRelativeTime, low2RelativeTime, highDuration,
                lowDuration  };

        usbService.write(data);
        nBytes+=data.length;
        lastBytes = data.length;


        Log.i("RobotAction", "playBongoDef1() - fim");

    }
    public void playBongoDef2(OSCMessage oscMessage) {
        Log.i("RobotAction", "playBongoDef2() - inicio");

        Long idMessage = Long.parseLong(oscMessage.getArguments().get(0).toString());
        Short relativeTime = Short.parseShort(oscMessage.getArguments().get(1).toString());
        Short duration = Short.parseShort(oscMessage.getArguments().get(2).toString());


        byte low2RelativeTime =  (byte)(relativeTime&0xFF);
        byte lowRelativeTime =  (byte)((relativeTime>>8)&0xFF);
        byte high2RelativeTime =  (byte)((relativeTime>>16)&0xFF);
        byte highRelativeTime = (byte)(relativeTime>>24);
        byte lowDuration = (byte)(duration&0xFF);
        byte highDuration = (byte)(duration>>8);


        byte idMsgArduino = convertId( idMessage );
        byte[] data = { 20, idMsgArduino, highRelativeTime, high2RelativeTime, lowRelativeTime, low2RelativeTime, highDuration,
                lowDuration  };

        usbService.write(data);
        nBytes+=data.length;
        lastBytes = data.length;

        Log.i("RobotAction", "playBongoDef2() - fim");
    }


    public void playNote(OSCMessage oscMessage){
        Log.i("RobotAction", "playNote() - inicio");
        Long idMessage = Long.parseLong(oscMessage.getArguments().get(0).toString());
        Short relativeTime = Short.parseShort(oscMessage.getArguments().get(1).toString());
        Short duration = Short.parseShort(oscMessage.getArguments().get(2).toString());
        String symbolNote = oscMessage.getArguments().get(3).toString();


        Note note = new Note(symbolNote);
        if (timeFirstMessage == -1 || relativeTime == 0 ){
            timeFirstMessage = System.currentTimeMillis();
            while ((timeFirstMessage + relativeTime) > System.currentTimeMillis()) {
            }
            //timeFirstMessage = System.currentTimeMillis();
            playSoundSmartPhone(note.getFrequency(), duration);
        }else {
            while ((timeFirstMessage + relativeTime) > System.currentTimeMillis()) {
            }
            playSoundSmartPhone(note.getFrequency(), duration);
        }
        bongo.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                View v = bongo.getActivity().findViewById(R.id.view);
                v.setBackgroundColor(color);
                if(color == 0xFF0000FF){
                    color = 0xFF0066CC;
                }else{
                    color = 0xFF0000FF;
                }
                v.setBackgroundColor(color);
            }
        });
        Log.i("RobotAction", "playNote() - fim");
    }
    /*
    method to play a instrumentString with the press fret
    Format OSC = [timestamp, id, instrumentString, fret ]
    Message to Arduino:  action Arduino code (00), position, action server id
    */
    /*public void playNote(String id, FrettedNotePosition frettedNotePosition){

        byte instrumentString = frettedNotePosition.getInstrumentString().byteValue();
        byte fret =  frettedNotePosition.getFret().byteValue();

        byte idArduino = convertId( Long.parseLong(id) );
        byte[] data = {65, instrumentString, fret, idArduino };
        usbService.write(data);
    }*/

    /*
    * method to convert the id from server to send to arduino
    * @paran idFromServer the message id received from the server
    */
    public Byte convertId( Long idFromServer){
        return (byte)(idFromServer%128);
    }

    public void playSound(OSCMessage oscMessage) {
        System.out.println(oscMessage.getArguments().size()+" [3]"+oscMessage.getArguments().get(3)+" [4]"+oscMessage.getArguments().get(2));
        Integer duration = Integer.parseInt((String)oscMessage.getArguments().get(3));
        final int sampleRate = 8000;
        final int numSamples = (duration) * sampleRate;
        final double sample[] = new double[numSamples];
        Integer freqOfTone = 440; // hz
        byte generatedSnd[] = new byte[2 * numSamples];

        freqOfTone = Integer.parseInt((String)oscMessage.getArguments().get(2));




            // fill out the array
            for (int i = 0; i < numSamples; ++i) {
                sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
            }

            // convert to 16 bit pcm sound array
            // assumes the sample buffer is normalised.
            int idx = 0;
            for (final double dVal : sample) {
                // scale to maximum amplitude
                final short val = (short) ((dVal * 32767));
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

            }

            final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, numSamples,
                    AudioTrack.MODE_STATIC);
            audioTrack.write(generatedSnd, 0, generatedSnd.length);
            audioTrack.play();

    }


    public void playSoundSmartPhone(double frequency, double duration){
        final int sampleRate = 8000;
        final int numSamples = (int) ( (duration/1000) * sampleRate );
        final double sample[] = new double[numSamples];
        byte generatedSnd[] = new byte[2 * numSamples];

        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/frequency));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }

        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, numSamples,
                AudioTrack.MODE_STATIC);

        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();

    }
    /*
    public void playHappyBirth(OSCMessage oscMessage){
        int delay = 1500;

        List<Note> notes = new ArrayList<>();

        notes.add(new Note("E4"));
        notes.add(new Note("E4"));
        notes.add(new Note("F#4"));
        notes.add(new Note("E4"));
        notes.add(new Note("A4"));
        notes.add(new Note("G#4"));

        notes.add(new Note("E4"));
        notes.add(new Note("E4"));
        notes.add(new Note("F#4"));
        notes.add(new Note("E4"));
        notes.add(new Note("B4"));
        notes.add(new Note("A4"));
        notes.add(new Note("A4"));

        notes.add(new Note("C#5"));
        notes.add(new Note("C#5"));
        notes.add(new Note("E5"));
        notes.add(new Note("C#5"));
        notes.add(new Note("A4"));
        notes.add(new Note("G#4"));
        notes.add(new Note("F#4"));

        notes.add(new Note("D5"));
        notes.add(new Note("D5"));
        notes.add(new Note("C#5"));
        notes.add(new Note("A4"));
        notes.add(new Note("B4"));
        notes.add(new Note("A4"));
        notes.add(new Note("A4"));

        for (Note note: notes) {
            FrettedNotePosition notePosition = this.bongo.getNoteClosePosition(note);
            if(notePosition != null){
                if(bongo.getEmulate()) { // verifica se é emulacao. vai emitir som no celular
                    playSoundSmartPhone(note.getFrequency(), delay);
                }else{
                    playNote(oscMessage.getArguments().get(0).toString(), notePosition);
                }
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else{

                Log.i("playNote", "playNote: note "+note.getSymbol()+note.getOctavePitch() +" not possible");
            }
        }


    }*/


}