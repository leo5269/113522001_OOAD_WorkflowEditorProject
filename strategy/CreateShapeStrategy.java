package strategy;

import java.awt.event.MouseEvent;

import model.WorkflowModel;
import shape.Shape;
import shape.ShapeFactory;

// 創建形狀策略，處理創建矩形和橢圓形狀的滑鼠事件
public class CreateShapeStrategy implements ToolStrategy {
    // 需要委派給CanvasPanel的操作
    public interface CreateShapeDelegate {
        WorkflowModel getModel();
        void repaint();
    }
    
    private final CreateShapeDelegate delegate;
    private final ShapeFactory shapeFactory;
    
    // 創建basic物件的策略 
    // @param delegate 委派物件
    // @param shapeFactory 形狀工廠     
    public CreateShapeStrategy(CreateShapeDelegate delegate, ShapeFactory shapeFactory) {
        this.delegate = delegate;
        this.shapeFactory = shapeFactory;
    }

    @Override
    public boolean handleMousePressed(MouseEvent e) {
        // 使用工廠創建basic物件
        Shape newShape = shapeFactory.createShape(e.getX(), e.getY());
        
        if (newShape != null) {
            WorkflowModel model = delegate.getModel(); // CanvasPanel 送過來的
            
            // 正確設置深度，確定新建的basic物件顯示在最上層
            int maxDepth = model.getAllShapes().stream() // 因為是ArrayList，所以List去轉Stream做連環判斷
                .mapToInt(Shape::getDepth)
                .max()
                .orElse(-1); // List是空的回傳 -1
            newShape.setDepth(maxDepth + 1);
            
            model.addShape(newShape);
            delegate.repaint(); // 確保重繪，有可能會因為View快速點擊的滑鼠事件就不理CanvasPanel裡面被Observer通知的repaint()了
            return true;
        }
        
        return false;
    }

    @Override
    public boolean handleMouseDragged(MouseEvent e) { // 創建basic物件不需要處理拖曳
        return false;
    }

    @Override
    public boolean handleMouseReleased(MouseEvent e) { // 創建basic物件不需要處理放開
        return false;
    }
}