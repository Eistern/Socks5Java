package net.fit.gui.error;

import javax.swing.*;

public class ErrorBox {
    public static void showError(String content) {
        JOptionPane.showMessageDialog(new JFrame(), content, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
