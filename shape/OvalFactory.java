package shape;

// oval工廠
public class OvalFactory implements ShapeFactory {
    private static final int DEFAULT_WIDTH = 120;
    private static final int DEFAULT_HEIGHT = 80;
    
    @Override
    public Shape createShape(int x, int y) {
        return new OvalShape(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
}