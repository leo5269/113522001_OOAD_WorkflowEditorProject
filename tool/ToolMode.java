package tool;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import link.LinkType;

// ToolMode的列舉，代表Editor可用的各種工具
public enum ToolMode {
    SELECT("select", null),
    ASSOCIATION("association", LinkType.ASSOCIATION),
    GENERALIZATION("generalization", LinkType.GENERALIZATION),
    COMPOSITION("composition", LinkType.COMPOSITION),
    RECTANGLE("rect", null),
    OVAL("oval", null);
    
    private static final Map<String, ToolMode> NAME_MAP = 
            Arrays.stream(values()).collect(Collectors.toMap(ToolMode::getName, Function.identity()));
            
    private static final Set<ToolMode> LINK_MODES = 
            EnumSet.of(ASSOCIATION, GENERALIZATION, COMPOSITION);
    
    private final String name;
    private final LinkType linkType;
    
    ToolMode(String name, LinkType linkType) {
        this.name = name;
        this.linkType = linkType;
    }
    
    public String getName() {
        return name;
    }
 
    public LinkType getLinkType() { // 獲取對應的連線類型
        if (!isLinkMode()) {
            throw new UnsupportedOperationException("Not a link mode: " + this);
        }
        return linkType;
    }
    
    public static ToolMode fromString(String modeName) { // 根據字串名稱取得對應的ToolMode
        return NAME_MAP.getOrDefault(modeName.toLowerCase(), SELECT);
    }

    public boolean isLinkMode() { // 檢查該模式是否為連線
        return LINK_MODES.contains(this);
    }

    public boolean isShapeCreationMode() { // 檢查該模式是否為basic物件的建立模式
        return this == RECTANGLE || this == OVAL;
    }
}