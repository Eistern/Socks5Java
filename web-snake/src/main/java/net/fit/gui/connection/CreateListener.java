package net.fit.gui.connection;

import lombok.RequiredArgsConstructor;
import net.fit.ConfigService;
import net.fit.GameModel;
import net.fit.thread.ThreadManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

@RequiredArgsConstructor
public class CreateListener implements ActionListener {
    private final GameModel model;
    private final ThreadManager threadManager;
    private final int homePort;

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            model.init(ConfigService.getSystemConfig(), "127.0.0.1", homePort, "Eistern_init_host");
            threadManager.activateMaster();
        } catch (ClassNotFoundException | IOException ex) {
            ex.printStackTrace();
        }
    }
}
