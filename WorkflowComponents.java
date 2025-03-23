import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

// 用來包裝搜尋到的 shape & port
class PortResult {
    public Shape shape;
    public Point port;
    public PortResult(Shape shape, Point port) {
        this.shape = shape;
        this.port = port;
    }
}

// CanvasPanel
class CanvasPanel extends JPanel {
    private final List<Shape> shapes = new ArrayList<>();
    private final List<LinkShape> links = new ArrayList<>();
    private final WorkflowEditor editor;
    private Shape startShape;
    private Point startPort;
    private List<Point> currentPath = new ArrayList<>();
    private boolean isDrawingLink = false; // 標記是否正在繪製連線
    private List<Shape> selectedShapes = new ArrayList<>();
    private Point selectionStart, selectionEnd;

    public CanvasPanel(WorkflowEditor editor) {
        this.editor = editor;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(500, 350));
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                String mode = editor.getMode();

                // 1. 建立 basic rect/oval
                if (mode.equals("rect") || mode.equals("oval")) {
                    shapes.add(mode.equals("rect")
                            ? new RectangleShape(e.getX(), e.getY(), 120, 80)
                            : new OvalShape(e.getX(), e.getY(), 120, 80));
                    repaint();
                    return;
                }

                // 2. select 模式
                if (mode.equals("select")) {
                    Shape shape = getShapeAt(e.getX(), e.getY());
                    if (shape != null) {
                        selectedShapes.clear();
                        selectedShapes.add(shape);
                        repaint();
                    } else {
                        selectionStart = e.getPoint();
                        selectionEnd = null;
                    }
                    return;
                }

                // 3. association / generalization / composition 模式
                if (mode.matches("association|generalization|composition")) {
                    // 如果已經在繪製連線
                    if (isDrawingLink) {
                        // 找離滑鼠最近的port
                        PortResult close = getClosestPort(e.getX(), e.getY(), 15);
                        
                        if (close != null && close.shape != startShape) {
                            // 找到目標port，完成連線
                            
                            // 替換最後一個點為目標控制點，保持方向不變
                            currentPath.set(currentPath.size() - 1, close.port);
                            
                            // 新增LinkShape
                            links.add(new LinkShape(
                                startShape,
                                close.shape,
                                startPort,
                                close.port,
                                editor.getMode(),
                                new ArrayList<>(currentPath)
                            ));
                            
                            // 重置狀態
                            startShape = null;
                            startPort = null;
                            currentPath.clear();
                            isDrawingLink = false;
                        } else {
                            // 在空白處點擊，新增一個轉折點
                            // 記錄當前滑鼠點擊位置為固定轉折點
                            Point clickPoint = new Point(e.getX(), e.getY());
                            
                            // 將當前臨時點（跟隨滑鼠移動的點）更新為固定轉折點
                            currentPath.set(currentPath.size() - 1, clickPoint);
                            
                            // 添加新的臨時點（會跟隨滑鼠移動）
                            currentPath.add(new Point(e.getX(), e.getY()));
                        }
                        repaint();
                        return;
                    }
                    
                    // 開始新的連線
                    Shape shape = getShapeAt(e.getX(), e.getY());
                    if (shape != null) {
                        // 檢查是否有 port
                        Point port = getPortAt(shape, e.getX(), e.getY());
                        if (port != null) {
                            startShape = shape;
                            startPort = port;
                            currentPath.clear();
                            currentPath.add(startPort);
                            
                            // 添加一個初始的臨時點，這個點會跟隨滑鼠移動
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

                // a. select模式結束框選
                if (mode.equals("select") && selectionStart != null) {
                    selectionEnd = e.getPoint();
                    Rectangle selectionRect = new Rectangle(
                            Math.min(selectionStart.x, selectionEnd.x),
                            Math.min(selectionStart.y, selectionEnd.y),
                            Math.abs(selectionStart.x - selectionEnd.x),
                            Math.abs(selectionStart.y - selectionEnd.y));

                    List<Shape> newSelection = new ArrayList<>();
                    for (Shape s : shapes) {
                        Rectangle bounds = new Rectangle(s.x, s.y, s.width, s.height);
                        if (selectionRect.contains(bounds)) {
                            newSelection.add(s);
                        }
                    }
                    if (!newSelection.isEmpty()) {
                        selectedShapes = newSelection;
                    }
                    selectionStart = selectionEnd = null;
                    repaint();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                String mode = editor.getMode();

                // a. select模式
                if (mode.equals("select") && selectionStart != null) {
                    selectionEnd = e.getPoint();
                    repaint();
                    return;
                }
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                // 如果正在繪製連線，更新最後一個點的位置為當前滑鼠位置
                if (isDrawingLink && currentPath.size() >= 2) {
                    // 更新最後一個點的位置（臨時點）
                    currentPath.set(currentPath.size() - 1, new Point(e.getX(), e.getY()));
                    repaint();
                }
            }
        });
    }

