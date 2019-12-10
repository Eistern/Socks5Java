package net.fit.gui.score;

import lombok.RequiredArgsConstructor;
import net.fit.GameModel;
import net.fit.proto.SnakesProto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RefreshScoreListener implements ActionListener {
    private final JTable scoreTable;
    private final GameModel model;

    @Override
    public void actionPerformed(ActionEvent e) {
        List<SnakesProto.GamePlayer> playerList = model.getPlayers().getPlayersList();
        Collector<SnakesProto.GamePlayer, ?, Map<String, Integer>> collector = Collectors.toMap(SnakesProto.GamePlayer::getName, SnakesProto.GamePlayer::getScore);
        Map<String, Integer> score = playerList.parallelStream().collect(collector);
        DefaultTableModel model = (DefaultTableModel) scoreTable.getModel();
        model.setRowCount(0);
        score.forEach((name, playerScore) -> {
            String[] row = new String[2];
            row[0] = name;
            row[1] = playerScore.toString();
            model.addRow(row);
        });
    }
}
