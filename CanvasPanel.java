import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import dialog.LabelStyleDialog;
import link.LinkShape;
import model.ModelChangeListener;
import model.WorkflowModel;
import shape.OvalFactory;
import shape.RectangleFactory;
import shape.Shape;
import shape.ShapeFactory;
import shape.ShapeHandler;
import strategy.*;
import tool.ToolMode;

// CanvasPanel類別，處理繪圖和事件
public class CanvasPanel extends JPanel implements 
        SelectToolStrategy.SelectToolDelegate, 
        CreateShapeStrategy.CreateShapeDelegate,
        LinkToolStrategy.LinkToolDelegate,
        ModelChangeListener {
    
    private final WorkflowModel model; // 工作流程的Model
    private final WorkflowEditor editor; // 指向主視窗的reference
    
    // 策略mapping表
    private final Map<ToolMode, ToolStrategy> strategies = new EnumMap<>(ToolMode.class);
    private final Map<ToolMode, LinkToolStrategy> linkStrategies = new EnumMap<>(ToolMode.class);

    // 建立CanvasPanel
    // @param editor 工作流程編輯器
    public CanvasPanel(WorkflowEditor editor) {
        this.editor = editor;
        this.model = new WorkflowModel();
        this.model.addModelChangeListener(this);
        
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(500, 350));
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        
        // 初始化策略
        initializeStrategies();
        
        // 添加滑鼠事件監聽器
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e);
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e);
            }
        });
    }
    
    // 初始化策略物件，使用工廠模式創建shape
    private void initializeStrategies() {
        // 選擇工具
        strategies.put(ToolMode.SELECT, new SelectToolStrategy(this));
        
        // 形狀創建工具
        ShapeFactory rectangleFactory = new RectangleFactory();
        strategies.put(ToolMode.RECTANGLE, new CreateShapeStrategy(this, rectangleFactory));
        
        ShapeFactory ovalFactory = new OvalFactory();
        strategies.put(ToolMode.OVAL, new CreateShapeStrategy(this, ovalFactory));
        
        // 連線工具
        for (ToolMode mode : ToolMode.values()) {
            if (mode.isLinkMode()) { // ToolMode 裡面找這個 method
                LinkToolStrategy linkStrategy = new LinkToolStrategy(this, mode);
                strategies.put(mode, linkStrategy);
                linkStrategies.put(mode, linkStrategy);
            }
        }
    }
    
    // 處理滑鼠事件
    // consumer為java內建的func interface
    // public interface Consumer<T> { // accept(要傳入的參數型別為T)
    //    void accept(T t); // 接受一個參數，不返回值
    // }
    private boolean handleMouseEvent(MouseEvent e, Consumer<ToolStrategy> handler) { // accept(要傳入的參數型別為ToolStrategy)
        ToolMode mode = ToolMode.fromString(editor.getMode());
        ToolStrategy strategy = strategies.get(mode); // map會去找跟mode符合的key
        if (strategy != null) {
            handler.accept(strategy); // 會call strategy.handleMousePressed(e)等等
            return true;
        }
        return false;
    }
    
    private void handleMousePressed(MouseEvent e) { // 處理滑鼠按下事件
        handleMouseEvent(e, strategy -> strategy.handleMousePressed(e)); // Lambda 表達式，compile還不知道strategy
    }
    
    private void handleMouseDragged(MouseEvent e) { // 處理滑鼠拖曳事件
        handleMouseEvent(e, strategy -> strategy.handleMouseDragged(e));
    }
    
    private void handleMouseReleased(MouseEvent e) { // 處理滑鼠放開事件
        handleMouseEvent(e, strategy -> strategy.handleMouseReleased(e));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 收集所有連線port以顯示
        List<Point> allLinkedPorts = new ArrayList<>();
        for (LinkShape link : model.getAllLinks()) {
            if (link.getStart() != null) allLinkedPorts.add(link.getStart());
            if (link.getEnd() != null) allLinkedPorts.add(link.getEnd());
        }

        // 繪製所有shape(按深度排序)
        model.getAllShapes().stream()
            .sorted(Comparator.comparingInt(Shape::getDepth))
            .forEach(shape -> {
                boolean isSelected = model.getSelectedShapes().contains(shape);
                shape.draw(g2d, isSelected, allLinkedPorts);
            });

        // 繪製所有連線
        model.getAllLinks().forEach(link -> link.draw(g2d));
        
        // 繪製正在建立的連線路徑
        drawCurrentLinkPath(g2d);
    }
    
    // 繪製正在建立的連線路徑
    private void drawCurrentLinkPath(Graphics2D g2d) {
        ToolMode mode = ToolMode.fromString(editor.getMode());
        
        if (!mode.isLinkMode()) {
            return;
        }
        
        LinkToolStrategy currentLinkStrategy = linkStrategies.get(mode);
        if (currentLinkStrategy != null && currentLinkStrategy.isDrawingLink()) {
            List<Point> path = currentLinkStrategy.getCurrentPath();
            if (path.size() > 1) {
                Stroke originalStroke = g2d.getStroke();
                g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.setColor(Color.BLACK);

                for (int i = 0; i < path.size() - 1; i++) {
                    Point p1 = path.get(i);
                    Point p2 = path.get(i + 1);
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                }

                if (path.size() >= 2) {
                    Point from = path.get(path.size() - 2);
                    Point to = path.get(path.size() - 1);
                    LinkShape tempLink = new LinkShape(null, null, null, null, mode.getLinkType(), null);
                    tempLink.drawArrow(g2d, from.x, from.y, to.x, to.y, mode.getLinkType());
                }
                g2d.setStroke(originalStroke);
            }
        }
    }
    
    // 實現 SelectToolDelegate 接口的方法
    @Override
    public WorkflowModel getModel() { // 取得model
        return model;
    }
    
    @Override
    public void updateEditMenuForSelection(List<Shape> selectedShapes) { // 更新編輯選單
        editor.updateEditMenuForSelection(selectedShapes);
    }
    
    //  實現 ModelChangeListener 接口的方法
    @Override
    public void onShapeAdded(Shape shape) { 
        repaint(); // model更新給View
    }
    
    @Override
    public void onShapeRemoved(Shape shape) {
        repaint();
    }
    
    @Override
    public void onShapeModified(Shape shape) {
        repaint();
    }
    
    @Override
    public void onLinkAdded(LinkShape link) {
        repaint();
    }
    
    @Override
    public void onLinkRemoved(LinkShape link) {
        repaint();
    }
    
    @Override
    public void onLinkModified(LinkShape link) {
        repaint();
    }

    @Override
    public void onSelectionChanged(List<Shape> selectedShapes) {
        editor.updateEditMenuForSelection(selectedShapes);
        repaint();
    }
    
    public void groupSelectedShapes() { // group select到的shape
        if (model.getSelectedShapes().size() > 1) {
            model.groupSelectedShapes();
        }
    }
    
    public void ungroupSelectedShape() { // ungroup(解構)group select的shape
        if (model.getSelectedShapes().size() == 1) {
            model.ungroupSelectedShape();
        }
    }
    
    // 顯示label對話框
    public void showLabelDialogForSelectedShape() {
        List<Shape> selectedShapes = model.getSelectedShapes();
        if (selectedShapes.size() != 1) return;
        
        Shape shape = selectedShapes.get(0);
        // 使用Visitor pattern替代 instanceof
        LabelDialogHandler handler = new LabelDialogHandler();
        shape.accept(handler);
    }
    
    // label對話框處理器
    private class LabelDialogHandler implements ShapeHandler {
        @Override
        public void handleBasicShape(Shape shape) {
            LabelStyleDialog dialog = new LabelStyleDialog((JFrame) SwingUtilities.getWindowAncestor(CanvasPanel.this), shape);
            dialog.setVisible(true);
            
            if (dialog.isConfirmed()) {
                shape.setLabelText(dialog.getLabelName());
                shape.setLabelShape(dialog.getLabelShape());
                shape.setLabelColor(dialog.getLabelColor());
                shape.setLabelFontSize(dialog.getFontSize());
                model.shapeModified(shape);
            }
        }
        
        @Override
        public void handleCompositeShape(shape.CompositeShape shape) {
            // composite shape不支援label
        }
    }
}