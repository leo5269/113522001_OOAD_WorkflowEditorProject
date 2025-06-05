package shape;

import java.util.List;

// 具體composite shape類別
public class ConcreteCompositeShape extends CompositeShape {
    
    public ConcreteCompositeShape(List<Shape> children) {
        super(children);
    }
    
    public ConcreteCompositeShape() { // 創建一個空的composite shape
        super(List.of());
    }
}