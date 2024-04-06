package org.codegenerator.utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;
import com.vp.plugin.view.IDialog;
import com.vp.plugin.view.IDialogHandler;
import org.codegenerator.AboutActionController;

public class AboutDialogHandler implements IDialogHandler {
    private Component getInfoPanel() {
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        JEditorPane infoEditorPane = new JEditorPane("text/html; charset=UTF-8", Config.PLUGIN_DESCRIPTION);
        Dimension infoTextAreaDimension = new Dimension(640, 100);

        infoEditorPane.setPreferredSize(infoTextAreaDimension);
        infoEditorPane.setOpaque(false);
        infoEditorPane.setEditable(false);
        infoEditorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (IOException | URISyntaxException exception) {
                    exception.printStackTrace();
                }
        });

        GUI.addAll(infoPanel, infoEditorPane);
        return infoPanel;
    }

    private Component getContactPanel() {
        JPanel contactPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        JLabel homepageLabel = new JLabel("Email: ");
        JButton homepageButton = GUI.createLinkButton(Config.PLUGIN_CONTACT, URI.create("mailto://"+Config.PLUGIN_CONTACT));
        Box homepageBox = new Box(BoxLayout.LINE_AXIS);

        GUI.addAll(homepageBox, homepageLabel, homepageButton);
        contactPanel.setBorder(GUI.getDefaultTitledBorder("Contact Information"));
        contactPanel.add(homepageBox);
        return contactPanel;
    }

    private Component getContentPanel() {
        JPanel contentPanel = new JPanel();
        String logoImagePath = String.join("/", Config.IMAGES_PATH, "logo.png");
        ImageIcon logoImageIcon = GUI.loadImage(logoImagePath, "CodeGenerator logo", 0.5f);
        JLabel logoLabel = new JLabel(Config.PLUGIN_VERSION, logoImageIcon, SwingConstants.CENTER);

        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        logoLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        logoLabel.setForeground(Color.BLUE);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.PAGE_AXIS));
        GUI.addAll(contentPanel, GUI.DEFAULT_PADDING, logoLabel, getInfoPanel(), getContactPanel());
        return contentPanel;
    }


    private Component getTeamPanel() {

        JPanel teamPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

        try {
            ClassLoader classLoader = getClass().getClassLoader();
            URL resource = classLoader.getResource(Config.PLUGIN_TEAM);

            JEditorPane infoTeamPane= new JEditorPane();

            infoTeamPane.setPage(resource);

            teamPanel.add(infoTeamPane, BorderLayout.CENTER);

            Dimension infoTextAreaDimension = new Dimension(640, 300);

            infoTeamPane.setPreferredSize(infoTextAreaDimension);
            infoTeamPane.setOpaque(false);
            infoTeamPane.setEditable(false);
            infoTeamPane.addHyperlinkListener(e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (IOException | URISyntaxException exception) {
                        exception.printStackTrace();
                    }
            });

            teamPanel.setLayout(new BoxLayout(teamPanel, BoxLayout.PAGE_AXIS));
            GUI.addAll(teamPanel, GUI.DEFAULT_PADDING,
                    getContactPanel());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return teamPanel;
    }
    private Component getLicensePanel() {
        JPanel licensePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

        try {

            ClassLoader classLoader = getClass().getClassLoader();
            URL resource = classLoader.getResource(Config.PLUGIN_LICENSE_FILE);

            JEditorPane infoEditorPane= new JEditorPane();
            infoEditorPane.setPage(resource);

            JScrollPane editorScrollPane = new JScrollPane(infoEditorPane);
            editorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

            JScrollPane scrollPane =new JScrollPane(infoEditorPane);

            scrollPane.setVisible(true);

            licensePanel.add(scrollPane, BorderLayout.CENTER);


            Dimension infoTextAreaDimension = new Dimension(640, 550);


            infoEditorPane.setPreferredSize(infoTextAreaDimension);
            infoEditorPane.setOpaque(false);
            infoEditorPane.setEditable(false);
            infoEditorPane.addHyperlinkListener(e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (IOException | URISyntaxException exception) {
                        exception.printStackTrace();
                    }
            });

            licensePanel.setLayout(new BoxLayout(licensePanel, BoxLayout.PAGE_AXIS));
            GUI.addAll(licensePanel, GUI.DEFAULT_PADDING, scrollPane,
                    getContactPanel());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return licensePanel;
    }


    @Override
    public boolean canClosed() {
        return true;
    }

    @Override
    public Component getComponent() {
        JTabbedPane rootPanel = new JTabbedPane();

        rootPanel.addTab("About", getContentPanel());
        rootPanel.addTab("License", getLicensePanel());
        rootPanel.addTab("Team", getTeamPanel());

        return rootPanel;
    }

    @Override
    public void prepare(IDialog dialog) {
        GUI.prepareDialog(dialog, AboutActionController.TAG);
    }

    @Override
    public void shown() {
        // Empty
    }

}

