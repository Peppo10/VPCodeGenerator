package org.giuse.CodeGenerator.utils;
import com.vp.plugin.ApplicationManager;
import com.vp.plugin.ProjectManager;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.view.IDialog;
import com.vp.plugin.view.IDialogHandler;
import org.giuse.CodeGenerator.GenerateCodeActionController;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;

public class GenerateDialogHandler implements IDialogHandler {
    @Override
    public Component getComponent() {
        ProjectManager projectManager = ApplicationManager.instance().getProjectManager();

        IDiagramUIModel[] iDiagramUIModels = projectManager.getProject().toDiagramArray();


        // Create the root node
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

        // Create child nodes
        DefaultMutableTreeNode parent1 = new DefaultMutableTreeNode("Parent 1");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("Child 1");
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode("Child 2");
        parent1.add(child1);
        parent1.add(child2);

        DefaultMutableTreeNode parent2 = new DefaultMutableTreeNode("Parent 2");
        DefaultMutableTreeNode child3 = new DefaultMutableTreeNode("Child 3");
        parent2.add(child3);

        root.add(parent1);
        root.add(parent2);

        // Create a JTree with the root node
        JTree tree = new JTree(root);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Create a custom renderer for checkboxes
        tree.setCellRenderer(new CheckBoxNodeTreeRender());

        // Add mouse listener to handle checkbox selection
        tree.addMouseListener(new CheckBoxNodeTreeRender.CheckBoxNodeListener());

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(500,500));


        JPanel rootPanel = new JPanel();
        JPanel co = new JPanel();
        co.setPreferredSize(new Dimension(500,500));
        co.setLayout(new FlowLayout(FlowLayout.LEFT));
        co.add(scrollPane);
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.PAGE_AXIS));
        GUI.addAll(rootPanel, GUI.DEFAULT_PADDING, co);
        return rootPanel;
    }

    @Override
    public void prepare(IDialog dialog) {
        GUI.prepareDialog(dialog, GenerateCodeActionController.TAG);
    }

    @Override
    public void shown() {

    }

    @Override
    public boolean canClosed() {
        return true;
    }
}
