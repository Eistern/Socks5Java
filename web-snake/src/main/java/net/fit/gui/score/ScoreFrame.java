package net.fit.gui.score;

import net.fit.GameModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;

public class ScoreFrame extends JFrame {
    JPanel pnPanel0;
    JButton btBut0;
    JTable tbTable1;

    public ScoreFrame(GameModel model) {
        super("Score");

        pnPanel0 = new JPanel();
        GridBagLayout gbPanel0 = new GridBagLayout();
        GridBagConstraints gbcPanel0 = new GridBagConstraints();
        pnPanel0.setLayout(gbPanel0);

        btBut0 = new JButton("Refresh");
        gbcPanel0.gridx = 0;
        gbcPanel0.gridy = 0;
        gbcPanel0.gridwidth = 20;
        gbcPanel0.gridheight = 2;
        gbcPanel0.fill = GridBagConstraints.BOTH;
        gbcPanel0.weightx = 1;
        gbcPanel0.weighty = 0;
        gbcPanel0.anchor = GridBagConstraints.NORTH;
        gbPanel0.setConstraints(btBut0, gbcPanel0);
        pnPanel0.add(btBut0);

        tbTable1 = new JTable();
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.setColumnIdentifiers(new String[] {"Name", "Score"});
        tbTable1.setModel(tableModel);
        JScrollPane scpTable1 = new JScrollPane(tbTable1);
        gbcPanel0.gridx = 0;
        gbcPanel0.gridy = 2;
        gbcPanel0.gridwidth = 20;
        gbcPanel0.gridheight = 18;
        gbcPanel0.fill = GridBagConstraints.BOTH;
        gbcPanel0.weightx = 1;
        gbcPanel0.weighty = 1;
        gbcPanel0.anchor = GridBagConstraints.NORTH;
        gbPanel0.setConstraints(scpTable1, gbcPanel0);
        pnPanel0.add(scpTable1);

        ActionListener refreshActionListener = new RefreshScoreListener(tbTable1, model);
        btBut0.addActionListener(refreshActionListener);

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setContentPane(pnPanel0);
        pack();
        setVisible(true);
    }
}
