package com.example.gabrielwohlford.apcsync;
//^^
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 * Created by seb on 7/10/17.
 */
interface MyHandlerInterface
{
    void downloadComplete(String Message);
}

class uploadFile {


    MyHandlerInterface myHandler;

    public void setHandlerListener(MyHandlerInterface listener)
    {
        myHandler=listener;
    }

    protected void myEventFired(final String Message)
    {
        if(myHandler!=null)
            myHandler.downloadComplete(Message);
    }


    BufferedReader br = null;
    FileReader fr = null;
    DataOutputStream send;
    DataInputStream receive;
    FileInputStream fis = null;
    BufferedInputStream bis = null;

    void upload(String filePath, Socket clientSock) {
        try {
            receive = new DataInputStream(clientSock.getInputStream());
/*
            File f = new File(filePath);
            DataInputStream dis = new DataInputStream(new FileInputStream(f)); //to read the file
            byte[] b = new byte[(int)f.length()]; //to store the bytes
            int l = dis.read(b); //stores the bytes in b
            dis.close();
*/
            File myFile = new File(filePath);
            long fileSize = myFile.length();
            fr = new FileReader(filePath);
            br = new BufferedReader(fr);
            send = new DataOutputStream(clientSock.getOutputStream());
//            send.writeUTF("1");
            send.writeBytes("1");

            String action = "0";
            System.out.println("Mofup-Waiting Action Data Confirmation");
            while (action.equals("0")) {
                try {
                    action = String.valueOf(receive.read());
                } catch (Exception e) {
                    Log.w("Mofup-In catch", e.toString());
                }
            }
            System.out.println("Mofup- Action Data Confirmation Received: " + action);

/*
            String fileSize = String.valueOf((int)f.length());
            send.writeBytes(fileSize);
            System.out.println("Mofup-Size"+(int)f.length());

            String buffer = "0";
            System.out.println("Mofup-Waiting Buffer");
            while(buffer.equals("0"))
            {
                try{
                    buffer = String.valueOf(receive.read());
                }catch (Exception e){
                    Log.w("In catch", e.toString());
                }
            }
            System.out.println("Mofup- Buffer Received: "+buffer);
*/

            send.writeBytes(filePath + "NAME");
            int namercv = -1;
            System.out.println("Mofup-Waiting name Confirmation");
            while (namercv == -1) {
                try {
                    namercv = receive.read();
                } catch (Exception e) {
                    Log.w("In catch", e.toString());
                }
            }
            System.out.println("Mofup-Name Confirmation Received: " + namercv);

            if(namercv == 49){
                System.out.println("File Exists");

                myEventFired(filePath);
                return;
            }
            if (fileSize > 100000000) {
                sendByChunks(filePath);
            } else{
                readWholeFile(filePath);
            }

            System.out.println("Done");

            send.writeBytes("ENDOFFILE");
            System.out.println("ENDOFFILE Sent");

            String done = "0";
            System.out.println("Mofup-Receiving EndOfFile Confirmation");
            while (done.equals("0")) {
                try {
                    done = String.valueOf(receive.read());
                } catch (Exception e) {
                    Log.w("In catch", e.toString());
                }
            }
            System.out.println("Mofup-EndOfFile Confirmation Received!");
            myEventFired(filePath);

        } catch (IOException e) {
            Log.w("Mofup-Sending", e.toString());
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                    System.out.println("Closed");
                }
                if (fr != null) {
                    fr.close();
                    System.out.println("Closed");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                System.out.println("Exception");
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void readByBytes(String path) throws IOException {

        InputStream in = new FileInputStream(path);
        int bytesRead, i = 0, a = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
//                byte[] b = line.getBytes();
                byte[] b = line.getBytes(Charset.forName("UTF-8"));
                send.write(b);
                send.flush();
                System.out.println(b);
                // process the line.
            }
        }
    }
    public boolean readWholeFile(String FilePath){
        try {
            File myFile = new File(FilePath);
            byte[] mybytearray = new byte[(int) myFile.length()];
            fis = new FileInputStream(myFile);
            bis = new BufferedInputStream(fis);
            bis.read(mybytearray, 0, mybytearray.length);
            System.out.println("Sending " + FilePath + "(" + mybytearray.length + " bytes)");
            send.write(mybytearray, 0, mybytearray.length);
            send.flush();
        }catch (Exception e){
            return false;
        }
        return true;
    }

    public void sendByChunks(String file) throws IOException {
        File myFile = new File(file);
        long fileSize = myFile.length();
        int bufferToSend = 1024;
        int remaining = (int)fileSize;
        System.out.println("Size: "+remaining);

        byte[] buffer = fileSize<bufferToSend? new byte[(int)fileSize] : new byte[bufferToSend];

        FileInputStream in = new FileInputStream(file);
        int rc = in.read(buffer);
        while(rc != -1)
        {
            StringBuilder builder = new StringBuilder();
            // rc should contain the number of bytes read in this operation.
            // do stuff...
            for(byte b:buffer) {
//                send.write(b);
                builder.append((char)b);
            }
//            System.out.println(builder);
            send.writeBytes(builder.toString());


            // next read
            remaining-=bufferToSend;
            bufferToSend = remaining>1024? 1024:remaining;
            buffer = new byte[bufferToSend];
            System.out.println("Sending:"+bufferToSend+" Remaining: "+remaining);
            if(bufferToSend==0)
                break;
            rc = in.read(buffer);
        }
    }
}
