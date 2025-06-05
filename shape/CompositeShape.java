package shape;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

// Composite抽象類別，用Composition pattern
public abstract class CompositeShape extends Shape {
    private List<Shape> children;

    public CompositeShape(List<Shape> children) {
        super(0, 0, 0, 0);
        this.children = new ArrayList<>(children);
        updateBounds();
    }

    public void updateBounds() { // 更新composite shape的邊界
        if (children.isEmpty()) {
            return;
        }

        // 初始化為第一個子物件的邊界
        Shape firstChild = children.get(0);
        int minX = firstChild.getX();
        int minY = firstChild.getY();
        int maxX = firstChild.getX() + firstChild.getWidth();
        int maxY = firstChild.getY() + firstChild.getHeight();

        // 迴圈找所有子物件，找出實際的邊界
        for (Shape child : children) {
            minX = Math.min(minX, child.getX());
            minY = Math.min(minY, child.getY());
            maxX = Math.max(maxX, child.getX() + child.getWidth());
            maxY = Math.max(maxY, child.getY() + child.getHeight());
        }

        // 設置緊貼的邊界，沒有額外的padding
        this.x = minX;
        this.y = minY;
        this.width = maxX - minX;
        this.height = maxY - minY;
    }

    @Override
    public void draw(Graphics g, boolean showPorts, List<Point> alwaysShowPorts) {
        // 繪製所有子物件
        for (Shape child : getChildren()) {
            // 選中group物件時，子物件也顯示port
            child.draw(g, showPorts, alwaysShowPorts);
        }

        // 繪製group物件自己的port
        if (showPorts) {
            g.setColor(Color.BLACK);
            for (Point p : getConnectionPorts()) {
                if (alwaysShowPorts.contains(p)) {
                    g.fillRect(p.x - 5, p.y - 5, 10, 10);
                }
            }
        }
    }

    @Override
    public List<Point> getConnectionPorts() {
        List<Point> ports = new ArrayList<>();
        ports.add(new Point(x + width / 2, y));          // 上中
        ports.add(new Point(x + width, y + height / 2)); // 右中
        ports.add(new Point(x + width / 2, y + height)); // 下中
        ports.add(new Point(x, y + height / 2));         // 左中
        return ports;
    }

    public List<Shape> getChildren() { // 取得所有子物件
        return new ArrayList<>(children);
    }

    public void addChild(Shape child) { // 新增子物件
        children.add(child);
        updateBounds();
    }

    public void removeChild(Shape child) { // 移除子物件
        children.remove(child);
        updateBounds();
    }

    // 檢查是否包含特定物件（遞迴搜尋）
    public boolean containsRecursively(Shape target) {
        if (children.contains(target)) {
            return true;
        }

        for (Shape child : children) {
            // 直接使用多型檢查子形狀
            ContainmentChecker checker = new ContainmentChecker(target);
            child.accept(checker);
            if (checker.containsTarget()) {
                return true;
            }
        }

        return false;
    }

    // 包含檢查器供內部使用
    private static class ContainmentChecker implements ShapeHandler {
        private final Shape target;
        private boolean contains = false;
        
        public ContainmentChecker(Shape target) {
            this.target = target;
        }
        
        @Override
        public void handleBasicShape(Shape shape) {
            // basic shape,不包含其他形狀
            contains = false;
        }
        
        @Override
        public void handleCompositeShape(CompositeShape composite) {
            // 遞迴檢查
            contains = composite.children.contains(target);
            if (!contains) {
                // 檢查子shape的子shape
                for (Shape child : composite.children) {
                    ContainmentChecker childChecker = new ContainmentChecker(target);
                    child.accept(childChecker);
                    if (childChecker.containsTarget()) {
                        contains = true;
                        break;
                    }
                }
            }
        }
        
        public boolean containsTarget() {
            return contains;
        }
    }

    @Override
    public void accept(ShapeHandler handler) { // override Shape的 accept 方法，以多型方式處理Composite shape
        handler.handleCompositeShape(this);
    }
}
