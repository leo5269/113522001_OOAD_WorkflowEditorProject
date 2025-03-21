import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class WorkflowEditor extends JFrame {
    private CanvasPanel canvasPanel;
    private String mode = "Select"; // 預設模式
    //private JButton selectedButton = null; // 紀錄當前選中的按鈕

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

// 創建選單列
private JMenuBar createMenuBar() {
    JMenuBar menuBar = new JMenuBar();
    menuBar.setBackground(Color.LIGHT_GRAY); // 設定背景顏色

    JMenu fileMenu = new JMenu("File");
    JMenu editMenu = new JMenu("Edit");

    fileMenu.setOpaque(true);
    fileMenu.setBackground(Color.LIGHT_GRAY); // 設定背景顏色
    fileMenu.setFont(new Font("Arial", Font.PLAIN, 14)); // 設定字體大小

    editMenu.setOpaque(true);
    editMenu.setBackground(Color.LIGHT_GRAY);
    editMenu.setFont(new Font("Arial", Font.PLAIN, 14));

    menuBar.add(fileMenu);
    menuBar.add(editMenu);
    return menuBar;
}

// 創建左側工具欄
private JPanel createToolPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // 垂直排列
    panel.setBackground(Color.WHITE); // 設定背景純白

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
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // 移除額外的框線

    // 創建標籤（文字顯示）
    JLabel label = new JLabel(labelText);
    label.setHorizontalAlignment(SwingConstants.LEFT);
    label.setPreferredSize(new Dimension(100, 40)); // 控制標籤大小
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
    
    // 統一設定圖示大小（確保所有圖示大小相同）
    Image image = originalIcon.getImage().getScaledInstance(50, 40, Image.SCALE_SMOOTH);
    ImageIcon scaledIcon = new ImageIcon(image);

    JButton button = new JButton(scaledIcon);
    button.setPreferredSize(new Dimension(50, 50)); // 確保所有按鈕大小一致
    button.setMinimumSize(new Dimension(50, 50));
    button.setMaximumSize(new Dimension(50, 50));

    button.setFocusPainted(false);
    button.setBorder(BorderFactory.createEmptyBorder()); // 確保不影響圖示顯示
    button.setContentAreaFilled(false);
    button.setOpaque(true);
    button.setBackground(Color.WHITE);
    button.setMargin(new Insets(0, 0, 0, 0)); // 取消內邊距

    return button;
}

// 按鈕點擊事件（變色顯示選擇狀態）
private JButton selectedButton = null;
private void handleButtonClick(JButton button, String modeName) {
    if (selectedButton != null) {
        selectedButton.setBackground(Color.WHITE);
    }
    button.setBackground(Color.LIGHT_GRAY);
    selectedButton = button;
    
    // 設定當前模式
    mode = modeName.toLowerCase(); // "rect" 或 "oval"
}
    // 取得當前模式
    public String getMode() {
        return mode;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WorkflowEditor::new);
    }
}

class CanvasPanel extends JPanel {
    private final List<Shape> shapes = new ArrayList<>();
    private final WorkflowEditor editor;
    private int startX, startY, endX, endY; // 用來儲存滑鼠拖曳時的座標
    private boolean isDragging = false; // 是否正在拖曳
    private String currentMode = ""; // 記錄當前模式 (rect or oval)

    public CanvasPanel(WorkflowEditor editor) {
        this.editor = editor;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(500, 350)); // 設定較小的 Canvas
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 1)); // 黑色邊框

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 只有在 "rect" 或 "oval" 模式時才開始繪製
                currentMode = editor.getMode();
                if (currentMode.equals("rect") || currentMode.equals("oval")) {
                    startX = e.getX();
                    startY = e.getY();
                    isDragging = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDragging) {
                    endX = e.getX();
                    endY = e.getY();
                    isDragging = false;

                    int width = Math.abs(endX - startX);
                    int height = Math.abs(endY - startY);
                    int x = Math.min(startX, endX);
                    int y = Math.min(startY, endY);

                    if (currentMode.equals("rect")) {
                        shapes.add(new RectangleShape(x, y, width, height));
                    } else if (currentMode.equals("oval")) {
                        shapes.add(new OvalShape(x, y, width, height));
                    }

                    repaint();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    endX = e.getX();
                    endY = e.getY();
                    repaint(); // 重繪畫布以顯示拖曳的形狀
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Canvas", 10, 20);

        for (Shape shape : shapes) {
            shape.draw(g);
        }

        // 顯示拖曳中的形狀
        if (isDragging) {
            int width = Math.abs(endX - startX);
            int height = Math.abs(endY - startY);
            int x = Math.min(startX, endX);
            int y = Math.min(startY, endY);

            g.setColor(Color.GRAY); // 畫出半透明的預覽框
            if (currentMode.equals("rect")) {
                g.drawRect(x, y, width, height);
            } else if (currentMode.equals("oval")) {
                g.drawOval(x, y, width, height);
            }
        }
    }
}



// 抽象類別 - 形狀
abstract class Shape {
    protected int x, y, width, height;

    public Shape(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void draw(Graphics g);
}

// 繼承 - 矩形
class RectangleShape extends Shape {
    public RectangleShape(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(new Color(198, 198, 198));
        g.fillRect(x, y, width, height);
    }
}

// 繼承 - 橢圓
class OvalShape extends Shape {
    public OvalShape(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(new Color(198, 198, 198));
        g.fillOval(x, y, width, height);
    }
}
