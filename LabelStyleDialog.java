import javax.swing.*;
import java.awt.*;

class LabelStyleDialog extends JDialog {
    private JTextField nameField;
    private JComboBox<String> shapeCombo;
    private JTextField colorField;
    private JTextField fontSizeField;
    private boolean confirmed = false;

    public LabelStyleDialog(JFrame parent, Shape shape) {
        super(parent, "Custom label Style", true);
        setLayout(new GridLayout(5, 2, 10, 10));
        setSize(300, 250);
        setLocationRelativeTo(parent);

        nameField = new JTextField(shape.getLabelText() != null ? shape.getLabelText() : "");
        shapeCombo = new JComboBox<>(new String[]{"Rect", "Oval"});
        shapeCombo.setSelectedItem(shape.getLabelShape());

        colorField = new JTextField(colorToString(shape.getLabelColor()));
        fontSizeField = new JTextField(String.valueOf(shape.getLabelFontSize()));

        add(new JLabel("Name")); add(nameField);
        add(new JLabel("Shape")); add(shapeCombo);
        add(new JLabel("Color")); add(colorField);
        add(new JLabel("FontSize")); add(fontSizeField);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> { confirmed = false; dispose(); });
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> { confirmed = true; dispose(); });

        add(cancelBtn); add(okBtn);
    }

    public boolean isConfirmed() { return confirmed; }
    public String getLabelName() { return nameField.getText(); }
    public String getLabelShape() { return (String) shapeCombo.getSelectedItem(); }
    public Color getLabelColor() { return parseColor(colorField.getText()); }
    public int getFontSize() {
        try { return Integer.parseInt(fontSizeField.getText()); }
        catch (NumberFormatException e) { return 12; }
    }

    private Color parseColor(String name) {
        switch (name.toLowerCase()) {
            case "yellow": return Color.YELLOW;
            case "red": return Color.RED;
            case "blue": return Color.BLUE;
            case "green": return Color.GREEN;
            case "gray": return Color.GRAY;
            case "white": return Color.WHITE;
            case "black": return Color.BLACK;
            default: return Color.LIGHT_GRAY;
        }
    }

    private String colorToString(Color c) {
        if (c.equals(Color.YELLOW)) return "yellow";
        if (c.equals(Color.RED)) return "red";
        if (c.equals(Color.BLUE)) return "blue";
        if (c.equals(Color.GREEN)) return "green";
        if (c.equals(Color.GRAY)) return "gray";
        if (c.equals(Color.BLACK)) return "black";
        if (c.equals(Color.WHITE)) return "white";
        return "lightgray";
    }
}