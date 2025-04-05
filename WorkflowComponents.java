import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

class PortResult { // 用來包裝搜尋到的 shape & port
    public Shape shape;
    public Point port;
    public PortResult(Shape shape, Point port) {
        this.shape = shape;
        this.port = port;
    }
}

class CanvasPanel extends JPanel { // CanvasPanel
    private final List<Shape> shapes = new ArrayList<>();
    private final List<LinkShape> links = new ArrayList<>();
    private final WorkflowEditor editor;
    private Shape startShape;
    private Point startPort;
    private List<Point> currentPath = new ArrayList<>();
    private boolean isDrawingLink = false;
    private List<Shape> selectedShapes = new ArrayList<>();
    private Point selectionStart, selectionEnd;

    private boolean dragging = false;
    private Point dragStartPoint = null;
    private Shape draggingShape = null;

    public CanvasPanel(WorkflowEditor editor) {
        this.editor = editor;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(500, 350));
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                String mode = editor.getMode();

                if (mode.equals("rect") || mode.equals("oval")) {
                    Shape newShape = mode.equals("rect")
                            ? new RectangleShape(e.getX(), e.getY(), 120, 80)
                            : new OvalShape(e.getX(), e.getY(), 120, 80);
                    assignDepthByOverlapGroup(newShape);
                    shapes.add(newShape);
                    repaint();
                    return;
                }

                if (mode.equals("select")) {
                    Shape shape = getTopMostShapeAt(e.getX(), e.getY());
                    if (shape != null) {
                        Shape topShape = getTopMostComposite(shape);
                        selectedShapes.clear();
                        selectedShapes.add(topShape);
                        editor.updateEditMenuForSelection(selectedShapes);
                        dragging = true;
                        dragStartPoint = e.getPoint();
                        draggingShape = topShape;
                        repaint();
                    } else {
                        selectionStart = e.getPoint();
                        selectionEnd = null;
                    }
                    return;
                }

