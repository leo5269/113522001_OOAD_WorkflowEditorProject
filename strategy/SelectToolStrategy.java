package strategy;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import model.WorkflowModel;
import shape.CompositeShape;
import shape.Shape;
import shape.ShapeHandler;

// 選擇工具策略，處理選擇模式下的滑鼠事件
public class SelectToolStrategy implements ToolStrategy {
    // 需要委派給CanvasPanel的操作
    public interface SelectToolDelegate {
        WorkflowModel getModel();
        void updateEditMenuForSelection(List<Shape> selectedShapes);
        void repaint();
    }
    
    private final SelectToolDelegate delegate;
    
    private boolean dragging = false;
    private Point dragStartPoint = null;
    private Shape draggingShape = null;
    private Point selectionStart = null;
    private Point selectionEnd = null;
    
    private int initialX, initialY; // 拖曳開始時的物件位置
    
    // 創建選擇工具策略
    // @param delegate 委派物件
    public SelectToolStrategy(SelectToolDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean handleMousePressed(MouseEvent e) {
        WorkflowModel model = delegate.getModel();
        
        // 獲取最上層的basic物件
        Shape shape = model.getTopMostShapeAt(e.getX(), e.getY());
        
        if (shape != null) {
            // 直接使用返回的形狀，不需要找最上層的composite物件
            List<Shape> selectedShapes = new ArrayList<>();
            selectedShapes.add(shape);
            model.setSelectedShapes(selectedShapes);
            delegate.updateEditMenuForSelection(selectedShapes);
            
            dragging = true;
            dragStartPoint = getPoint(e);
            draggingShape = shape;
            initialX = shape.getX();
            initialY = shape.getY();
            delegate.repaint();
        } else {
            // 開始選擇區域
            selectionStart = getPoint(e);
            selectionEnd = null;
        }
        
        return true;
    }

    @Override
    public boolean handleMouseDragged(MouseEvent e) {
        WorkflowModel model = delegate.getModel();
        
        if (dragging && dragStartPoint != null && draggingShape != null) {
            // 拖曳形狀
            int dx = e.getX() - dragStartPoint.x;
            int dy = e.getY() - dragStartPoint.y;
            
            // 使用移動處理器
            MoveHandler handler = new MoveHandler(dx, dy);
            draggingShape.accept(handler);
            
            // 更新拖曳起點
            dragStartPoint = getPoint(e);
            model.updateConnectedLinks();
            delegate.repaint();
        } else if (selectionStart != null) {
            // 更新選擇區域
            selectionEnd = getPoint(e);
            delegate.repaint();
        }
        
        return true;
    }

    @Override
    public boolean handleMouseReleased(MouseEvent e) {
        WorkflowModel model = delegate.getModel();
        
        if (dragging && draggingShape != null) {
            // 結束拖曳，將物件提到前面
            model.bringToFront(draggingShape);
            dragging = false;
            draggingShape = null;
            dragStartPoint = null;
            delegate.repaint();
        } else if (selectionStart != null) {
            // 處理選擇區域
            selectionEnd = getPoint(e);
            processSelectionArea();
            selectionStart = selectionEnd = null;
            delegate.repaint();
        }
        
        return true;
    }
    
    // 形狀移動處理器
    private class MoveHandler implements ShapeHandler {
        private final int dx, dy;
        
        public MoveHandler(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
        
        @Override
        public void handleBasicShape(Shape shape) {
            // 移動基本形狀
            shape.setX(shape.getX() + dx);
            shape.setY(shape.getY() + dy);
        }
        
        @Override
        public void handleCompositeShape(CompositeShape group) {
            // 移動所有子shape
            for (Shape child : group.getChildren()) {
                child.accept(this);
            }
            // 更新composite shape的邊界
            group.updateBounds();
        }
    }

    // 處理選擇區域
    private void processSelectionArea() {
        if (selectionStart == null || selectionEnd == null) {
            return;
        }
        
        WorkflowModel model = delegate.getModel();
        
        Rectangle selectionRect = new Rectangle(
                Math.min(selectionStart.x, selectionEnd.x),
                Math.min(selectionStart.y, selectionEnd.y),
                Math.abs(selectionStart.x - selectionEnd.x),
                Math.abs(selectionStart.y - selectionEnd.y));
        
        // 使用模型的框選方法處理選擇邏輯
        model.selectShapesInRect(selectionRect, false);
        delegate.updateEditMenuForSelection(model.getSelectedShapes());
    }
}