    // 找「離 (mouseX, mouseY) 最近的 port」
    private PortResult getClosestPort(int mouseX, int mouseY, int threshold) {
        PortResult best = null;
        double bestDist = Double.MAX_VALUE;

        for (Shape shape : shapes) {
            for (Point p : shape.getConnectionPorts()) {
                double dist = p.distance(mouseX, mouseY);
                if (dist <= threshold && dist < bestDist) {
                    bestDist = dist;
                    best = new PortResult(shape, p);
                }
            }
        }
        return best;
    }

    // 找到最上層含點 (x, y) 的 shape
    private Shape getShapeAt(int x, int y) {
        for (int i = shapes.size() - 1; i >= 0; i--) {
            if (shapes.get(i).contains(x, y)) return shapes.get(i);
        }
        return null;
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

// 在CanvasPanel類中的paintComponent方法也需要相應修改

@Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    
    // 創建Graphics2D以使用高級功能
    Graphics2D g2d = (Graphics2D) g;
    
    // 開啟抗鋸齒功能，使線條更平滑
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // 1. 先畫所有 shape
    for (Shape shape : shapes) {
        List<Point> linkedPorts = getLinkedPorts(shape);
        boolean show = selectedShapes.contains(shape);
        shape.draw(g2d, show, linkedPorts);
    }

    // 2. 再畫連線
    for (LinkShape link : links) {
        link.draw(g2d);
    }

    // 3. 畫 in-progress path
    if (isDrawingLink && currentPath.size() > 1) {
        Stroke originalStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(
            2.0f,                    // 線寬增加到2.0f
            BasicStroke.CAP_ROUND,   // 圓形線帽
            BasicStroke.JOIN_ROUND   // 圓形連接點
        ));
        
        g2d.setColor(Color.BLACK);
        
        for (int i = 0; i < currentPath.size() - 1; i++) {
            Point p1 = currentPath.get(i);
            Point p2 = currentPath.get(i + 1);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // 畫箭頭 (只在最後一段繪製)
        if (currentPath.size() >= 2) {
            String type = editor.getMode();
            Point from = currentPath.get(currentPath.size() - 2);
            Point to = currentPath.get(currentPath.size() - 1);
            
            LinkShape tempLink = new LinkShape(null, null, null, null, type, null);
            tempLink.drawArrow(g2d, from.x, from.y, to.x, to.y, type);
        }
        
        g2d.setStroke(originalStroke);
    }

    // 4. 如果正在框選，就畫框選範圍
    if (selectionStart != null && selectionEnd != null) {
        g2d.setColor(Color.LIGHT_GRAY);
        int x = Math.min(selectionStart.x, selectionEnd.x);
        int y = Math.min(selectionStart.y, selectionEnd.y);
        int w = Math.abs(selectionStart.x - selectionEnd.x);
        int h = Math.abs(selectionStart.y - selectionEnd.y);
        g2d.drawRect(x, y, w, h);
    }
}

    private List<Point> getLinkedPorts(Shape shape) {
        List<Point> ports = new ArrayList<>();
        for (LinkShape link : links) {
            if (link.fromShape == shape && link.start != null) ports.add(link.start);
            if (link.toShape == shape && link.end != null) ports.add(link.end);
        }
        return ports;
    }

    // group / ungroup 與 shape 相關程式不動
    public void groupSelectedShapes() {
        if (selectedShapes.size() > 1) {
            CompositeShape group = new ConcreteCompositeShape(selectedShapes);
            shapes.removeAll(selectedShapes);
            shapes.add(group);
            selectedShapes.clear();
            selectedShapes.add(group);
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
        // store the entire path
        this.path = path;
    }

    public void draw(Graphics g) {
        // 使用 Graphics2D 以獲得更平滑的線條
        Graphics2D g2d = (Graphics2D) g;
        
        // 保存原來的線條樣式
        Stroke originalStroke = g2d.getStroke();
        
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
        // 箭頭更大更明顯
        int arrowSize = 14; // 增大箭頭尺寸
        double angle = Math.atan2(y2 - y1, x2 - x1);
        Graphics2D g2d = (Graphics2D) g;

        if (type.equals("generalization")) {
            // 空心三角形箭頭
        
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
            // 實現45度旋轉的正菱形箭頭（正方形旋轉45度）
            
            // 正菱形的邊長（正方形旋轉45度）
            int diamondSize = arrowSize;
            
            // 計算菱形的四個角（正方形旋轉45度）
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

    public Shape(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void draw(Graphics g, boolean showPorts, List<Point> alwaysShowPorts);

    public boolean contains(int px, int py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    public List<Point> getConnectionPorts() {
        return new ArrayList<>();
    }
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
        for (Point p : getConnectionPorts()) {
            if (showPorts || alwaysShowPorts.contains(p)) {
                g.fillRect(p.x - 5, p.y - 5, 10, 10);
            }
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
        for (Point p : getConnectionPorts()) {
            if (showPorts || alwaysShowPorts.contains(p)) {
                g.fillRect(p.x - 5, p.y - 5, 10, 10);
            }
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
        for (Shape child : children) {
            child.draw(g, showPorts, alwaysShowPorts);
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