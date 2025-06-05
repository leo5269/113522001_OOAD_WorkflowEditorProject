package model;

import shape.Shape;
import link.LinkShape;
import java.util.List;

// 模型變更監聽器接口
public interface ModelChangeListener {

    void onShapeAdded(Shape shape); // 當Shape被添加時調用
    
    void onShapeRemoved(Shape shape); // 當Shape被移除時調用
    
    void onShapeModified(Shape shape); // 當Shape被修改時調用
    
    void onLinkAdded(LinkShape link); // 當連線被添加時調用
    
    void onLinkRemoved(LinkShape link); // 當連線被移除時調用
    
    void onLinkModified(LinkShape link); // 當連線被修改時調用
    
    void onSelectionChanged(List<Shape> selectedShapes); // 當選中的Shape變更時調用
}