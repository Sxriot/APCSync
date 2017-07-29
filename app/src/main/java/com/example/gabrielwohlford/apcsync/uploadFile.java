package com.example.gabrielwohlford.apcsync;
//^^testing for commit
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
//rjowl was here...Koki was too
/**
 * Created by seb on 7/10/17.
 */
interface MyHandlerInterface
{
    void downloadComplete(String Message);
    void downloadProgress(String Message);
    void downloadStarted(String Message);
}

class uploadFile {


    MyHandlerInterface myHandler;

    public void setHandlerListener(MyHandlerInterface listener)
    {
        myHandler=listener;
    }

    private void myEventFired(final String Message)
    {
        if(myHandler!=null)
            myHandler.downloadComplete(Message);
    }

    private void eventFeedBack(final String Message)
    {
        if(myHandler!=null)
            myHandler.downloadProgress(Message);
    }

    private void eventDownloadStarted(final String Message)
    {
        if(myHandler!=null)
            myHandler.downloadStarted(Message);
    }


    private BufferedReader br = null;
    private FileReader fr = null;
    private DataOutputStream send;
    private DataInputStream receive;
    void upload(String filePath, Socket clientSock) {
        try {
            receive = new DataInputStream(clientSock.getInputStream());
            send = new DataOutputStream(clientSock.getOutputStream());

            File myFile = new File(filePath);
            long fileSize = myFile.length();
/*
            fr = new FileReader(filePath);
            br = new BufferedReader(fr);
*/
//            send.writeUTF("1");
            send.writeBytes("1");
            int action = 0;
            System.out.println("- Mofup-WAITING Action Data Response");
            try {
                // http://www.asciitable.com/index/asciifull.gif
                action = Character.getNumericValue(receive.read());
                //System.out.println("Server says " + receive.readUTF());
            } catch (Exception e) {
                Log.w("Mofup-In catch", e.toString());
            }
            System.out.println("Mofup- Action Data Confirmed: " + action);
            if(action != 1) // means server didn't send the right acknowledgement
                return;

            send.writeBytes(filePath + "NAME");
            int nameRcv = -1;
            System.out.println("Mofup-WAITING name Response");
            try {
                nameRcv = Character.getNumericValue(receive.read());
//                    String d= receive.readUTF();
//                    System.out.println("Mofup-Name STRING: " + d);
            } catch (Exception e) {
                Log.w("In catch", e.toString());
            }
            System.out.println("Mofup-Name Confirmed: " + nameRcv);

            if (nameRcv == 1) { //receiving '1' = Name sent + file exists so don't upload the file
                System.out.println("File Exists");
                myEventFired(filePath);
            }
            else if (nameRcv == 0) {//receiving '0' = Name sent but file does not exist so upload the file
                if (fileSize > 100000000) {
                    sendByChunks(filePath);
                } else {
                    //readWholeFile(filePath);
                    sendByChunks(filePath);
                }

                System.out.println("Done Uploading..");

                send.writeBytes("ENDOFFILE");
                System.out.println("ENDOFFILE file uploaded successfully indicator Sent");

                System.out.println("Mofup-Receiving EndOfFile Response");
                try {
                    int  done = Character.getNumericValue(receive.read());
                } catch (Exception e) {
                    Log.w("In catch", e.toString());
                }
                System.out.println("Mofup-EndOfFile Confirmed!!");
                myEventFired(filePath);
            }
        } catch (IOException e) {
            Log.w("Mofup-Sending", e.toString());
            e.printStackTrace();
        }
/*
        finally {
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
*/
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
    private boolean readWholeFile(String FilePath){
        try {
            File myFile = new File(FilePath);
            byte[] mybytearray = new byte[(int) myFile.length()];
            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(mybytearray, 0, mybytearray.length);
            System.out.println("Sending " + FilePath + "(" + mybytearray.length + " bytes)");
            send.write(mybytearray, 0, mybytearray.length);
            send.flush();
        }catch (Exception e){
            return false;
        }
        return true;
    }

    private void sendByChunks(String file) throws IOException {
        File myFile = new File(file);
        long fileSize = myFile.length();
        int BUFFER_TO_SEND = 1024;
        int bufferSentTotal = 0;//Koki 07/27/2017 added BufferSend total for feedback
        int FeedbackIncrementor=1;

        int remaining = (int)fileSize;
        System.out.println("Size: "+remaining);

        byte[] buffer = fileSize<BUFFER_TO_SEND? new byte[(int)fileSize] : new byte[BUFFER_TO_SEND];


        FileInputStream in = new FileInputStream(file);
        int rc = in.read(buffer);
        eventDownloadStarted(file);
        eventFeedBack("\n"+"|");
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
            if (bufferSentTotal >= fileSize/10*FeedbackIncrementor){
                //eventFeedBack(bufferSentTotal+"/"+(int)fileSize+"kb");//Koki 07/27/2017 added feed back BufferSent total
                eventFeedBack("*");
                FeedbackIncrementor+=1;
            }

            // next read
            remaining-=BUFFER_TO_SEND;
            BUFFER_TO_SEND = remaining>1024? 1024:remaining;
            bufferSentTotal+=BUFFER_TO_SEND;
            buffer = new byte[BUFFER_TO_SEND];
            System.out.println("Sending:"+BUFFER_TO_SEND+" Remaining: "+remaining);
            if(BUFFER_TO_SEND==0)
                break;
            rc = in.read(buffer);
        }
    }
}
