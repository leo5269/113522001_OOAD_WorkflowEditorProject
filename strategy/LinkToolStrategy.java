package strategy;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import link.LinkShape;
import model.WorkflowModel;
import shape.Shape;
import tool.ToolMode;
import util.PortResult;

// 連線工具策略，處理建立連線的滑鼠事件
public class LinkToolStrategy implements ToolStrategy {
    // 需要委派給CanvasPanel的操作
    public interface LinkToolDelegate {
        WorkflowModel getModel();
        void repaint();
    }
    
    private final LinkToolDelegate delegate;
    private final ToolMode mode;
    
    private Shape startShape;
    private Point startPort;
    private List<Point> currentPath = new ArrayList<>();
    private boolean isDrawingLink = false;
    
    // 創建連線工具策略
    // @param delegate 委派物件
    // @param mode 工具模式
    public LinkToolStrategy(LinkToolDelegate delegate, ToolMode mode) {
        this.delegate = delegate;
        this.mode = mode;
    }

    @Override
    public boolean handleMousePressed(MouseEvent e) {
        if (!mode.isLinkMode()) {
            return false;
        }
        
        WorkflowModel model = delegate.getModel();
        
        Shape shape = model.getTopMostShapeAt(e.getX(), e.getY()); // 獲取最上層的basic物件
        
        if (shape != null) {
            // 不需要再次檢查是否為最上層，因為 getTopMostShapeAt 已經做了這個檢查
            
            // 檢查是否點擊到port
            Point port = model.getPortAt(shape, e.getX(), e.getY());
            if (port != null) {
                startShape = shape;
                startPort = port;
                currentPath.clear();
                currentPath.add(startPort); 
                currentPath.add(new Point(e.getX(), e.getY()));
                isDrawingLink = true;
                delegate.repaint();
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean handleMouseDragged(MouseEvent e) {
        if (isDrawingLink && currentPath.size() == 2) {
            currentPath.set(1, new Point(e.getX(), e.getY())); // 確保終點的index為1
            delegate.repaint();
            return true;
        }
        
        return false;
    }

    @Override
    public boolean handleMouseReleased(MouseEvent e) {
        if (isDrawingLink && startShape != null && startPort != null) {
            WorkflowModel model = delegate.getModel();
            
            // 使用 getClosestTopPort，它會確保只返回最上層形狀的連接port
            PortResult close = model.getClosestTopPort(e.getX(), e.getY(), 15); // threshold 簡單設置能連線的長度
            
            if (close != null && close.getShape() != startShape) { // 如果有終點且終點的shape不為起點的shape
                currentPath.set(1, close.getPort());
                
                // 創建連線
                LinkShape link = new LinkShape(
                    startShape,
                    close.getShape(),
                    startPort,
                    close.getPort(),
                    mode.getLinkType(),
                    new ArrayList<>(currentPath)
                );
                
                model.addLink(link);
            }
            
            // 清除狀態
            startShape = null;
            startPort = null;
            currentPath.clear();
            isDrawingLink = false;
            delegate.repaint();
            return true;
        }
        
        return false;
    }

    public boolean isDrawingLink() { // 檢查是否正在繪製連線
        return isDrawingLink;
    }
    
    public List<Point> getCurrentPath() { // 取得當前路徑
        return new ArrayList<>(currentPath);
    }
}