                if (mode.matches("association|generalization|composition")) {
                    if (isDrawingLink) {
                        PortResult close = getClosestTopPort(e.getX(), e.getY(), 15);

                        if (close != null && close.shape != startShape) {
                            currentPath.set(currentPath.size() - 1, close.port);
                            links.add(new LinkShape(
                                startShape,
                                close.shape,
                                startPort,
                                close.port,
                                editor.getMode(),
                                new ArrayList<>(currentPath)
                            ));
                            startShape = null;
                            startPort = null;
                            currentPath.clear();
                            isDrawingLink = false;
                        } else {
                            Point clickPoint = new Point(e.getX(), e.getY());
                            currentPath.set(currentPath.size() - 1, clickPoint);
                            currentPath.add(new Point(e.getX(), e.getY()));
                        }
                        repaint();
                        return;
                    }

                    Shape shape = getTopMostShapeAt(e.getX(), e.getY());
                    if (shape != null) {
                        Point port = getPortAt(shape, e.getX(), e.getY());
                        if (port != null) {
                            startShape = shape;
                            startPort = port;
                            currentPath.clear();
                            currentPath.add(startPort);
                            currentPath.add(new Point(e.getX(), e.getY()));
                            isDrawingLink = true;
                            repaint();
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                String mode = editor.getMode();

                if (mode.equals("select")) {
                    if (dragging) {
                        dragging = false;
                        bringToFront(draggingShape);
                        draggingShape = null;
                        dragStartPoint = null;
                        repaint();
                    } else if (selectionStart != null) {
                        selectionEnd = e.getPoint();
                        Rectangle selectionRect = new Rectangle(
                                Math.min(selectionStart.x, selectionEnd.x),
                                Math.min(selectionStart.y, selectionEnd.y),
                                Math.abs(selectionStart.x - selectionEnd.x),
                                Math.abs(selectionStart.y - selectionEnd.y));

                        List<Shape> newSelection = new ArrayList<>();
                        for (Shape s : shapes) {
                            Rectangle bounds = new Rectangle(s.x, s.y, s.width, s.height);
                            if (selectionRect.contains(bounds) && isTopMostInGroup(s)) {
                                newSelection.add(s);
                            }
                        }
                        if (!newSelection.isEmpty()) {
                            selectedShapes = newSelection;
                            editor.updateEditMenuForSelection(selectedShapes);
                        }
                        selectionStart = selectionEnd = null;
                        repaint();
                    }
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                String mode = editor.getMode();

                if (mode.equals("select")) {
                    if (dragging && dragStartPoint != null && draggingShape != null) {
                        int dx = e.getX() - dragStartPoint.x;
                        int dy = e.getY() - dragStartPoint.y;

                        moveShapeWithChildren(draggingShape, dx, dy);

                        dragStartPoint = e.getPoint();
                        updateConnectedLinks();
                        repaint();
                    } else if (selectionStart != null) {
                        selectionEnd = e.getPoint();
                        repaint();
                    }
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (isDrawingLink && currentPath.size() >= 2) {
                    currentPath.set(currentPath.size() - 1, new Point(e.getX(), e.getY()));
                    repaint();
                }
            }
        });
    }

    private void assignDepthByOverlapGroup(Shape newShape) {
        Rectangle newRect = new Rectangle(newShape.x, newShape.y, newShape.width, newShape.height);
        int maxDepthInOverlap = -1;
        for (Shape s : shapes) {
            Rectangle r = new Rectangle(s.x, s.y, s.width, s.height);
            if (r.intersects(newRect)) {
                maxDepthInOverlap = Math.max(maxDepthInOverlap, s.getDepth());
            }
        }
        newShape.setDepth(maxDepthInOverlap + 1);
    }

    private boolean isTopMostInGroup(Shape target) {
        Rectangle rect = new Rectangle(target.x, target.y, target.width, target.height);
        for (Shape other : shapes) {
            if (other == target) continue;
            Rectangle orect = new Rectangle(other.x, other.y, other.width, other.height);
            if (orect.intersects(rect) && other.getDepth() > target.getDepth()) {
                return false;
            }
        }
        return true;
    }
    
    private void bringToFront(Shape shape) {
        int maxDepth = shapes.stream().mapToInt(Shape::getDepth).max().orElse(0);
        shape.setDepth(maxDepth + 1);
    }

    private void updateConnectedLinks() { // 修改 updateConnectedLinks 方法，確保 group 後連線端點仍然正確
        for (LinkShape link : links) {
            if (link.fromShape != null) {
                Point closestPort = null;
                
                // 如果是 CompositeShape，尋找最近的子物件連接點
                if (link.fromShape instanceof CompositeShape) {
                    closestPort = findClosestChildPort((CompositeShape) link.fromShape, link.start);
                } else {
                    closestPort = findClosestPort(link.fromShape, link.start);
                }
                
                if (closestPort != null) {
                    link.start = closestPort;
                }
            }
            
            if (link.toShape != null) {
                Point closestPort = null;
                
                // 如果是 CompositeShape，尋找最近的子物件連接點
                if (link.toShape instanceof CompositeShape) {
                    closestPort = findClosestChildPort((CompositeShape) link.toShape, link.end);
                } else {
                    closestPort = findClosestPort(link.toShape, link.end);
                }
                
                if (closestPort != null) {
                    link.end = closestPort;
                }
            }
            
            // 更新路徑
            List<Point> newPath = new ArrayList<>();
            if (link.start != null) newPath.add(link.start);
            if (link.end != null) newPath.add(link.end);
            link.setPath(newPath);
        }
    }

    private Point findClosestChildPort(CompositeShape composite, Point oldPort) { // 尋找 CompositeShape 中最近的子物件連接點
        Point closest = null;
        double bestDist = Double.MAX_VALUE;
        
        for (Shape child : composite.getChildren()) { // 檢查所有子物件
            if (child instanceof CompositeShape) {
                // 遞迴檢查子 CompositeShape
                Point childPort = findClosestChildPort((CompositeShape) child, oldPort);
                if (childPort != null) {
                    double dist = childPort.distance(oldPort);
                    if (dist < bestDist) {
                        bestDist = dist;
                        closest = childPort;
                    }
                }
            } else {
                // 檢查普通 Shape 的連接點
                for (Point p : child.getConnectionPorts()) {
                    double dist = p.distance(oldPort);
                    if (dist < bestDist) {
                        bestDist = dist;
                        closest = p;
                    }
                }
            }
        }
        
        // 如果沒有找到子物件的連接點，使用 CompositeShape 自己的連接點
        if (closest == null) {
            closest = findClosestPort(composite, oldPort);
        }
        
        return closest;
    }

    private Point findClosestPort(Shape shape, Point oldPort) {
        Point closest = null;
        double bestDist = Double.MAX_VALUE;
        for (Point p : shape.getConnectionPorts()) {
            double d = p.distance(oldPort);
            if (d < bestDist) {
                bestDist = d;
                closest = p;
            }
        }
        return closest;
    }

    // 找「離 (mouseX, mouseY) 最近的 port」
    private PortResult getClosestTopPort(int mouseX, int mouseY, int threshold) {
        Shape topShape = getTopMostShapeAt(mouseX, mouseY);
        if (topShape == null) return null;

        Point best = null;
        double bestDist = Double.MAX_VALUE;
        for (Point p : topShape.getConnectionPorts()) {
            double dist = p.distance(mouseX, mouseY);
            if (dist <= threshold && dist < bestDist) {
                bestDist = dist;
                best = p;
            }
        }
        return best != null ? new PortResult(topShape, best) : null;
    }

    private Shape getTopMostComposite(Shape target) {
        Shape current = target;
        boolean found;

        do {
            found = false;
            for (Shape s : shapes) {
                if (s instanceof CompositeShape) {
                    CompositeShape group = (CompositeShape) s;
                    if (containsRecursively(group, current)) {
                        current = group;
                        found = true;
                        break;
                    }
                }
            }
        } while (found);

        return current;
    }

    private void moveShapeWithChildren(Shape shape, int dx, int dy) {
        if (shape instanceof CompositeShape) {
            CompositeShape group = (CompositeShape) shape;
            for (Shape child : group.getChildren()) {
                moveShapeWithChildren(child, dx, dy);
            }
            group.updateBounds();
        } else {
            shape.x += dx;
            shape.y += dy;
        }
    }

    private boolean containsRecursively(CompositeShape group, Shape target) {
        if (group.getChildren().contains(target)) return true;
        for (Shape child : group.getChildren()) {
            if (child instanceof CompositeShape) {
                if (containsRecursively((CompositeShape) child, target)) return true;
            }
        }
        return false;
    }

    private Shape getTopMostShapeAt(int x, int y) {
        return shapes.stream()
                .filter(s -> s.contains(x, y) && isTopMostInGroup(s))
                .sorted((a, b) -> Integer.compare(b.getDepth(), a.getDepth()))
                .findFirst().orElse(null);
    }

    // 找 shape 中最接近 (x, y) 的 port
    private Point getPortAt(Shape shape, int x, int y) {
        for (Point p : shape.getConnectionPorts()) {
            if (new Rectangle(p.x - 5, p.y - 5, 10, 10).contains(x, y)) {
                return p;
            }
        }
        return null;
    }

    // 修改 paintComponent 方法，確保所有連線的端點都會被顯示
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 先收集所有連線端點
        List<Point> allLinkedPorts = new ArrayList<>();
        for (LinkShape link : links) {
            if (link.start != null) allLinkedPorts.add(link.start);
            if (link.end != null) allLinkedPorts.add(link.end);
        }

        // 繪製所有 shape
        shapes.stream()
            .sorted((a, b) -> Integer.compare(a.getDepth(), b.getDepth()))
            .forEach(shape -> {
                // 取得該 shape 所有應該顯示的連接點
                boolean isSelected = selectedShapes.contains(shape);
                shape.draw(g2d, isSelected, allLinkedPorts);
            });

        links.forEach(link -> link.draw(g2d)); // 繪製所有連線

        // 繪製選擇框
        if (selectionStart != null && selectionEnd != null) {
            g2d.setColor(Color.LIGHT_GRAY);
            int x = Math.min(selectionStart.x, selectionEnd.x);
            int y = Math.min(selectionStart.y, selectionEnd.y);
            int w = Math.abs(selectionStart.x - selectionEnd.x);
            int h = Math.abs(selectionStart.y - selectionEnd.y);
            g2d.drawRect(x, y, w, h);
        }
        
        // 繪製正在建立的連線
        if (isDrawingLink && currentPath.size() > 1) {
            Stroke originalStroke = g2d.getStroke();
            g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.setColor(Color.BLACK);

            for (int i = 0; i < currentPath.size() - 1; i++) {
                Point p1 = currentPath.get(i);
                Point p2 = currentPath.get(i + 1);
                g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            }

            if (currentPath.size() >= 2) {
                String type = editor.getMode();
                Point from = currentPath.get(currentPath.size() - 2);
                Point to = currentPath.get(currentPath.size() - 1);
                LinkShape tempLink = new LinkShape(null, null, null, null, type, null);
                tempLink.drawArrow(g2d, from.x, from.y, to.x, to.y, type);
            }
            g2d.setStroke(originalStroke);
        }
    }

