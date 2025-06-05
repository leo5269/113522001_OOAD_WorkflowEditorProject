package link;

// 連線的類型列舉，代表不同種類的連線
public enum LinkType {
    ASSOCIATION,
    GENERALIZATION,
    COMPOSITION;

    public String toLowerCaseString() { // 轉換為小寫字串表示
        return this.name().toLowerCase();
    }
}