package shape;

// Shape工廠接口
public interface ShapeFactory {
    Shape createShape(int x, int y); // 在指定位置創建形狀
}