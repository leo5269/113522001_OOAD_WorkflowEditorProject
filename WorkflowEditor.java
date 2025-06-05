import javax.swing.*;
import java.awt.*;
import java.util.List;

import shape.CompositeShape;
import shape.Shape;
import shape.ShapeHandler;
import tool.ToolMode;

// WorkflowEditor 主視窗類別
public class WorkflowEditor extends JFrame {
    private CanvasPanel canvasPanel;
    private String mode = ToolMode.SELECT.getName(); // 預設模式
    private JButton selectedButton = null; // 紀錄當前選中的按鈕
    
    // 編輯選單
    private JMenu editMenu;
    private JMenuItem labelItem, groupItem, ungroupItem;

    // 創建WorkflowEditor
    public WorkflowEditor() {
        setTitle("Workflow Design Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLayout(new BorderLayout());

        setJMenuBar(createMenuBar()); // 創建選單

        JPanel buttonPanel = createToolPanel(); // 創建工具列（按鈕）

        canvasPanel = new CanvasPanel(this); // 創建Canvas
        
        // 添加元件
        add(buttonPanel, BorderLayout.WEST);
        add(canvasPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    // 創建選單列
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(Color.LIGHT_GRAY);
    
        JMenu fileMenu = new JMenu("File");
        editMenu = new JMenu("Edit");
        
        // group 功能
        groupItem = new JMenuItem("Group");
        groupItem.addActionListener(e -> canvasPanel.groupSelectedShapes());
    
        ungroupItem = new JMenuItem("UnGroup");
        ungroupItem.addActionListener(e -> canvasPanel.ungroupSelectedShape());

        // label 功能
        labelItem = new JMenuItem("label");
        labelItem.addActionListener(e -> canvasPanel.showLabelDialogForSelectedShape());
    
        // 預設先加 group/ungroup
        editMenu.add(groupItem);
        editMenu.add(ungroupItem);
    
        fileMenu.setFont(new Font("Arial", Font.PLAIN, 14));
        editMenu.setFont(new Font("Arial", Font.PLAIN, 14));
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
    
        return menuBar;
    }    

    // 創建工具面板
    private JPanel createToolPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);

        // 創建按鈕區塊(文字在左，圖示按鈕在右)
        for (ToolMode toolMode : ToolMode.values()) {
            panel.add(createLabeledButton(toolMode.getName(), "icons/" + toolMode.getName() + ".png"));
        }

        return panel;
    }

    // 創建帶label的按鈕
    private JPanel createLabeledButton(String labelText, String iconPath) {
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 創建label(文字顯示)
        JLabel label = new JLabel(labelText);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setPreferredSize(new Dimension(100, 40));
        label.setFont(new Font("Arial", Font.PLAIN, 14));

        // 創建按鈕(只有包含圖示)
        JButton button = createToolButton(iconPath);

        // 設定按鈕點擊行為(點擊時變色)
        button.addActionListener(e -> handleButtonClick(button, labelText));

        // 將label和按鈕加入 Panel
        buttonPanel.add(label, BorderLayout.WEST);
        buttonPanel.add(button, BorderLayout.EAST);

        return buttonPanel;
    }

    // 創建工具按鈕
    private JButton createToolButton(String iconPath) {
        ImageIcon originalIcon = new ImageIcon(iconPath);
        Image image = originalIcon.getImage().getScaledInstance(50, 40, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(image);

        JButton button = new JButton(scaledIcon);
        button.setPreferredSize(new Dimension(50, 50));
        button.setMinimumSize(new Dimension(50, 50));
        button.setMaximumSize(new Dimension(50, 50));

        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(Color.WHITE);
        button.setMargin(new Insets(0, 0, 0, 0));

        return button;
    }

    // 處理按鈕點擊事件
    private void handleButtonClick(JButton button, String modeName) {
        if (selectedButton != null) {
            selectedButton.setBackground(Color.WHITE);
        }
        button.setBackground(Color.BLACK);
        selectedButton = button;

        // 設定當前模式
        mode = modeName.toLowerCase();
    }

    // 更新編輯選單
    public void updateEditMenuForSelection(List<Shape> selectedShapes) {
        editMenu.removeAll();
    
        if (selectedShapes.size() == 1) {
            Shape shape = selectedShapes.get(0);
            
            // 使用 ShapeHandler 來決定顯示哪些選單項目
            MenuUpdater menuUpdater = new MenuUpdater();
            shape.accept(menuUpdater);
            
            if (menuUpdater.shouldShowLabel()) {
                editMenu.add(labelItem);
            }
            if (menuUpdater.shouldShowGroupUngroup()) {
                editMenu.add(groupItem);
                editMenu.add(ungroupItem);
            }
        } else if (selectedShapes.size() > 1) {
            // 選多個basic物件就顯示 group 按鈕
            editMenu.add(groupItem);
        }
    
        // 重新顯示 menu
        editMenu.revalidate();
        editMenu.repaint();
    }

    // 選單更新器
    private class MenuUpdater implements ShapeHandler {
        private boolean showLabel = false;
        private boolean showGroupUngroup = false;
        
        @Override
        public void handleBasicShape(Shape shape) {
            // basic shape顯示 label 選項
            showLabel = true;
            showGroupUngroup = false;
        }
        
        @Override
        public void handleCompositeShape(CompositeShape composite) {
            // composite shape顯示 group/ungroup 選項
            showLabel = false;
            showGroupUngroup = true;
        }
        
        public boolean shouldShowLabel() {
            return showLabel;
        }
        
        public boolean shouldShowGroupUngroup() {
            return showGroupUngroup;
        }
    }

    public String getMode() { // 取得當前模式
        return mode;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(WorkflowEditor::new);
    }
}