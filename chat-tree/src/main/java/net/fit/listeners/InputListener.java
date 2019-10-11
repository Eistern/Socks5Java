package net.fit.listeners;

import lombok.RequiredArgsConstructor;
import net.fit.dto.Message;
import net.fit.nodes.MessageManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.UUID;

@RequiredArgsConstructor
public class InputListener implements Runnable {
    private final MessageManager manager;
    private final String hostName;

    @Override
    public void run() {
        InputStreamReader reader = new InputStreamReader(System.in);
        while (true) {
            try {
                char[] inputBuffer = new char[256];
                reader.read(inputBuffer);
                String input = String.valueOf(inputBuffer);
                input = input.trim();

                Message message = new Message(UUID.randomUUID(), hostName, input, new Date());
                System.out.println(message.getPrintingRep());
                manager.addMessage(message, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
