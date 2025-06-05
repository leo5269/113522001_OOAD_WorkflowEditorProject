package shape;

// Shape處理器接口，用於多型處理不同類型的Shape
public interface ShapeHandler {
    void handleBasicShape(Shape shape); // 處理basic Shape
    
    void handleCompositeShape(CompositeShape shape); // 處理 composite shape
}