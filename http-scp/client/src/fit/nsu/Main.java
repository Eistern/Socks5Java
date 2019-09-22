package fit.nsu;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Argument format: file_path server_ip server_port");
            return;
        }

        File file = new File(args[0]);
        long length = file.length();
        String fileName = file.getName();

        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(args[1], Integer.parseInt(args[2])));

        InputStream fromServer = socket.getInputStream();
        OutputStream toServer = socket.getOutputStream();

        ObjectOutputStream toServerStream = new ObjectOutputStream(toServer);
        toServerStream.writeUTF(fileName);
        toServerStream.writeLong(length);
        toServerStream.flush();

        FileInputStream fromFile = new FileInputStream(file);

        byte[] output = new byte[256];
        int bytesGet;
        while ((bytesGet = fromFile.read(output)) != -1)
            toServer.write(output, 0, bytesGet);

        socket.close();
    }
}
