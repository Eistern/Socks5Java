package fit.nsu;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    private static File uploadsDirectory;

    static File getUploadsDirectory() {
        return uploadsDirectory;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Argument format: port");
            return;
        }

        uploadsDirectory = new File("http-scp/server/resources/uploads");
        if (!uploadsDirectory.exists())
            if (!uploadsDirectory.mkdir()) {
                System.err.println("Can't create directory uploads");
                return;
            }

        ServerSocket listener = new ServerSocket(Integer.parseInt(args[0]));
        while (true) {
            Socket client = listener.accept();

            ClientService clientService = new ClientService(client);
            Thread clientThread = new Thread(clientService);
            clientThread.run();
        }
    }
}