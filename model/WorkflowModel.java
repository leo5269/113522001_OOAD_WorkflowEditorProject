package model;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import link.LinkShape;
import shape.CompositeShape;
import shape.ConcreteCompositeShape;
import shape.Shape;
import shape.ShapeHandler;
import util.PortResult;

// 工作流程 Model 類，負責管理 Shape 和連線
public class WorkflowModel {
    private final List<Shape> shapes = new ArrayList<>();
    private final List<LinkShape> links = new ArrayList<>();
    private List<Shape> selectedShapes = new ArrayList<>();
    private final List<ModelChangeListener> listeners = new ArrayList<>();
    

    public void addModelChangeListener(ModelChangeListener listener) { // 添加model變更監聽器
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void removeModelChangeListener(ModelChangeListener listener) { // 移除model變更監聽器
        listeners.remove(listener);
    }

    public void addShape(Shape shape) { // 添加shape
        shapes.add(shape);
        notifyShapeAdded(shape);
    }

    public void removeShape(Shape shape) { // 移除shape
        shapes.remove(shape);
        notifyShapeRemoved(shape);
    }

    public void shapeModified(Shape shape) { // 通知shape被修改
        notifyShapeModified(shape);
    }

    public void addLink(LinkShape link) { // 添加連線
        links.add(link);
        notifyLinkAdded(link);
    }

    public void removeLink(LinkShape link) { // 移除連線
        links.remove(link);
        notifyLinkRemoved(link);
    }

    public void linkModified(LinkShape link) { // 通知連線被修改
        notifyLinkModified(link);
    }

    public void setSelectedShapes(List<Shape> shapes) { // 設置選中的shape
        selectedShapes = new ArrayList<>(shapes);
        notifySelectionChanged();
    }

    public List<Shape> getSelectedShapes() { // 取得選中的shape
        return new ArrayList<>(selectedShapes);
    }

    public List<Shape> getAllShapes() { // 取得所有shape
        return new ArrayList<>(shapes);
    }

    public List<LinkShape> getAllLinks() { // 取得所有連線
        return new ArrayList<>(links);
    }
    
    // 找到指定座標處最上層的shape
    // 使用多型方式處理
    public Shape getTopMostShapeAt(int x, int y) {
        List<Shape> candidates = new ArrayList<>();
        List<Shape> compositeCandidates = new ArrayList<>();
        
        // 使用 ShapeHandler 來收集候選shape，下面有寫一個 ClickCandidateCollector 來 implements ShapeHandler 
        ClickCandidateCollector collector = new ClickCandidateCollector(x, y);
        for (Shape shape : shapes) { // 這邊就在做多型了
            shape.accept(collector); // 看傳進去的是Shape還是Composite shape
            if (collector.isCandidate()) {
                if (collector.isCompositeCandidate()) {
                    compositeCandidates.add(shape);
                } else {
                    candidates.add(shape);
                }
                collector.reset();
            }
        }
        
        // 如果點擊在composite shape的子shape上，直接返回composite shape(不檢查深度)
        if (!compositeCandidates.isEmpty()) {
            // 如果有多個composite shape，返回第一個找到的
            return compositeCandidates.get(0);
        }
        
        // 否則處理basic shape，需要檢查深度和重疊群組
        if (!candidates.isEmpty()) {
            // 如果只有一個候選shape，檢查它是否是其重疊群組的最上層
            if (candidates.size() == 1) {
                Shape candidate = candidates.get(0);
                if (isTopMostInOverlappingGroup(candidate)) {
                    return candidate;
                }
                return null;
            }
            
            // 多個候選shape的情況
            // 找出所有包含點擊位置的shape的重疊群組
            Set<Shape> overlappingGroup = getOverlappingGroup(candidates);
            
            // 在包含點擊位置的shape中找出深度最大的
            Shape topMost = null;
            int maxDepth = -1;
            
            for (Shape shape : candidates) {
                if (shape.getDepth() > maxDepth) {
                    // 確認這個shape在其重疊群組中是最上層的
                    boolean isTopInGroup = true;
                    Rectangle shapeRect = new Rectangle(
                        shape.getX(), shape.getY(),
                        shape.getWidth(), shape.getHeight()
                    );
                    
                    for (Shape other : overlappingGroup) {
                        if (other != shape) {
                            Rectangle otherRect = new Rectangle(
                                other.getX(), other.getY(),
                                other.getWidth(), other.getHeight()
                            );
                            
                            if (shapeRect.intersects(otherRect) && other.getDepth() > shape.getDepth()) { // 下面的物件深度比較大的情況下就是false
                                isTopInGroup = false;
                                break;
                            }
                        }
                    }
                    
                    if (isTopInGroup) {
                        maxDepth = shape.getDepth(); // 各群basic物件深度最大的
                        topMost = shape;
                    }
                }
            }
            
            return topMost;
        }
        
        return null;
    }

    // 點擊候選收集器
    private class ClickCandidateCollector implements ShapeHandler {
        private final int x, y;
        private boolean isCandidate = false;
        private boolean isCompositeCandidate = false;
        
        public ClickCandidateCollector(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public void handleBasicShape(Shape shape) {
            if (shape.contains(x, y)) {
                isCandidate = true;
                isCompositeCandidate = false;
            }
        }
        
        @Override
        public void handleCompositeShape(CompositeShape composite) {
            // 檢查點擊是否在任何子shape內
            for (Shape child : composite.getChildren()) {
                if (child.contains(x, y)) {
                    isCandidate = true;
                    isCompositeCandidate = true; // 因為是composite物件，所以要為true
                    return;
                }
            }
        }
        
        public boolean isCandidate() {
            return isCandidate;
        }
        
        public boolean isCompositeCandidate() {
            return isCompositeCandidate;
        }
        
        public void reset() {
            isCandidate = false;
            isCompositeCandidate = false;
        }
    }
    
    // 獲取包含指定shape集合的重疊群組
    // 使用遞迴方式找出所有相互重疊的shape
    private Set<Shape> getOverlappingGroup(List<Shape> initialShapes) {
        Set<Shape> group = new HashSet<>(initialShapes);
        Set<Shape> toCheck = new HashSet<>(initialShapes); // 要被 check 是否加進 group的集合
        
        while (!toCheck.isEmpty()) {
            Set<Shape> newOverlaps = new HashSet<>(); // 用來操作給每一輪加進 group 的中介集合
            
            for (Shape checkShape : toCheck) { // 要把用來 check 集合的物件畫出來
                Rectangle checkRect = new Rectangle(
                    checkShape.getX(), checkShape.getY(),
                    checkShape.getWidth(), checkShape.getHeight()
                );
                
                for (Shape shape : shapes) {
                    if (!group.contains(shape)) { // 找 group 以外的shape 有無跟group的shape有交集
                        Rectangle shapeRect = new Rectangle(
                            shape.getX(), shape.getY(),
                            shape.getWidth(), shape.getHeight()
                        );
                        
                        if (checkRect.intersects(shapeRect)) {
                            newOverlaps.add(shape);
                        }
                    }
                }
            }
            
            toCheck = newOverlaps;
            group.addAll(newOverlaps);
        }
        
        return group;
    }
    
    // 根據座標選擇shape
    public void selectShapesAt(Point point, boolean addToSelection) {
        // 點選模式
        Shape shape = getTopMostShapeAt(point.x, point.y);
        
        if (shape != null) {
            if (!addToSelection) {
                selectedShapes.clear();
            }
        
            // 如果點到的是已選中的shape，則取消選中
            if (selectedShapes.contains(shape) && addToSelection) {
                selectedShapes.remove(shape);
            } else {
                // 否則添加到選中列表
                if (!selectedShapes.contains(shape)) {
                    selectedShapes.add(shape);
                }
            }

            notifySelectionChanged();
        }
    }
    
    // 根據框選區域選擇shape
    public void selectShapesInRect(Rectangle selectionRect, boolean addToSelection) {
        List<Shape> newSelection = new ArrayList<>();
        Set<Shape> processedChildren = new HashSet<>();
        
        // 使用 ShapeHandler 處理不同類型的shape
        RectSelectionHandler handler = new RectSelectionHandler(selectionRect, processedChildren);
        
        // 第一步：處理所有shape
        for (Shape shape : shapes) {
            shape.accept(handler);
            if (handler.shouldSelect()) {
                newSelection.add(shape);
                handler.reset();
            }
        }
        
        // 第二步：處理未被composite shape包含的basic shape
        List<Shape> candidateBasicShapes = new ArrayList<>();
        for (Shape shape : shapes) {
            BasicShapeChecker checker = new BasicShapeChecker();
            shape.accept(checker);
            
            if (checker.isBasicShape() && !processedChildren.contains(shape)) {
                Rectangle shapeBounds = new Rectangle(
                    shape.getX(), shape.getY(), 
                    shape.getWidth(), shape.getHeight()
                );
                
                if (selectionRect.contains(shapeBounds)) {
                    candidateBasicShapes.add(shape);
                }
            }
        }
        
        // 第三步：對basic shape進行深度檢查
        for (Shape shape : candidateBasicShapes) {
            if (isTopMostInOverlappingGroup(shape)) {
                newSelection.add(shape);
            }
        }
        
        if (!newSelection.isEmpty()) {
            // 更新選擇
            if (!addToSelection) {
                selectedShapes.clear();
            }
        
        for (Shape shape : newSelection) {
            if (!selectedShapes.contains(shape)) {
                selectedShapes.add(shape);
            }
        }
        
        notifySelectionChanged();
    }
}
    
    // rect選擇處理器
    private class RectSelectionHandler implements ShapeHandler {
        private final Rectangle selectionRect;
        private final Set<Shape> processedChildren;
        private boolean shouldSelect = false;
        
        public RectSelectionHandler(Rectangle selectionRect, Set<Shape> processedChildren) {
            this.selectionRect = selectionRect;
            this.processedChildren = processedChildren;
        }
        
        @Override
        public void handleBasicShape(Shape shape) {
            // basic shape 在後續處理
            shouldSelect = false;
        }
        
        @Override
        public void handleCompositeShape(CompositeShape composite) {
            List<Shape> children = composite.getChildren();
            
            if (!children.isEmpty()) {
                boolean allChildrenInRect = true;
                
                for (Shape child : children) {
                    Rectangle childBounds = new Rectangle(
                        child.getX(), child.getY(),
                        child.getWidth(), child.getHeight()
                    );
                    
                    if (!selectionRect.contains(childBounds)) {
                        allChildrenInRect = false;
                        break;
                    }
                }
                
                // 如果所有子shape都在選擇框內，選中composite shape
                if (allChildrenInRect) {
                    shouldSelect = true;
                    processedChildren.addAll(children);
                }
            }
        }
        
        public boolean shouldSelect() {
            return shouldSelect;
        }
        
        public void reset() {
            shouldSelect = false;
        }
    }
    
    // basic shape檢查器
    private class BasicShapeChecker implements ShapeHandler {
        private boolean isBasic = false;
        
        @Override
        public void handleBasicShape(Shape shape) {
            isBasic = true;
        }
        
        @Override
        public void handleCompositeShape(CompositeShape composite) {
            isBasic = false;
        }
        
        public boolean isBasicShape() {
            return isBasic;
        }
    }
    
    // 檢查shape是否是其重疊群組中的最上層
    // 使用多型方式
    private boolean isTopMostInOverlappingGroup(Shape target) {
        // 使用 ShapeHandler 檢查是否為composite shape
        CompositeChecker checker = new CompositeChecker();
        target.accept(checker);
        
        // composite shape沒有深度，不需要檢查
        if (checker.isComposite()) {
            return true;
        }
        
        Rectangle targetRect = new Rectangle( // 這是新建出來的 rect 或 oval
            target.getX(), target.getY(),
            target.getWidth(), target.getHeight()
        );
        
        // 找出所有與目標shape重疊的basic shape
        List<Shape> overlappingShapes = new ArrayList<>();
        overlappingShapes.add(target);
        
        for (Shape shape : shapes) {
            if (shape != target) { // 每一個在shapes裡面的基本 shape 都去檢查有沒有跟新建的基本 shape 重疊
                CompositeChecker shapeChecker = new CompositeChecker();
                shape.accept(shapeChecker);
                
                if (!shapeChecker.isComposite()) { 
                    Rectangle shapeRect = new Rectangle(
                        shape.getX(), shape.getY(),
                        shape.getWidth(), shape.getHeight()
                    );
                    
                    if (targetRect.intersects(shapeRect)) {
                        overlappingShapes.add(shape);
                    }
                }
            }
        }
        
        // 獲取完整的重疊群組(只包含basic shape)
        Set<Shape> group = getOverlappingGroup(overlappingShapes);
        
        // 檢查目標shape是否有最高深度
        for (Shape shape : group) {
            CompositeChecker groupChecker = new CompositeChecker();
            shape.accept(groupChecker);
            
            if (shape != target && !groupChecker.isComposite() && shape.getDepth() > target.getDepth()) {
                Rectangle shapeRect = new Rectangle(
                    shape.getX(), shape.getY(),
                    shape.getWidth(), shape.getHeight()
                );
                
                if (targetRect.intersects(shapeRect)) {
                    return false;
                }
            }
        }
        
        return true;
    }

    // composite shape檢查器
    private class CompositeChecker implements ShapeHandler {
        private boolean isComposite = false;
        
        @Override
        public void handleBasicShape(Shape shape) {
            isComposite = false;
        }
        
        @Override
        public void handleCompositeShape(CompositeShape composite) {
            isComposite = true;
        }
        
        public boolean isComposite() {
            return isComposite;
        }
    }
    
    // 找到shape包含的最上層composite shape
    // 使用多型方式
    public Shape getTopMostComposite(Shape target) {
        Shape current = target;
        boolean found;

        do {
            found = false;
            for (Shape s : shapes) {
                CompositeContainmentChecker checker = new CompositeContainmentChecker(current);
                s.accept(checker);
                
                if (checker.containsTarget()) {
                    current = s;
                    found = true;
                    break;
                }
            }
        } while (found);

        return current;
    }
    
    // composite shape 包含檢查器
    private class CompositeContainmentChecker implements ShapeHandler {
        private final Shape target;
        private boolean contains = false;
        
        public CompositeContainmentChecker(Shape target) {
            this.target = target;
        }
        
        @Override
        public void handleBasicShape(Shape shape) {
            contains = false;
        }
        
        @Override
        public void handleCompositeShape(CompositeShape composite) {
            contains = composite.getChildren().contains(target);
        }
        
        public boolean containsTarget() {
            return contains;
        }
    }
    
    public boolean isTopMostInGroup(Shape target) {  // 檢查shape是否是其重疊組中的最上層
        Rectangle rect = new Rectangle(target.getX(), target.getY(), target.getWidth(), target.getHeight());
        for (Shape other : shapes) {
            if (other == target) continue;
            Rectangle orect = new Rectangle(other.getX(), other.getY(), other.getWidth(), other.getHeight());
            if (orect.intersects(rect) && other.getDepth() > target.getDepth()) {
                return false;
            }
        }
        return true;
    }
    
    public void bringToFront(Shape shape) { // 將shape提到前面(增加深度)
        int maxDepth = shapes.stream().mapToInt(Shape::getDepth).max().orElse(0);
        shape.setDepth(maxDepth + 1);
        notifyShapeModified(shape);
    }
    
    public void assignDepthByOverlapGroup(Shape newShape) { // 分配新建shape的深度，基於重疊關係
        Rectangle newRect = new Rectangle(newShape.getX(), newShape.getY(), newShape.getWidth(), newShape.getHeight());
        int maxDepthInOverlap = -1;
        for (Shape s : shapes) {
            Rectangle r = new Rectangle(s.getX(), s.getY(), s.getWidth(), s.getHeight());
            if (r.intersects(newRect)) {
                maxDepthInOverlap = Math.max(maxDepthInOverlap, s.getDepth());
            }
        }
        newShape.setDepth(maxDepthInOverlap + 1);
    }

    public Point getPortAt(Shape shape, int x, int y) { // 找到shape上指定座標處的port
        // 不需要額外檢查，因為傳入的 shape 應該已經是 getTopMostShapeAt 返回的最上層shape
        for (Point p : shape.getConnectionPorts()) {
            if (new Rectangle(p.x - 5, p.y - 5, 10, 10).contains(x, y)) {
                return p;
            }
        }
        return null;
    }

    public PortResult getClosestTopPort(int x, int y, int threshold) { // 找到最接近指定座標的port
        Shape topShape = getTopMostShapeAt(x, y);
        if (topShape == null) return null;

        Point best = null;
        double bestDist = Double.MAX_VALUE;
        for (Point p : topShape.getConnectionPorts()) {
            double dist = p.distance(x, y);
            if (dist <= threshold && dist < bestDist) { // 線長度小於15 且 座標點
                bestDist = dist;
                best = p;
            }
        }
        return best != null ? new PortResult(topShape, best) : null;
    }

    public void updateConnectedLinks() { // 更新所有連接到選定shape的連線
        boolean modified = false;
        
        for (LinkShape link : links) {
            if (link.getFromShape() != null) {
                Point closestPort = null;
                
                // 使用多型代替 instanceof 判斷
                PortResult result = getLinkPort(link.getFromShape(), link.getStart());
                if (result != null) {
                    closestPort = result.getPort();
                }
                
                if (closestPort != null) {
                    link.setStart(closestPort);
                    modified = true;
                }
            }
            
            if (link.getToShape() != null) {
                Point closestPort = null;
                
                // 使用多型代替 instanceof 判斷
                PortResult result = getLinkPort(link.getToShape(), link.getEnd());
                if (result != null) {
                    closestPort = result.getPort();
                }
                
                if (closestPort != null) {
                    link.setEnd(closestPort);
                    modified = true;
                }
            }
            
            // 更新路徑
            if (modified) {
                List<Point> newPath = new ArrayList<>();
                if (link.getStart() != null) newPath.add(link.getStart());
                if (link.getEnd() != null) newPath.add(link.getEnd());
                link.setPath(newPath);
                notifyLinkModified(link);
            }
        }
    }
    
    // 使用多型獲取連接點
    private PortResult getLinkPort(Shape shape, Point oldPort) {
        LinkPortHandler handler = new LinkPortHandler(oldPort);
        shape.accept(handler);
        return handler.getResult();
    }

    // 連接點處理器
    private class LinkPortHandler implements ShapeHandler {
        private final Point oldPort;
        private PortResult result;
        
        public LinkPortHandler(Point oldPort) {
            this.oldPort = oldPort;
        }
        
        @Override
        public void handleBasicShape(Shape shape) {
            Point closest = findClosestPort(shape, oldPort);
            if (closest != null) {
                result = new PortResult(shape, closest);
            }
        }
        
        @Override
        public void handleCompositeShape(CompositeShape composite) {
            result = findClosestChildPort(composite, oldPort);
        }
        
        public PortResult getResult() {
            return result;
        }
    }
    
    // 群組選取的shape
    // 使用多型方式
    public void groupSelectedShapes() {
        if (selectedShapes.size() > 1) {
            // 創建一個新的composite shape
            List<Shape> shapesToGroup = new ArrayList<>(selectedShapes);
            
            // 確保所有要group的composite shape都有正確的邊界
            BoundsUpdater boundsUpdater = new BoundsUpdater();
            for (Shape shape : shapesToGroup) {
                shape.accept(boundsUpdater);
            }
            
            // 從模型中移除選中的shape
            for (Shape shape : shapesToGroup) {
                shapes.remove(shape);
                notifyShapeRemoved(shape);
            }
            
            // 創建新的composite shape
            ConcreteCompositeShape group = new ConcreteCompositeShape(shapesToGroup);
            
            // 確保composite shape的邊界是緊密的，僅包含子shape的範圍
            group.updateBounds();
            
            // composite shape不設置深度值
            
            // 添加到模型
            shapes.add(group);
            
            // 更新選擇為新的composite shape
            selectedShapes.clear();
            selectedShapes.add(group);
            
            notifyShapeAdded(group);
            notifySelectionChanged();
            
            // 更新連線
            updateConnectedLinks();
        }
    }
    
    // 邊界更新器
    private class BoundsUpdater implements ShapeHandler {
        @Override
        public void handleBasicShape(Shape shape) {
            // basic shape 不需要更新邊界
        }
        
        @Override
        public void handleCompositeShape(CompositeShape composite) {
            composite.updateBounds();
        }
    }
    
    // ungroup選取的shape
    // 使用多型方式
    public void ungroupSelectedShape() {
        if (selectedShapes.size() == 1) {
            Shape shape = selectedShapes.get(0);
            
            UngroupHandler ungroupHandler = new UngroupHandler();
            shape.accept(ungroupHandler);
            
            if (ungroupHandler.canUngroup()) {
                // 從模型中移除這個composite shape
                shapes.remove(shape);
                notifyShapeRemoved(shape);
                
                // 獲取直接子shape列表
                List<Shape> directChildren = ungroupHandler.getChildren();
                
                // 將子shape添加到模型中
                for (Shape child : directChildren) {
                    // 如果子shape也是composite shape,確保其邊界正確
                    child.accept(boundsUpdater);
                    
                    shapes.add(child);
                    notifyShapeAdded(child);
                }
                
                // 更新選擇為解構後的子shape
                selectedShapes.clear();
                selectedShapes.addAll(directChildren);
                
                notifySelectionChanged();
                
                // 更新連線
                updateConnectedLinks();
            }
        }
    }
    
    // ungroup處理器
    private class UngroupHandler implements ShapeHandler {
        private boolean canUngroup = false;
        private List<Shape> children = new ArrayList<>();
        
        @Override
        public void handleBasicShape(Shape shape) {
            canUngroup = false;
        }
        
        @Override
        public void handleCompositeShape(CompositeShape composite) {
            canUngroup = true;
            children = new ArrayList<>(composite.getChildren());
        }
        
        public boolean canUngroup() {
            return canUngroup;
        }
        
        public List<Shape> getChildren() {
            return children;
        }
    }
    
    private final BoundsUpdater boundsUpdater = new BoundsUpdater();
    
    // 尋找composite shape中最近的子物件連接點
    private PortResult findClosestChildPort(CompositeShape composite, Point oldPort) {
        Point closest = null;
        Shape closestShape = null;
        double bestDist = Double.MAX_VALUE;
        
        for (Shape child : composite.getChildren()) {
            // 使用多型處理子shape
            ChildPortFinder finder = new ChildPortFinder(oldPort);
            child.accept(finder);
            PortResult childResult = finder.getResult();
            
            if (childResult != null) {
                double dist = childResult.getPort().distance(oldPort);
                if (dist < bestDist) {
                    bestDist = dist;
                    closest = childResult.getPort();
                    closestShape = childResult.getShape();
                }
            }
        }
        
        // 如果沒有找到子物件的連接點，使用composite shape自己的連接點
        if (closest == null) {
            closest = findClosestPort(composite, oldPort);
            closestShape = composite;
        }
        
        return closest != null ? new PortResult(closestShape, closest) : null;
    }
    
    // 子shape連接點尋找器
    private class ChildPortFinder implements ShapeHandler {
        private final Point oldPort;
        private PortResult result;
        
        public ChildPortFinder(Point oldPort) {
            this.oldPort = oldPort;
        }
        
        @Override
        public void handleBasicShape(Shape shape) {
            Point closestPort = findClosestPort(shape, oldPort);
            if (closestPort != null) {
                result = new PortResult(shape, closestPort);
            }
        }
        
        @Override
        public void handleCompositeShape(CompositeShape composite) {
            result = findClosestChildPort(composite, oldPort);
        }
        
        public PortResult getResult() {
            return result;
        }
    }

    // 找到shape上最接近指定點的port
    private Point findClosestPort(Shape shape, Point oldPort) {
        Point closest = null;
        double bestDist = Double.MAX_VALUE;
        for (Point p : shape.getConnectionPorts()) {
            double d = p.distance(oldPort);
            if (d < bestDist) {
                bestDist = d;
                closest = p;
            }
        }
        return closest;
    }
    
    // 檢查兩個shape是否有交集
    public boolean shapesIntersect(Shape shape1, Shape shape2) {
        Rectangle r1 = new Rectangle(
            shape1.getX(), shape1.getY(), 
            shape1.getWidth(), shape1.getHeight()
        );
        Rectangle r2 = new Rectangle(
            shape2.getX(), shape2.getY(), 
            shape2.getWidth(), shape2.getHeight()
        );
        return r1.intersects(r2);
    }
    
    // 通知方法
    private void notifyShapeAdded(Shape shape) {
        for (ModelChangeListener listener : listeners) {
            listener.onShapeAdded(shape);
        }
    }
    
    private void notifyShapeRemoved(Shape shape) {
        for (ModelChangeListener listener : listeners) {
            listener.onShapeRemoved(shape);
        }
    }
    
    private void notifyShapeModified(Shape shape) {
        for (ModelChangeListener listener : listeners) {
            listener.onShapeModified(shape);
        }
    }
    
    private void notifyLinkAdded(LinkShape link) {
        for (ModelChangeListener listener : listeners) {
            listener.onLinkAdded(link);
        }
    }
    
    private void notifyLinkRemoved(LinkShape link) {
        for (ModelChangeListener listener : listeners) {
            listener.onLinkRemoved(link);
        }
    }
    
    private void notifyLinkModified(LinkShape link) {
        for (ModelChangeListener listener : listeners) {
            listener.onLinkModified(link);
        }
    }
    
    private void notifySelectionChanged() {
        for (ModelChangeListener listener : listeners) {
            listener.onSelectionChanged(selectedShapes);
        }
    }
}