    public void groupSelectedShapes() {
        if (selectedShapes.size() > 1) {
            CompositeShape group = new ConcreteCompositeShape(selectedShapes);
            shapes.removeAll(selectedShapes);
            shapes.add(group);
            selectedShapes.clear();
            selectedShapes.add(group);
            editor.updateEditMenuForSelection(selectedShapes);
            repaint();
        }
    }

    public void ungroupSelectedShape() {
        if (selectedShapes.size() == 1 && selectedShapes.get(0) instanceof CompositeShape) {
            CompositeShape group = (CompositeShape) selectedShapes.get(0);
            shapes.remove(group);
            shapes.addAll(group.getChildren());
            selectedShapes.clear();
            selectedShapes.addAll(group.getChildren());
            editor.updateEditMenuForSelection(selectedShapes);
            repaint();
        }
    }

    public void showLabelDialogForSelectedShape() {
        if (selectedShapes.size() != 1) return;
        Shape shape = selectedShapes.get(0);
        if (shape instanceof CompositeShape) return;
    
        LabelStyleDialog dialog = new LabelStyleDialog((JFrame) SwingUtilities.getWindowAncestor(this), shape);
        dialog.setVisible(true);
    
        if (dialog.isConfirmed()) {
            shape.setLabelText(dialog.getLabelName());
            shape.setLabelShape(dialog.getLabelShape());
            shape.setLabelColor(dialog.getLabelColor());
            shape.setLabelFontSize(dialog.getFontSize());
            repaint();
        }
    }
}

