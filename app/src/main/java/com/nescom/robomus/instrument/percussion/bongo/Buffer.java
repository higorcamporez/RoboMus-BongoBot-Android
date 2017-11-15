/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nescom.robomus.instrument.percussion.bongo;



import android.app.Activity;
import android.util.Log;

import android.widget.TextView;

import com.nescom.robomus.communication.arduino.UsbService;
import com.nescom.robomus.communication.illposed.osc.OSCMessage;
import com.nescom.robomus_bongo.R;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Higor
 */
public class Buffer extends RobotAction{

    private List<OSCMessage> messages;
    protected List<Integer> idProcessedMsg;
    private Activity activity;
    private TextView textLog;

    private InetAddress serverIp;
    private String serverOscAddress;
    private int serverPort;
    private UsbService usbService;
    private Bongo bongo;

    public Buffer(Activity activity, Bongo bongo) {
        super(bongo);

        this.messages = new ArrayList<OSCMessage>();
        this.idProcessedMsg = new ArrayList<Integer>();
        this.textLog = (TextView) activity.findViewById(R.id.textViewLog);

        /* enviar robo para posicao inicial */
        if(bongo.getUsbService() != null)
            initialRobotPosition();

    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public InetAddress getServerIp() {
        return serverIp;
    }

    public void setServerIp(InetAddress serverIp) {
        this.serverIp = serverIp;
    }

    public String getServerOscAddress() {
        return serverOscAddress;
    }

    public void setServerOscAddress(String serverOscAddress) {
        this.serverOscAddress = serverOscAddress;
    }

    /*
      * Method to get the original message id from the server. The msg from server is converted to a unique byte.
      * Example: server msg id 7500 -> 7500%256 = 76 (id to send to arduino)
      * @paran format represents the msg format
     */
    public int getIdConfirmMessage(int idFromArduino){
        //imprimirId();

        for (Integer id: this.idProcessedMsg) {

            if( (id%128) == idFromArduino ){
                this.idProcessedMsg.remove(id);
                this.nBytes -= this.lastBytes;
                return id;
            }
        }
        return -1;
    }
    public void imprimirId(){
        for (Integer id: this.idProcessedMsg) {
            Log.i("vetorId = ",id.toString());
        }
    }
    public OSCMessage remove(){
        return messages.remove(0);
    }

    public void add(OSCMessage l){
        //comentario
        if(l.getArguments().get(0).equals("/synch") ){
            messages.add(0,l);
        }else{

        }
        messages.add(messages.size(), l);
    }

    public void remove(int n){
        for (int i = 0; i < n; i++) {
            messages.remove(i);
        }
    }
    
    public OSCMessage get(){
        if(messages.isEmpty()){
            return messages.get(0);
        }
        return null;
    }

    
    public Long getFirstTimestamp(){
        
        OSCMessage oscMsg = messages.get(0);
        System.out.println(oscMsg.getArguments().size());
        return (Long)oscMsg.getArguments().get(0);
        
    }
    
    public void print(){
        int cont =0;
        System.out.println("_________________buffer______________");
        
        for (OSCMessage message : messages) {
            System.out.println("------------ posicao = "+cont+" -------------");
            for (Object obj : message.getArguments()) {
               
                System.out.println(obj);
            }
            cont++;
        }
        System.out.println("_____________ End buffer_______________");
    }


    /*
     * pega o cabeçalho da msg
     * @paran msg osc
     */
    public String getHeader(OSCMessage oscMessage){
        String header = (String) oscMessage.getAddress();
                    
        if(header.startsWith("/"))
            header = header.substring(1);

        String[] split = header.split("/", -1);

        if (split.length >= 2) {
            header = split[1];
        }else{
            header = null;
        }
        return header;
    }

    /*
      * Method to write a message OSC on screep smartphone to debug
      * @paran format represents the msg format
      * @paran oscMessage the message to write on the screen
     */
    public void writeMsgLog(final String format , final OSCMessage oscMessage){



        String txt = "";
        for(Object arg: oscMessage.getArguments()){
            txt += arg.toString()+",";

        }

        final StringBuffer sb = new StringBuffer(txt);
        sb.setCharAt(txt.length()-1, ']');
        final TextView txtLog = this.textLog;
        bongo.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if(txtLog.getText().length() > 2000){
                    txtLog.setText("\n"+format+"\n");
                    txtLog.setText("Adress: "+oscMessage.getAddress()+" ["+sb.toString()+"\n");
                }else{
                    txtLog.append("\n"+format+"\n");
                    txtLog.append("Adress: "+oscMessage.getAddress()+" ["+sb.toString()+"\n");
                }

            }
        });






    }



    public void run() {
        
        Long timeSleep = (long)0;
        String header;

        while(true){


            if (!this.messages.isEmpty()&& (getBufferArduino() >15)  ) {

                final OSCMessage oscMessage = messages.get(0);
                if(oscMessage.getArguments().isEmpty()){
                    continue;
                }
                int idMessage = Integer.parseInt(oscMessage.getArguments().get(0).toString());

                if(getIdConfirmMessage(idMessage) != -1){ //o arduino aceita id até 128 apenas
                    continue;
                }
                //add the msg id in a list
                this.idProcessedMsg.add( idMessage );

                header = getHeader(oscMessage);

                if (header != null) {


                    switch (header) {
                        case "playBongoDefG":
                            this.playBongoDefG(oscMessage);
                            //this.writeMsgLog("playString: Format = [id, RT, dur]",oscMessage);
                            break;
                        case "playBongoDefA":
                            this.playBongoDefA(oscMessage);
                            //this.writeMsgLog("playNote: Format = [ id, RT, dur]",oscMessage);
                            break;

                        case "playBongoTogetherDef":
                            this.playBongoTogetherDef(oscMessage);
                            //this.writeMsgLog("playNote: Format = [ id, RT, dur]",oscMessage);
                            break;
                        case "playBongoTogether":
                            this.playBongoTogether(oscMessage);
                            //this.writeMsgLog("playNote: Format = [ id, RT, dur]",oscMessage);
                            break;
                        case "playBongoG":
                            this.playBongoG(oscMessage);
                            //this.writeMsgLog("playNote: Format = [ id, RT, dur]",oscMessage);
                            break;
                        case "playBongoA":
                            this.playBongoA(oscMessage);
                            //this.writeMsgLog("playNote: Format = [ id, RT, dur]",oscMessage);
                            break;
                        case "playNote":
                            this.playNote(oscMessage);
                           // this.writeMsgLog("testeMsg: Format OSC = [timestamp, id]",oscMessage);
                            break;
                        case "playHappyBirth":
                            //this.playHappyBirth(oscMessage);
                            //this.writeMsgLog("testeMsg: Format OSC = [timestamp, id]",oscMessage);
                            break;
                    }

                    remove();
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }


            }
        }
    }
    
    
}
