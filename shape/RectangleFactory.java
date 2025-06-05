package shape;

// rect工廠
public class RectangleFactory implements ShapeFactory {
    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 120;
    
    @Override
    public Shape createShape(int x, int y) {
        return new RectangleShape(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
}