class LinkShape {
    public Shape fromShape, toShape;
    public Point start, end;
    private String type;
    private List<Point> path;

    public LinkShape(Shape fromShape, Shape toShape, Point start, Point end, String type, List<Point> path) {
        this.fromShape = fromShape;
        this.toShape = toShape;
        this.start = start;
        this.end = end;
        this.type = type;
        this.path = path; // store the entire path
    }
    
    public List<Point> getPath() {
        return path;
    }

    public void setPath(List<Point> newPath) {
        this.path = newPath;
    }

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g; // 使用 Graphics2D 以獲得更平滑的線條
        
        Stroke originalStroke = g2d.getStroke(); // 保存原來的線條樣式
        
        // 使用更平滑的線條樣式
        g2d.setStroke(new BasicStroke(
            2.0f,                    // 線寬增加到2.0f
            BasicStroke.CAP_ROUND,   // 圓形線帽
            BasicStroke.JOIN_ROUND   // 圓形連接點
        ));
        
        g2d.setColor(Color.BLACK);
        
        // draw the entire path as segments
        for (int i = 0; i < path.size() - 1; i++) {
            Point p1 = path.get(i);
            Point p2 = path.get(i + 1);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // arrow/triangle/diamond on last segment
        if (path.size() > 1) {
            Point from = path.get(path.size() - 2);
            Point to = path.get(path.size() - 1);
            switch (type) {
                case "association":
                    drawArrow(g2d, from.x, from.y, to.x, to.y, type);
                    break;
                case "generalization":
                    drawArrow(g2d, from.x, from.y, to.x, to.y, type);
                    break;
                case "composition":
                    drawArrow(g2d, from.x, from.y, to.x, to.y, type);
                    break;
            }
        }
        
        // 還原原來的線條樣式
        g2d.setStroke(originalStroke);
    }

