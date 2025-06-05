package shape;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

// oval shape類別
public class OvalShape extends Shape {

    public OvalShape(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void draw(Graphics g, boolean showPorts, List<Point> alwaysShowPorts) {
        // 繪製oval
        g.setColor(new Color(198, 198, 198));
        g.fillOval(x, y, width, height);
        g.setColor(Color.BLACK);
        g.drawOval(x, y, width, height);

        // 繪製所有可見的連接點
        for (Point p : getConnectionPorts()) {
            // 如果這個點是連線的port，或者物件被選中，則顯示
            if (showPorts || alwaysShowPorts.contains(p)) {
                g.fillRect(p.x - 5, p.y - 5, 10, 10);
            }
        }

        // 繪製label
        if (hasLabel()) {
            drawLabel(g);
        }
    }

    // 繪製label
    private void drawLabel(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int labelW = 60;
        int labelH = 30;
        int cx = x + width / 2;
        int cy = y + height / 2;
        int labelX = cx - labelW / 2;
        int labelY = cy - labelH / 2;

        g2d.setColor(labelColor);
        if ("oval".equalsIgnoreCase(labelShape)) {
            g2d.fillOval(labelX, labelY, labelW, labelH);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(labelX, labelY, labelW, labelH);
        } else {
            g2d.fillRect(labelX, labelY, labelW, labelH);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(labelX, labelY, labelW, labelH);
        }

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(labelText);
        int textHeight = fm.getHeight();
        g2d.drawString(labelText, cx - textWidth / 2, cy + textHeight / 4);
    }

    @Override
    public List<Point> getConnectionPorts() {
        List<Point> ports = new ArrayList<>();
        ports.add(new Point(x + width / 2, y));          // 上中
        ports.add(new Point(x, y + height / 2));         // 左中
        ports.add(new Point(x + width, y + height / 2)); // 右中
        ports.add(new Point(x + width / 2, y + height)); // 下中
        return ports;
    }
}