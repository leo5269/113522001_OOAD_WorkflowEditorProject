import javax.swing.*;
import java.awt.*;

public class WorkflowEditor extends JFrame {
    private CanvasPanel canvasPanel;
    private String mode = "Select"; // 預設模式
    private JButton selectedButton = null; // 紀錄當前選中的按鈕

    public WorkflowEditor() {
        setTitle("Workflow Design Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLayout(new BorderLayout());

        // 創建選單
        setJMenuBar(createMenuBar());

        // 創建工具列（按鈕）
        JPanel buttonPanel = createToolPanel();

        // 創建畫布
        canvasPanel = new CanvasPanel(this);

        // 添加元件
        add(buttonPanel, BorderLayout.WEST);
        add(canvasPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(Color.LIGHT_GRAY);
    
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
    
        fileMenu.setOpaque(true);
        fileMenu.setBackground(Color.LIGHT_GRAY);
        fileMenu.setFont(new Font("Arial", Font.PLAIN, 14));
    
        editMenu.setOpaque(true);
        editMenu.setBackground(Color.LIGHT_GRAY);
        editMenu.setFont(new Font("Arial", Font.PLAIN, 14));
    
        // 新增 Group 與 UnGroup 選項
        JMenuItem groupItem = new JMenuItem("Group");
        groupItem.addActionListener(e -> canvasPanel.groupSelectedShapes());
        JMenuItem ungroupItem = new JMenuItem("UnGroup");
        ungroupItem.addActionListener(e -> canvasPanel.ungroupSelectedShape());
    
        editMenu.add(groupItem);
        editMenu.add(ungroupItem);
    
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        return menuBar;
    }
    

    // 創建左側工具欄
    private JPanel createToolPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);

        // 創建按鈕區塊（文字在左，圖示按鈕在右）
        panel.add(createLabeledButton("select", "icons/select.png"));
        panel.add(createLabeledButton("association", "icons/association.png"));
        panel.add(createLabeledButton("generalization", "icons/generalization.png"));
        panel.add(createLabeledButton("composition", "icons/composition.png"));
        panel.add(createLabeledButton("rect", "icons/rect.png"));
        panel.add(createLabeledButton("oval", "icons/oval.png"));

        return panel;
    }

    // 創建 "Label + 圖示按鈕" 的組合（確保背景純白）
    private JPanel createLabeledButton(String labelText, String iconPath) {
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 創建標籤（文字顯示）
        JLabel label = new JLabel(labelText);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setPreferredSize(new Dimension(100, 40));
        label.setFont(new Font("Arial", Font.PLAIN, 14));

        // 創建按鈕（僅包含圖示）
        JButton button = createToolButton(iconPath);

        // 設定按鈕點擊行為（點擊時變色）
        button.addActionListener(e -> handleButtonClick(button, labelText));

        // 將標籤和按鈕加入 Panel
        buttonPanel.add(label, BorderLayout.WEST);
        buttonPanel.add(button, BorderLayout.EAST);

        return buttonPanel;
    }

    // 創建僅包含 "圖示" 的按鈕（確保大小統一）
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

    // 按鈕點擊事件（變色顯示選擇狀態）
    private void handleButtonClick(JButton button, String modeName) {
        if (selectedButton != null) {
            selectedButton.setBackground(Color.WHITE);
        }
        button.setBackground(Color.BLACK);
        selectedButton = button;

        // 設定當前模式
        mode = modeName.toLowerCase();
    }

    // 取得當前模式
    public String getMode() {
        return mode;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WorkflowEditor::new);
    }
}