    public void drawArrow(Graphics g, int x1, int y1, int x2, int y2, String type) {
        int arrowSize = 14; // 箭頭尺寸
        double angle = Math.atan2(y2 - y1, x2 - x1);
        Graphics2D g2d = (Graphics2D) g;

        if (type.equals("generalization")) { // 空心三角形箭頭
            // 調整三角形的形狀參數
            double baseWidth = 0.5; 
            double height = 1.3;   
            
            // 計算三角形三個點
            int[] xPoints = {
                x2, // 頂點
                x2 - (int)(arrowSize * height * Math.cos(angle) - arrowSize * baseWidth * Math.sin(angle)),
                x2 - (int)(arrowSize * height * Math.cos(angle) + arrowSize * baseWidth * Math.sin(angle))
            };
            int[] yPoints = {
                y2, // 頂點
                y2 - (int)(arrowSize * height * Math.sin(angle) + arrowSize * baseWidth * Math.cos(angle)),
                y2 - (int)(arrowSize * height * Math.sin(angle) - arrowSize * baseWidth * Math.cos(angle))
            };
            
            // 使用填充白色三角形 + 黑色邊框實現空心效果
            Color origColor = g.getColor();
            g.setColor(Color.WHITE);
            g.fillPolygon(xPoints, yPoints, 3);
            g.setColor(origColor);
            g.drawPolygon(xPoints, yPoints, 3);
        } else if (type.equals("composition")) {
            // 實現30度旋轉的正菱形箭頭（正方形旋轉30度）
            
            // 正菱形的邊長
            int diamondSize = arrowSize;
            
            // 計算菱形的四個角
            int[] xPoints = {
                x2,                                                      // 前端點
                x2 - (int)(diamondSize * Math.cos(angle - Math.PI/6)),  // 右側點
                x2 - (int)(diamondSize * Math.sqrt(2) * Math.cos(angle)), // 後端點
                x2 - (int)(diamondSize * Math.cos(angle + Math.PI/6))   // 左側點
            };
            int[] yPoints = {
                y2,                                                      // 前端點
                y2 - (int)(diamondSize * Math.sin(angle - Math.PI/6)),  // 右側點
                y2 - (int)(diamondSize * Math.sqrt(2) * Math.sin(angle)), // 後端點
                y2 - (int)(diamondSize * Math.sin(angle + Math.PI/6))   // 左側點
            };
            
            // 使用填充白色菱形 + 黑色邊框
            g.setColor(Color.WHITE);
            g.fillPolygon(xPoints, yPoints, 4);
            g.setColor(Color.BLACK);
            g.drawPolygon(xPoints, yPoints, 4);
        } else if (type.equals("association")) {
            // 關聯箭頭
            Stroke originalStroke = g2d.getStroke();
            g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // 使用更粗的圖角線
            
            g.drawLine(x2, y2, 
                      x2 - (int) (arrowSize * Math.cos(angle - Math.PI / 6)), 
                      y2 - (int) (arrowSize * Math.sin(angle - Math.PI / 6)));
            g.drawLine(x2, y2, 
                      x2 - (int) (arrowSize * Math.cos(angle + Math.PI / 6)), 
                      y2 - (int) (arrowSize * Math.sin(angle + Math.PI / 6)));
                      
            g2d.setStroke(originalStroke); // 恢復原始線寬
        }
    }
    
    public static void drawArrow(Graphics g, Point from, Point to) {
        LinkShape linkShape = new LinkShape(null, null, null, null, "association", null);
        linkShape.drawArrow(g, from.x, from.y, to.x, to.y, "association");
    }
    
    public static void drawTriangle(Graphics g, Point from, Point to) {
        LinkShape linkShape = new LinkShape(null, null, null, null, "generalization", null);
        linkShape.drawArrow(g, from.x, from.y, to.x, to.y, "generalization");
    }
    
    public static void drawDiamond(Graphics g, Point from, Point to) {
        LinkShape linkShape = new LinkShape(null, null, null, null, "composition", null);
        linkShape.drawArrow(g, from.x, from.y, to.x, to.y, "composition");
    }
}

abstract class Shape {
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

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public abstract void draw(Graphics g, boolean showPorts, List<Point> alwaysShowPorts);

