package link;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;

import shape.Shape;

// 連結物件類別，用於繪製basic物件間的連線
public class LinkShape {
    private Shape fromShape, toShape;
    private Point start, end;
    private LinkType type;
    private List<Point> path;

     // 創建一個連結物件
     // @param fromShape 起始basic
     // @param toShape 目標basic
     // @param start 起點
     // @param end 終點
     // @param type 連線類型
     // @param path 連線路徑
    public LinkShape(Shape fromShape, Shape toShape, Point start, Point end, LinkType type, List<Point> path) {
        this.fromShape = fromShape;
        this.toShape = toShape;
        this.start = start;
        this.end = end;
        this.type = type;
        this.path = path != null ? new ArrayList<>(path) : new ArrayList<>();
    }
    
    // 根據ToolMode字串創建連線類型
    public LinkShape(Shape fromShape, Shape toShape, Point start, Point end, String typeName, List<Point> path) {
        this(fromShape, toShape, start, end, 
             LinkType.valueOf(typeName.toUpperCase()), 
             path);
    }
    
    public Shape getFromShape() {
        return fromShape;
    }
    
    public void setFromShape(Shape fromShape) {
        this.fromShape = fromShape;
    }
    
    public Shape getToShape() {
        return toShape;
    }
    
    public void setToShape(Shape toShape) {
        this.toShape = toShape;
    }
    
    public Point getStart() {
        return start;
    }
    
    public void setStart(Point start) {
        this.start = start;
    }
    
    public Point getEnd() {
        return end;
    }
    
    public void setEnd(Point end) {
        this.end = end;
    }
    
    public List<Point> getPath() {
        return path;
    }

    public void setPath(List<Point> newPath) {
        this.path = new ArrayList<>(newPath);
    }

    // 繪製連線的直線部分
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        Stroke originalStroke = g2d.getStroke();
        
        // 使用更平滑的線條樣式
        g2d.setStroke(new BasicStroke(
            2.0f,                    // 線寬增加到2.0f
            BasicStroke.CAP_ROUND,   // 圓形線帽
            BasicStroke.JOIN_ROUND   // 圓形連接點
        ));
        
        g2d.setColor(Color.BLACK);
        
        // 繪製整個路徑
        for (int i = 0; i < path.size() - 1; i++) {
            Point p1 = path.get(i);
            Point p2 = path.get(i + 1);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // 繪製association/generalization/composition
        if (path.size() > 1) {
            Point from = path.get(path.size() - 2);
            Point to = path.get(path.size() - 1);
            drawArrow(g2d, from.x, from.y, to.x, to.y, type);
        }
        
        g2d.setStroke(originalStroke); // 還原原來的線條樣式
    }

    // 根據連線類型繪製箭頭 
    public void drawArrow(Graphics g, int x1, int y1, int x2, int y2, LinkType type) {
        int arrowSize = 14;
        double angle = Math.atan2(y2 - y1, x2 - x1);
        Graphics2D g2d = (Graphics2D) g;

        switch (type) { // 如果未來要擴充新的連線類型，這邊需要改
            case GENERALIZATION:
                drawGeneralization(g2d, x2, y2, angle, arrowSize);
                break;
            case COMPOSITION:
                drawComposition(g2d, x2, y2, angle, arrowSize);
                break;
            case ASSOCIATION:
            default:
                drawAssociation(g2d, x2, y2, angle, arrowSize);
                break;
        }
    }
    
    // 畫generalization箭頭
    private void drawGeneralization(Graphics2D g2d, int x2, int y2, double angle, int arrowSize) {
        // 調整空心三角形的形狀參數
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
        
        // 畫空心三角形
        Color origColor = g2d.getColor();
        g2d.setColor(Color.WHITE);
        g2d.fillPolygon(xPoints, yPoints, 3); // 白色去填
        g2d.setColor(origColor);
        g2d.drawPolygon(xPoints, yPoints, 3); // 原始g2d去畫(黑色)
    }

    // 畫composition箭頭
    private void drawComposition(Graphics2D g2d, int x2, int y2, double angle, int arrowSize) {
        // 用正方形旋轉30度來做菱形
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
        
        // 畫空心菱形
        g2d.setColor(Color.WHITE);
        g2d.fillPolygon(xPoints, yPoints, 4);
        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(xPoints, yPoints, 4);
    }

    // 畫association箭頭
    private void drawAssociation(Graphics2D g2d, int x2, int y2, double angle, int arrowSize) {
        Stroke originalStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        g2d.drawLine(x2, y2, 
                  x2 - (int) (arrowSize * Math.cos(angle - Math.PI / 6)), 
                  y2 - (int) (arrowSize * Math.sin(angle - Math.PI / 6)));
        g2d.drawLine(x2, y2, 
                  x2 - (int) (arrowSize * Math.cos(angle + Math.PI / 6)), 
                  y2 - (int) (arrowSize * Math.sin(angle + Math.PI / 6)));
                  
        g2d.setStroke(originalStroke);
    }
    
    // 以下使用三個靜態 method 來讓 CanvasPanel 呼叫畫三種連線的箭頭
    public static void drawArrow(Graphics g, Point from, Point to) {
        LinkShape linkShape = new LinkShape(null, null, null, null, LinkType.ASSOCIATION, null);
        linkShape.drawArrow(g, from.x, from.y, to.x, to.y, LinkType.ASSOCIATION);
    }
    
    public static void drawTriangle(Graphics g, Point from, Point to) {
        LinkShape linkShape = new LinkShape(null, null, null, null, LinkType.GENERALIZATION, null);
        linkShape.drawArrow(g, from.x, from.y, to.x, to.y, LinkType.GENERALIZATION);
    }
    
    public static void drawDiamond(Graphics g, Point from, Point to) {
        LinkShape linkShape = new LinkShape(null, null, null, null, LinkType.COMPOSITION, null);
        linkShape.drawArrow(g, from.x, from.y, to.x, to.y, LinkType.COMPOSITION);
    }
}