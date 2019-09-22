package fit.nsu;

import java.io.*;
import java.net.Socket;
import java.util.Date;

public class ClientService implements Runnable {
    private final Socket client;
    private Date startTime;
    private long bytesTransmitted;

    ClientService(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            InputStream fromClient = client.getInputStream();
            OutputStream toClient = client.getOutputStream();

            ObjectInputStream fromClientStream = new ObjectInputStream(fromClient);
            String fileName = fromClientStream.readUTF();
            long fileLength = fromClientStream.readLong();

            File uploadsPath = Main.getUploadsDirectory();
            String separator = uploadsPath.toPath().getFileSystem().getSeparator();

            FileOutputStream fileOutputStream = new FileOutputStream(uploadsPath.getPath() + separator + fileName);
            bytesTransmitted = 0;
            startTime = new Date();

            byte[] input = new byte[256];
            int bytesGet;
            while ((bytesGet = fromClient.read(input)) != -1) {
                bytesTransmitted += bytesGet;
                fileOutputStream.write(input, 0, bytesGet);
            }

            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
