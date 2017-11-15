package com.nescom.robomus.instrument.percussion.bongo;


import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import com.nescom.robomus.communication.arduino.UsbService;
import com.nescom.robomus.communication.illposed.osc.OSCListener;
import com.nescom.robomus.communication.illposed.osc.OSCMessage;
import com.nescom.robomus.communication.illposed.osc.OSCPortIn;
import com.nescom.robomus.communication.illposed.osc.OSCPortOut;
import com.nescom.robomus.instrument.Instrument;
import com.nescom.robomus_bongo.R;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Higor on 03/11/2017.
 */

public class Bongo extends Instrument {

    private volatile Buffer buffer;
    private OSCPortOut sender;
    private OSCPortIn receiver;
    private Activity activity;
    private  TextView textLog;
    private UsbService usbService;
    private Boolean emulate;

    //private PortControl portControl;
    public Bongo(String name, String OscAddress, int receivePort, String myIp) {
        super(name, OscAddress, receivePort, myIp);
    }
    public Bongo( String name, String OscAddress, int receivePort,
                    UsbService usbService, String myIp, Activity activity,TextView textLog) {

        super( name, OscAddress, receivePort, myIp);

        this.polyphony = 2;
        this.typeFamily = "Percussion";
        this.specificProtocol =
                "</playBongoDefG;relative_time_i;durationMillis_i>" + //grave
                "</playBongoDefA;relative_time_i;durationMillis_i>" + //agudo
                "</playBongoG;relative_time_i;durationMillis_i;descentAngle_i;riseAngle_i>" +
                "</playBongoA;relative_time_i;durationMillis_i;descentAngle_i;riseAngle_i>" +
                "</playBongoTogether;relative_time_i;durationMillis_i;descentAngle_i;riseAngle_i>"+
                "</playBongoTogetherDef;relative_time_i;durationMillis_i>"+
                "</playNote;relative_time_i;durationMillis_i;note_s>";

        if(usbService == null){
            this.emulate = true;
        }else{
            this.emulate = false;
        }

        this.usbService = usbService;

        this.buffer = new Buffer(activity, this);

        //Initializing the OSC Receiver
        this.receiver = null;
        try {

            this.receiver = new OSCPortIn(this.receivePort);
        }
        catch (SocketException ex) {
            Logger.getLogger(Bongo.class.getName()).log(Level.SEVERE, null, ex);

        }
        this.activity = activity;
        //clean view
        //final TextView textLog = (TextView) this.activity.findViewById(R.id.textViewLog);
        this.textLog = textLog;
        //this.textLog.setText("Waiting Connection...");
        listenThread();
        handshake();
        //this.textLog.setText("Waiting Connection...");

    }

    public UsbService getUsbService() {
        return usbService;
    }

    public Activity getActivity() {
        return activity;
    }

    public Boolean getEmulate() {
        return emulate;
    }


    public void handshake(){

        List args = new ArrayList<>();

        //instrument attributes
        args.add(this.name);
        args.add(this.myOscAddress);
        args.add(this.myIp);
        args.add(this.receivePort);
        args.add(this.polyphony);
        args.add(this.typeFamily);
        args.add(this.specificProtocol);



        OSCMessage msg = new OSCMessage("/handshake/instrument", args);
        OSCPortOut sender = null;

        try {
            //send de msg with the broadcast ip
            String s = this.myIp;
            String[] ip = s.split("\\.");
            String broadcastIp = ip[0]+"."+ip[1]+"."+ip[2]+".255";
            sender = new OSCPortOut(InetAddress.getByName(broadcastIp), this.receivePort);
        } catch (SocketException ex) {
            Logger.getLogger(Bongo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        try {
            sender.send(msg);
        } catch (IOException ex) {
            Logger.getLogger(Bongo.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    private void startBuffer(OSCMessage message){
        this.serverName = message.getArguments().get(0).toString();
        this.serverOscAddress = message.getArguments().get(1).toString();
        this.severIpAddress = message.getArguments().get(2).toString();
        this.sendPort = Integer.parseInt(message.getArguments().get(3).toString());
        //log screen

        final String s = "handshake: Format OSC = [oscAdd, ip, port]\n Adress:"+ message.getAddress()+
                " ["+ this.serverOscAddress+", "+ this.severIpAddress+", "+this.sendPort+"]\n";
        final TextView txtLog = textLog;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                txtLog.append(s);

            }
        });
        //end log screen
        try {
            this.buffer.setServerIp(InetAddress.getByName(this.severIpAddress) );
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.buffer.setServerOscAddress(this.serverOscAddress);
        this.buffer.setServerPort(this.sendPort);

        this.buffer.start();

        //Initializing the OSC sender
        this.sender = null;
        try {
            this.sender = new OSCPortOut(InetAddress.getByName(this.severIpAddress), this.sendPort);
        } catch (SocketException ex) {
            Logger.getLogger(Bongo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


    }

    public void listenThread(){
        System.out.println("Inicio p="+ this.receivePort+" end="+this.myOscAddress);

        OSCListener listener = new OSCListener() {

            public void acceptMessage(java.util.Date time, OSCMessage message) {

                String log = "";

                String header = (String) message.getAddress();
                header = header.substring(1);
                String[] split = header.split("\\/", -1);

                if(split.length == 2 && split[1].equals("handshake")){
                    startBuffer(message);
                }else{
                    log += message.getAddress()+" ";
                    List l = message.getArguments();
                    log += "[";
                    for (Object l1 : l) {
                        log +=l1+",";
                    }
                    log += ']';

                    //verificar se a mensagem é válida

                    buffer.add(message);
                }
                Log.d("Bongo","Message: "+log);
            }
        };
        this.receiver.addListener(this.myOscAddress+"/*", listener);
        this.receiver.startListening();

    }
    public void ConfirmMsgToServ(){

        List args = new ArrayList<>();
        args.add("action completed");

        OSCMessage msg = new OSCMessage(this.serverOscAddress+"/action/"+this.name, args);

        try {
            this.sender.send(msg);
        } catch (IOException ex) {
            Logger.getLogger(Bongo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    public void sendConfirmActionMessage( int idFromArduino){
        int idConveted = this.buffer.getIdConfirmMessage(idFromArduino);
        Byte b;

        Log.i("teste", "idConv="+idConveted);
        if(idConveted != -1){
            List args = new ArrayList<>();
            args.add(idConveted);
            OSCMessage msg = new OSCMessage(this.serverOscAddress+"/action", args);
            System.out.println("enviou conf");

            try {
                this.sender.send(msg);
            } catch (IOException ex) {
                Logger.getLogger(Bongo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }


    }
    public void disconnect() {
        if (this.serverOscAddress != null) {

            List args = new ArrayList<>();
            args.add(this.myOscAddress);
            OSCMessage msg = new OSCMessage(this.serverOscAddress + "/disconnect/instrument", args);

            try {
                this.sender.send(msg);
                receiver.stopListening();
                buffer.interrupt();
                this.sender.close();
                this.receiver.close();
            } catch (IOException ex) {
                Logger.getLogger(Bongo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void stop() {
        receiver.stopListening();
        buffer.interrupt();
        //this.sender.close();
        this.receiver.close();
    }




    public boolean isConneted(){
        return (this.serverOscAddress != null);
    }

}