    public boolean contains(int px, int py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    public List<Point> getConnectionPorts() {
        return new ArrayList<>();
    }

    public void setLabelText(String text) { this.labelText = text; }
    public void setLabelShape(String shape) { this.labelShape = shape; }
    public void setLabelColor(Color color) { this.labelColor = color; }
    public void setLabelFontSize(int size) { this.labelFontSize = size; }

    public String getLabelText() { return labelText; }
    public String getLabelShape() { return labelShape; }
    public Color getLabelColor() { return labelColor; }
    public int getLabelFontSize() { return labelFontSize; }
    public boolean hasLabel() { return labelText != null && !labelText.isEmpty(); }
}

class RectangleShape extends Shape {
    public RectangleShape(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void draw(Graphics g, boolean showPorts, List<Point> alwaysShowPorts) {
        g.setColor(new Color(198, 198, 198));
        g.fillRect(x, y, width, height);
        g.setColor(Color.BLACK);
        
        // 繪製所有可見的連接點
        for (Point p : getConnectionPorts()) {
            // 如果這個點是連線的端點，或者物件被選中，則顯示
            if (showPorts || alwaysShowPorts.contains(p)) {
                g.fillRect(p.x - 5, p.y - 5, 10, 10);
            }
        }

        if (hasLabel()) {
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
            } else {
                g2d.fillRect(labelX, labelY, labelW, labelH);
            }
        
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(labelText);
            int textHeight = fm.getHeight();
            g2d.drawString(labelText, cx - textWidth / 2, cy + textHeight / 4);
        }
    }
    
    @Override
    public List<Point> getConnectionPorts() {
        List<Point> ports = new ArrayList<>();
        ports.add(new Point(x, y));
        ports.add(new Point(x + width / 2, y));
        ports.add(new Point(x + width, y));
        ports.add(new Point(x, y + height / 2));
        ports.add(new Point(x + width, y + height / 2));
        ports.add(new Point(x, y + height));
        ports.add(new Point(x + width / 2, y + height));
        ports.add(new Point(x + width, y + height));
        return ports;
    }
}

class OvalShape extends Shape {
    public OvalShape(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void draw(Graphics g, boolean showPorts, List<Point> alwaysShowPorts) {
        g.setColor(new Color(198, 198, 198));
        g.fillOval(x, y, width, height);
        g.setColor(Color.BLACK);
        
        // 繪製所有可見的連接點
        for (Point p : getConnectionPorts()) {
            // 如果這個點是連線的端點，或者物件被選中，則顯示
            if (showPorts || alwaysShowPorts.contains(p)) {
                g.fillRect(p.x - 5, p.y - 5, 10, 10);
            }
        }

        if (hasLabel()) {
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
            } else {
                g2d.fillRect(labelX, labelY, labelW, labelH);
            }
        
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(labelText);
            int textHeight = fm.getHeight();
            g2d.drawString(labelText, cx - textWidth / 2, cy + textHeight / 4);
        }
    }

    @Override
    public List<Point> getConnectionPorts() {
        List<Point> ports = new ArrayList<>();
        ports.add(new Point(x + width / 2, y));
        ports.add(new Point(x, y + height / 2));
        ports.add(new Point(x + width, y + height / 2));
        ports.add(new Point(x + width / 2, y + height));
        return ports;
    }
}

abstract class CompositeShape extends Shape {
    private List<Shape> children;

    public CompositeShape(List<Shape> children) {
        super(0, 0, 0, 0);
        this.children = new ArrayList<>(children);
        updateBounds();
    }

    protected void updateBounds() {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = 0, maxY = 0;
        for (Shape child : children) {
            minX = Math.min(minX, child.x);
            minY = Math.min(minY, child.y);
            maxX = Math.max(maxX, child.x + child.width);
            maxY = Math.max(maxY, child.y + child.height);
        }
        this.x = minX;
        this.y = minY;
        this.width = maxX - minX;
        this.height = maxY - minY;
    }

    @Override
    public void draw(Graphics g, boolean showPorts, List<Point> alwaysShowPorts) {
        // 繪製所有子物件
        for (Shape child : getChildren()) {
            // 選中組合物件時，子物件也應顯示連接點
            child.draw(g, showPorts, alwaysShowPorts);
        }
        
        // 繪製組合物件自己的連接點
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
    public boolean contains(int px, int py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    @Override
    public List<Point> getConnectionPorts() {
        List<Point> ports = new ArrayList<>();
        ports.add(new Point(x + width / 2, y));
        ports.add(new Point(x + width, y + height / 2));
        ports.add(new Point(x + width / 2, y + height));
        ports.add(new Point(x, y + height / 2));
        return ports;
    }

    public List<Shape> getChildren() {
        return children;
    }
}

class ConcreteCompositeShape extends CompositeShape {
    public ConcreteCompositeShape(List<Shape> children) {
        super(children);
    }
}