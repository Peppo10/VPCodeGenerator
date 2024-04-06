package org.codegenerator.utils;

import com.vp.plugin.view.IDialog;
import com.vp.plugin.view.IDialogHandler;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import static org.codegenerator.utils.FormatUtils.listTypeJava;

public class ChooseListDialogHandler implements IDialogHandler {
    public static final String TAG = "ChooseList";

    final private String from, to;

    private static String choose = (String) listTypeJava.keySet().toArray()[0];

    public static Boolean applyAlways = false;

    private IDialog dialogRef;

    public ChooseListDialogHandler(String from, String to){
        this.from = from;
        this.to = to;
    }

    public String getChoose() {
        return choose;
    }

    @Override
    public Component getComponent() {
        JPanel chooseListPanel = new JPanel();
        chooseListPanel.setLayout(new BoxLayout(chooseListPanel, BoxLayout.Y_AXIS));
        chooseListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel containsListLabel = GUI.createLabel(from + " contains a list of " + to, null ,SwingConstants.CENTER , 13, Font.BOLD);

        JPanel containsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        containsPanel.add(containsListLabel);

        JPanel choosePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel chooseLabel = GUI.createLabel("Which type of list do you want to use? ");
        JComboBox<String> chooseListComboBox = new JComboBox<>(listTypeJava.keySet().toArray(new String[0]));
        chooseListComboBox.setSelectedItem(containsListLabel);
        choosePanel.add(chooseLabel);
        choosePanel.add(chooseListComboBox);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton chooseButton = new JButton("Choose");
        chooseButton.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                choose = (String) chooseListComboBox.getSelectedItem();
                dialogRef.close();
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
        JButton chooseForAllButton = new JButton("Choose for all");
        chooseForAllButton.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                choose = (String) chooseListComboBox.getSelectedItem();
                applyAlways = true;
                dialogRef.close();
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        buttonPanel.add(chooseButton);
        buttonPanel.add(chooseForAllButton);

        GUI.addAll(chooseListPanel, containsPanel, choosePanel, buttonPanel);

        return chooseListPanel;
    }

    @Override
    public void prepare(IDialog dialog) {
        GUI.prepareDialog(dialog, TAG);
        this.dialogRef = dialog;
    }

    @Override
    public void shown() {

    }

    @Override
    public boolean canClosed() {
        return false;
    }
}
