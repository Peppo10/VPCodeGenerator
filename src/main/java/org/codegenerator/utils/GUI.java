package org.codegenerator.utils;

import com.vp.plugin.ApplicationManager;
import com.vp.plugin.ViewManager;
import com.vp.plugin.diagram.shape.IClassUIModel;
import com.vp.plugin.model.IModelElement;
import com.vp.plugin.view.IDialog;
import org.codegenerator.logger.Logger;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GUI {
    public static final int DEFAULT_PADDING = 4;
    public static final int DEFAULT_BORDER_SIZE = 1;
    private static final Dimension defaultPaddingDimension = new Dimension(DEFAULT_PADDING, DEFAULT_PADDING);
    private static final Border defaultPaddingBorder = BorderFactory.createEmptyBorder(DEFAULT_PADDING, DEFAULT_PADDING,
            DEFAULT_PADDING, DEFAULT_PADDING);
    private static final Border defaultBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, DEFAULT_BORDER_SIZE, true), defaultPaddingBorder);
    public static final ViewManager viewManager = ApplicationManager.instance().getViewManager();

    public static final Color defaultError = new Color(255,0,0,255);

    private GUI(){}

    public static JComponent getPaddingComponent(Dimension dimension) {
        return (JComponent) Box.createRigidArea(dimension);
    }

    @SuppressWarnings("unused")
    public static JComponent getDefaultPaddingComponent() {
        return getPaddingComponent(defaultPaddingDimension);
    }

    @SuppressWarnings("unused")
    public static Border getDefaultBorder() {
        return defaultBorder;
    }

    @SuppressWarnings("unused")
    public static Border getDefaultTitledBorder(String title) {
        TitledBorder titledBorder = BorderFactory.createTitledBorder(title);
        titledBorder.setTitleFont(titledBorder.getTitleFont().deriveFont(Font.BOLD));
        return BorderFactory.createCompoundBorder(titledBorder, defaultPaddingBorder);
    }

    public static Point getCenterPoint() {
        Component rootComponent = viewManager.getRootFrame();
        Point rootPoint = rootComponent.getLocation();
        Dimension rootDimension = rootComponent.getSize();
        int xCoordinate = (int) (rootPoint.getX() + rootDimension.getWidth() / 2);
        int yCoordinate = (int) (rootPoint.getY() + rootDimension.getHeight() / 2);

        return new Point(xCoordinate, yCoordinate);
    }

    public static void addAll(Container container, int padding, Component... components) {
        Component lastComponent = components[components.length - 1];
        for (Component component : components) {
            container.add(component);
            if (padding > 0 && component != lastComponent) {
                JComponent defaultPaddingComponent = getPaddingComponent(new Dimension(padding, padding));
                container.add(defaultPaddingComponent);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void addAll(Container container, Component... components) {
        addAll(container, 0, components);
    }

    @SuppressWarnings("unused")
    public static void prepareDialog(IDialog dialog, String title) {
        Point point = getCenterPoint();
        dialog.pack();
        point.translate(-dialog.getWidth() / 2, -dialog.getHeight() / 2);
        dialog.setLocation(point);
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setTitle(String.join(" - ", Config.PLUGIN_NAME, title));
    }

    public static ImageIcon loadImage(String filePath, String description){
        return new ImageIcon(filePath, description);
    }

    public static ImageIcon loadImage(String filePath, String description, float scale) {
        ImageIcon imageIcon = loadImage(filePath, description);
        int scaledWidth = Math.round(imageIcon.getIconWidth() * scale);
        int scaledHeight = Math.round(imageIcon.getIconHeight() * scale);
        Image scaledImage = imageIcon.getImage().getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage, description);
    }

    @SuppressWarnings("unused")
    public static ImageIcon loadImage(String filePath, String description, int width, int height) {
        ImageIcon imageIcon = loadImage(filePath, description);
        Image scaledImage = imageIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage, description);
    }

    public static JButton createLinkButton(String text, URI uri, Icon icon) {
        JButton linkButton = icon == null ? new JButton(text) : new JButton(text, icon);
        Map<TextAttribute, Object> textAttributes = new HashMap<>(linkButton.getFont().getAttributes());
        linkButton.setOpaque(false);
        linkButton.setBackground(Color.LIGHT_GRAY);
        linkButton.setForeground(Color.BLUE);
        linkButton.setBorder(BorderFactory.createEmptyBorder());
        linkButton.setBorderPainted(false);
        linkButton.setFocusable(false);
        linkButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        linkButton.addActionListener(e -> {
            if (Desktop.isDesktopSupported()){
                try {
                    Desktop.getDesktop().browse(uri);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
                try {
                    Desktop.getDesktop().mail(uri);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw new UnsupportedOperationException("Desktop is not supported for browsing");
        });
        linkButton.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                // Empty
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // Empty
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                /// Empty
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                textAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                linkButton.setFont(linkButton.getFont().deriveFont(textAttributes));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                textAttributes.put(TextAttribute.UNDERLINE, -1);
                linkButton.setFont(linkButton.getFont().deriveFont(textAttributes));
            }

        });
        return linkButton;
    }

    public static JButton createLinkButton(String text, URI uri) {
        return createLinkButton(text, uri, null);
    }


    public static JLabel createLabel(String text, Icon icon, int horizontalAlignment, float size, int style) {
        JLabel label = new JLabel(text, icon, horizontalAlignment);
        label.setFont(label.getFont().deriveFont(style,size));
        return label;
    }

    public static JLabel createLabel(String text, Icon icon, int horizontalAlignment, float size) {
        return createLabel(text, icon, horizontalAlignment, size, Font.PLAIN);
    }

    public static JLabel createLabel(String text, Icon icon, int horizontalAlignment) {
        return createLabel(text, icon, horizontalAlignment, 11);
    }

    public static JLabel createLabel(String text, Icon icon) {
        return createLabel(text, icon, SwingConstants.LEADING);
    }

    @SuppressWarnings("unused")
    public static JLabel createLabel(String text) {
        return createLabel(text, null);
    }

    private static void disableTextFields(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTextField) {
                ((JTextField) component).setEditable(false);
            } else if (component instanceof Container) {
                disableTextFields((Container) component);
            }
        }
    }

    public static JFileChooser createGeneratorFileChooser(String title) {
        String fullTitle = String.join(" - ", Config.PLUGIN_NAME, title);
        JFileChooser fileChooser = viewManager.createJFileChooser();

        fileChooser.setLocale(Locale.ENGLISH);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setName(fullTitle);
        fileChooser.setDialogTitle(fullTitle);
        fileChooser.setToolTipText(fullTitle);
        fileChooser.setApproveButtonText("Generate");
        fileChooser.setApproveButtonToolTipText(title);
        fileChooser.setSelectedFile(new File(Config.GENERATE_CODE_DEFAULT_PATH));

        disableTextFields(fileChooser);

        return fileChooser;
    }

    public static void showInformationMessageDialog(Component component, String title, String msg) {
        viewManager.showMessageDialog(component, msg, String.join(" - ", Config.PLUGIN_NAME, title),
                JOptionPane.INFORMATION_MESSAGE, getImageIcon());
    }

    @SuppressWarnings("unused")
    public static void showWarningMessageDialog(Component component, String title, String msg) {
        viewManager.showMessageDialog(component, msg, String.join(" - ", Config.PLUGIN_NAME, title),
                JOptionPane.WARNING_MESSAGE, getWarningImageIcon());
    }

    @SuppressWarnings("unused")
    public static void showErrorMessageDialog(Component component, String title, String msg) {
        viewManager.showMessageDialog(component, msg, String.join(" - ", Config.PLUGIN_NAME, title),
                JOptionPane.ERROR_MESSAGE, getImageIcon());
    }

    public static ImageIcon getWarningImageIcon() {
        String discoverImagePath = String.join(File.separator, Config.ICONS_PATH, "CodeGeneratorWarning.png");
        return GUI.loadImage(discoverImagePath, "warning icon", 0.1f);
    }

    public static ImageIcon getImageIcon() {
        String discoverImagePath = String.join(File.separator, Config.ICONS_PATH, "CodeGenerator.png");
        return GUI.loadImage(discoverImagePath, "Code generator icon", 0.1f);
    }

    public static void showErrorParsingMessage(IClassUIModel iClassUIModel, IModelElement modelElement, String message){
        Logger.queueErrorMessage(message);
        iClassUIModel.setForeground(defaultError);
        modelElement.addPropertyChangeListener(evt -> iClassUIModel.setForeground(new Color(0,0,0)));
    }
}
