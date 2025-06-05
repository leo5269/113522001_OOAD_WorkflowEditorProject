package dialog;

import javax.swing.*;
import java.awt.*;
import shape.Shape;


// 標籤樣式對話框，用於編輯形狀的標籤屬性
public class LabelStyleDialog extends JDialog {
    private JTextField nameField;
    private JComboBox<String> shapeCombo;
    private JComboBox<String> colorCombo;
    private JTextField fontSizeField;
    private boolean confirmed = false;
    
    private static final String[] SHAPE_OPTIONS = {"Rect", "Oval"};
    private static final String[] COLOR_OPTIONS = {"White", "Yellow", "Red", "Blue", "Green", "Gray", "Black"};

    
    // 創建標籤樣式對話框  
    // @param parent 父視窗
    // @param shape 目標物件
    public LabelStyleDialog(JFrame parent, Shape shape) {
        super(parent, "Custom label Style", true);
        setLayout(new GridLayout(5, 2, 10, 10));
        setSize(300, 250);
        setLocationRelativeTo(parent);
        
        // 初始化元件
        nameField = new JTextField(shape.getLabelText() != null ? shape.getLabelText() : "");
        
        shapeCombo = new JComboBox<>(SHAPE_OPTIONS);
        shapeCombo.setSelectedItem(capitalize(shape.getLabelShape()));
        
        colorCombo = new JComboBox<>(COLOR_OPTIONS);
        colorCombo.setSelectedItem(colorToString(shape.getLabelColor()));
        
        fontSizeField = new JTextField(String.valueOf(shape.getLabelFontSize()));
        
        // 添加元件
        add(new JLabel("Name"));
        add(nameField);
        add(new JLabel("Shape"));
        add(shapeCombo);
        add(new JLabel("Color"));
        add(colorCombo);
        add(new JLabel("FontSize"));
        add(fontSizeField);
        
        // 按鈕
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> {
            confirmed = false;
            dispose();
        });
        
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        
        add(cancelBtn);
        add(okBtn);
    }
    
    public boolean isConfirmed() { // 檢查對話框是否確認
        return confirmed;
    }
    
    public String getLabelName() { // 取得標籤名稱
        return nameField.getText();
    }
    
    public String getLabelShape() { // 取得標籤形狀
        return ((String) shapeCombo.getSelectedItem()).toLowerCase();
    }
    
    public Color getLabelColor() { // 取得標籤顏色
        return parseColor((String) colorCombo.getSelectedItem());
    }
    
    public int getFontSize() { // 取得字體大小
        try {
            return Integer.parseInt(fontSizeField.getText());
        } catch (NumberFormatException e) {
            return 12; // 預設為12
        }
    }

    private Color parseColor(String name) { // 將顏色名稱解析為 Color 物件
        switch (name.toLowerCase()) {
            case "yellow": return Color.YELLOW;
            case "red": return Color.RED;
            case "blue": return Color.BLUE;
            case "green": return Color.GREEN;
            case "gray": return Color.GRAY;
            case "black": return Color.BLACK;
            case "white":
            default: return Color.WHITE;
        }
    }

    private String colorToString(Color c) { //  將 Color 物件轉換為顏色名稱
        if (c.equals(Color.YELLOW)) return "Yellow";
        if (c.equals(Color.RED)) return "Red";
        if (c.equals(Color.BLUE)) return "Blue";
        if (c.equals(Color.GREEN)) return "Green";
        if (c.equals(Color.GRAY)) return "Gray";
        if (c.equals(Color.BLACK)) return "Black";
        if (c.equals(Color.WHITE)) return "White";
        return "White"; // 預設為白色
    }
    
    private String capitalize(String str) { // 將字串首字母大寫
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}