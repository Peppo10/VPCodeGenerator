package org.codegenerator.utils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class CheckBoxNodeTreeRender extends DefaultTreeCellRenderer {
    private JCheckBox checkBox = new JCheckBox();
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component renderer = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        if (value instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();
            if (userObject instanceof String) {
                checkBox.setText((String) userObject);
            }
            checkBox.setSelected(false);
            return checkBox;
        }
        return renderer;
    }

    public static class CheckBoxNodeListener extends MouseAdapter {
        public void mouseClicked(MouseEvent event) {
            JTree tree = (JTree) event.getSource();
            int x = event.getX();
            int y = event.getY();
            int row = tree.getRowForLocation(x, y);
            TreePath path = tree.getPathForRow(row);
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node != null) {
                    JCheckBox checkBox = (JCheckBox) tree.getCellRenderer().getTreeCellRendererComponent(tree, node, false, false, false, row, false);
                    checkBox.setSelected(!checkBox.isSelected());
                }
            }
        }
    }
}
