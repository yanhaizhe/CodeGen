package me.hehaiyang.codegen.config.ui;

import com.google.common.collect.Lists;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.treeStructure.Tree;
import lombok.Data;
import me.hehaiyang.codegen.config.setting.TemplatesSetting;
import me.hehaiyang.codegen.model.CodeGroup;
import me.hehaiyang.codegen.model.CodeTemplate;
import me.hehaiyang.codegen.config.SettingManager;
import me.hehaiyang.codegen.config.UIConfigurable;
import me.hehaiyang.codegen.config.ui.template.TemplateEditor;
import me.hehaiyang.codegen.config.ui.template.TemplateTreeCellRenderer;
import me.hehaiyang.codegen.config.ui.variable.AddDialog;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Desc:
 * Mail: hehaiyang@terminus.io
 * Date: 2017/5/10
 */
@Data
public class TemplatesUI extends JBPanel implements UIConfigurable {

    private Tree templateTree;
    private ToolbarDecorator toolbarDecorator;
    private TemplateEditor templateEditor;

    private final SettingManager settingManager = SettingManager.getInstance();

    public TemplatesUI() {
        init();
        setTemplates(settingManager.getTemplatesSetting());
    }

    @Override
    public boolean isModified(){

        TemplatesSetting templatesSetting = settingManager.getTemplatesSetting();
        List<CodeGroup> groups = templatesSetting.getGroups();
        Map<String, List<CodeTemplate>> templateMap = templatesSetting.getTemplatesMap();

        DefaultMutableTreeNode rootNode=(DefaultMutableTreeNode)templateTree.getModel().getRoot();
        if(rootNode.getChildCount() != groups.size()){
            return true;
        }

        Enumeration enumeration = rootNode.children();
        while(enumeration.hasMoreElements()){
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
            CodeGroup group = (CodeGroup) node.getUserObject();
            if(node.getChildCount() != templateMap.get(group.getId()).size()){
                return true;
            }
            if(templateEditor != null){
                Enumeration childEnum = node.children();
                while(childEnum.hasMoreElements()){
                    DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) childEnum.nextElement();
                    CodeTemplate template = (CodeTemplate) childNode.getUserObject();
                    CodeTemplate tmp = templateEditor.getCodeTemplate();
                    if(template.getId().equals(tmp.getId()) && !template.equals(tmp)){
                       return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void apply() {
        List<CodeGroup> groups = Lists.newArrayList();
        DefaultTreeModel treeModel = (DefaultTreeModel) templateTree.getModel();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();
        Enumeration enumeration = rootNode.children();
        while(enumeration.hasMoreElements()){
            List<CodeTemplate> templates = Lists.newArrayList();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
            Enumeration childEnum = node.children();
            while(childEnum.hasMoreElements()){
                DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) childEnum.nextElement();
                CodeTemplate template = (CodeTemplate) childNode.getUserObject();
                if(templateEditor != null){
                    CodeTemplate tmp = templateEditor.getCodeTemplate();
                    if(template.getId().equals(tmp.getId())){
                        template = tmp;
                    }
                }
                templates.add(template);
            }
            CodeGroup group = (CodeGroup) node.getUserObject();
            group.setTemplates(templates);
            groups.add(group);
        }

        settingManager.getTemplatesSetting().setGroups(groups);
        setTemplates(settingManager.getTemplatesSetting());
    }

    @Override
    public void reset() {
        setTemplates(settingManager.getTemplatesSetting());
    }

    private void init(){
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        templateTree = new Tree();
        templateTree.putClientProperty("JTree.lineStyle", "Horizontal");
        templateTree.setRootVisible(true);
        templateTree.setShowsRootHandles(true);
        templateTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        templateTree.setCellRenderer(new TemplateTreeCellRenderer());

        templateTree.addTreeSelectionListener( it -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) templateTree.getLastSelectedPathComponent();
            if (node == null) return;
            Object object = node.getUserObject();
            if(object instanceof CodeTemplate) {
                CodeTemplate template = (CodeTemplate) object;
                templateEditor.refresh(template);
                System.out.println("你点击了：" + template.getDisplay());
            } else if(object instanceof String) {
                String text = (String)object;
                System.out.println("你点击了：" + text);
            }
        });

        toolbarDecorator = ToolbarDecorator.createDecorator(templateTree)
            .setAddAction( it -> addAction())
            .setRemoveAction( it -> removeAction())
            .setEditAction(it -> editorAction())
            .setEditActionUpdater( it -> {
                final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) templateTree.getLastSelectedPathComponent();
                return selectedNode != null && selectedNode.getParent() != null && !(selectedNode.getUserObject() instanceof CodeTemplate);
            })
            .setAddActionUpdater( it -> {
                final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) templateTree.getLastSelectedPathComponent();
                return selectedNode != null && !(selectedNode.getUserObject() instanceof CodeTemplate);
            })
            .setRemoveActionUpdater( it -> {
                final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) templateTree.getLastSelectedPathComponent();
                return selectedNode != null && selectedNode.getParent() != null;
            });
        JPanel templatesPanel = toolbarDecorator.createPanel();
        templatesPanel.setPreferredSize(new Dimension(200,100));
        add(templatesPanel, BorderLayout.WEST);

        templateEditor = new TemplateEditor();
        add(templateEditor, BorderLayout.CENTER);
    }

    private void addAction(){
        //获取选中节点
        final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) templateTree.getLastSelectedPathComponent();
        //如果节点为空，直接返回
        if (selectedNode == null) {
            return;
        }
        Object object = selectedNode.getUserObject();
        if(object instanceof CodeTemplate) return;
        if(object instanceof String) {
            // 新增模版组
            JDialog dialog = new AddDialog();
            dialog.setTitle("Create New Group");
            dialog.setLayout(new BorderLayout());

            JPanel form = new JPanel(new GridLayout(2,2));
            form.add(new Label("Group Name"));
            JTextField keyJTextField = new JTextField();
            form.add(keyJTextField);

            dialog.add(form, BorderLayout.CENTER);

            JButton add = new JButton("ADD");
            add.addActionListener( it ->{
                String key = keyJTextField.getText();

                addNode(selectedNode, new DefaultMutableTreeNode(key));

                dialog.setVisible(false);
            });
            dialog.add(add, BorderLayout.SOUTH);

            dialog.setSize(300, 100);
            dialog.setAlwaysOnTop(true);
            dialog.setLocationRelativeTo(this);
            dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setResizable(false);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
        }
        if(object instanceof CodeGroup){
            // 新增模版
            CodeGroup group = (CodeGroup) object;

            JDialog dialog = new AddDialog();
            dialog.setTitle("Create New Template");
            dialog.setLayout(new BorderLayout());

            JPanel form = new JPanel(new GridLayout(2,2));
            form.add(new Label("display"));
            JTextField displayJTextField = new JTextField();
            form.add(displayJTextField);
            form.add(new Label("extension"));
            JTextField extensionJTextField = new JTextField();
            form.add(extensionJTextField);

            dialog.add(form, BorderLayout.CENTER);

            JButton add = new JButton("ADD");
            add.addActionListener( it ->{
                String display = displayJTextField.getText();
                String extension = extensionJTextField.getText();

                addNode(selectedNode, new DefaultMutableTreeNode(new CodeTemplate(UUID.randomUUID().toString(), display, extension, "Unnamed", "")));

                dialog.setVisible(false);
            });
            dialog.add(add, BorderLayout.SOUTH);

            dialog.setSize(300, 100);
            dialog.setAlwaysOnTop(true);
            dialog.setLocationRelativeTo(this);
            dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setResizable(false);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
        }
    }

    private void addNode(DefaultMutableTreeNode pNode, MutableTreeNode newNode){
        //直接通过model来添加新节点，则无需通过调用JTree的updateUI方法
        //model.insertNodeInto(newNode, selectedNode, selectedNode.getChildCount());
        //直接通过节点添加新节点，则需要调用tree的updateUI方法
        pNode.add(newNode);
        //--------下面代码实现显示新节点（自动展开父节点）-------
        DefaultTreeModel model = (DefaultTreeModel) templateTree.getModel();
        TreeNode[] nodes = model.getPathToRoot(newNode);
        TreePath path = new TreePath(nodes);
        templateTree.scrollPathToVisible(path);
        templateTree.updateUI();
    }

    private void removeAction(){
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) templateTree.getLastSelectedPathComponent();
        if (selectedNode != null && selectedNode.getParent() != null) {
            //删除指定节点
            DefaultTreeModel model = (DefaultTreeModel) templateTree.getModel();
            model.removeNodeFromParent(selectedNode);
        }
    }

    private void editorAction(){
        TreePath selectedPath = templateTree.getSelectionPath();
        if (selectedPath != null) {
            //编辑选中节点
            templateTree.startEditingAtPath(selectedPath);
        }
    }

    private void setTemplates(TemplatesSetting templatesSetting){

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Template Group");

        List<CodeGroup> groups = templatesSetting.getGroups();
        groups.forEach( it -> {
            DefaultMutableTreeNode group = new DefaultMutableTreeNode(it);
            it.getTemplates().forEach( template -> {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(template);
                group.add(node);
            });
            root.add(group);
        });
        templateTree.setModel(new DefaultTreeModel(root, false));
    }

    public static void main(String[] args) {
        JFrame jFrame = new JFrame();
        jFrame.getContentPane().add(new TemplatesUI(), BorderLayout.CENTER);
        jFrame.setSize(300, 240);
        jFrame.setLocation(300, 200);
        jFrame.setVisible(true);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}