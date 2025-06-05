package strategy;

import java.awt.Point;
import java.awt.event.MouseEvent;

// 工具策略接口，定義不同工具的行為
public interface ToolStrategy {
    // 處理滑鼠按下事件
    // @param e 滑鼠事件
    // @return 是否處理了事件
    boolean handleMousePressed(MouseEvent e);
    
    // 處理滑鼠拖曳事件
    // @param e 滑鼠事件
    // @return 是否處理了事件
    
    boolean handleMouseDragged(MouseEvent e);
    
    // 處理滑鼠放開事件
    // @param e 滑鼠事件
    // @return 是否處理了事件
    boolean handleMouseReleased(MouseEvent e);
    
    // 取得當前滑鼠座標 
    // @param e 滑鼠事件
    // @return 座標點
    default Point getPoint(MouseEvent e) {
        return new Point(e.getX(), e.getY());
    }
}