package shape;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

// Shape抽象類別，所有圖形的基底class
public abstract class Shape implements Cloneable {
    protected int x, y, width, height;
    
    protected int depth = 0; // 0~99 越大越上層

    protected String labelText = null;
    protected String labelShape = "rect";
    protected Color labelColor = Color.WHITE;
    protected int labelFontSize = 12;

    public Shape(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    // 讓Shape被處理器處理的方法，用於多型
    // @param handler Shape處理器
    public void accept(ShapeHandler handler) {
        handler.handleBasicShape(this);
    }

    // 繪製形狀
    // @param g 繪圖環境
    // @param showPorts 是否顯示port
    // @param alwaysShowPorts 總是顯示的port清單
    public abstract void draw(Graphics g, boolean showPorts, List<Point> alwaysShowPorts);

    public boolean contains(int px, int py) { // 檢查點是否在Shape內
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    public List<Point> getConnectionPorts() { // 取得形狀的port清單
        return new ArrayList<>();
    }

    // label相關方法
    public void setLabelText(String text) { this.labelText = text; }
    public void setLabelShape(String shape) { this.labelShape = shape; }
    public void setLabelColor(Color color) { this.labelColor = color; }
    public void setLabelFontSize(int size) { this.labelFontSize = size; }

    public String getLabelText() { return labelText; }
    public String getLabelShape() { return labelShape; }
    public Color getLabelColor() { return labelColor; }
    public int getLabelFontSize() { return labelFontSize; }
    public boolean hasLabel() { return labelText != null && !labelText.isEmpty(); }

    // 實現 Cloneable 接口，支持Shape的copy
    @Override
    public Shape clone() {
        try {
            return (Shape) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone not supported", e);
        }
    }
}