package util;

import java.awt.Point;
import shape.Shape;

// 用來包裝搜尋到的 shape & port
public class PortResult {
    private final Shape shape;
    private final Point port;
    
    public PortResult(Shape shape, Point port) {
        this.shape = shape;
        this.port = port;
    }
    
    public Shape getShape() {
        return shape;
    }
    
    public Point getPort() {
        return port;
    }
}