package fit.nsu;

import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class ClientService implements Runnable {
    private final Socket client;
    private final Timer timer;
    private Date startTime;
    private volatile long bytesTransmitted;
    private String name;
    private volatile boolean hasSpeed;

    ClientService(Socket client) {
        this.client = client;
        this.timer = new Timer();
        this.hasSpeed = false;
    }

    private void speedTest() {
        hasSpeed = true;
        double time = new Date().getTime() - startTime.getTime();
        System.out.println("File " + name + " transmitting with speed: " + (bytesTransmitted * 8) / (time / 1000) + "b/s");
    }

    @Override
    public void run() {
        try {
            InputStream fromClient = client.getInputStream();
            OutputStream toClient = client.getOutputStream();

            ObjectInputStream fromClientStream = new ObjectInputStream(fromClient);
            String fileName = fromClientStream.readUTF();
            long fileLength = fromClientStream.readLong();

            name = fileName;

            File uploadsPath = Main.getUploadsDirectory();
            String separator = uploadsPath.toPath().getFileSystem().getSeparator();
            if (fileName.contains(separator))
                fileName = fileName.substring(fileName.indexOf(separator));

            FileOutputStream fileOutputStream = new FileOutputStream(uploadsPath.getPath() + separator + fileName);
            bytesTransmitted = 0;
            startTime = new Date();

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    speedTest();
                }
            };

            timer.schedule(task, 3000, 3000);
            byte[] input = new byte[256];
            int bytesGet;
            long bytesTransmittedLocal;
            while ((bytesGet = fromClient.read(input)) != -1) {
                bytesTransmittedLocal = bytesTransmitted;
                bytesTransmittedLocal += bytesGet;
                bytesTransmitted = bytesTransmittedLocal;
                fileOutputStream.write(input, 0, bytesGet);
            }
            timer.cancel();

            toClient.write(bytesTransmitted == fileLength ? 0 : 1);
            toClient.flush();

            if (!hasSpeed)
                speedTest();

            fileOutputStream.flush();
            fileOutputStream.close();

        } catch (IOException e) {
            System.err.println("Error with client");
            e.printStackTrace();
        }
        finally {
            timer.cancel();
            try {
                client.close();
            } catch (IOException e) {
                System.err.println("Error while closing socket");
                e.printStackTrace();
            }
        }
    }
}
