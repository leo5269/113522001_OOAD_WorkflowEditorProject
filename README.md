# 113522001_OOAD_WorkflowEditorProject
OOAD project

Requirement and Spec
(in Workflow Design syntax and format)
本需求文件採用 Object & Link 來描述系統需求與規格。我們將使用此 Workflow
Design editor 的 scenarios 先分類成幾大類:

A. 建立物件
B. 連結物件
C. 選取物件
D. 群組物件

Definitions:
● 基本物件(basic object): 如 rect 或 oval 物件。
● 連結物件(link): 如各種 association、generalization、composition links。
● composite 物件: composite 物件由多個基本物件經過 group 的功能組合
而成。composite 物件是一種樹狀的 container,也就是說 composite 物件
本身又可以包含 composite 物件。composite 物件的範圍可以定義為最小
的正方形區域完全包含它的所有組成物件。
● 物件深度(depth):每個物件相對於其他的物件都有一個深度值 0-99, 若某
個物件的深度值比其他物件深度值少,在繪圖時,該物件應該覆蓋其他物
件,而且先接收與攔截落於該物件的 mouse 事件。也就是說,當兩個物件
重疊時有mouse事件被觸發,則只有最上層的物件會接收到該 mouse 事
件。

UseCase A. Creating an object
precondition: 適用 rect 或 oval 按鈕被按下的情況

Case:
1. 按下按鈕之後按鈕的顏色變黑,以告知使用者目前欲建立物件的 mode。
2. 使用者移動游標至編輯區域。
3. 於座標 (x,y) 按下左鍵,則以 x,y 為左上角建立所選定的物件(在編輯區中,繪
製一個空白的物件)。
4. 使用者可重複 2-3 一直在編輯區域內建立同樣的物件,直到 mode 被改
變。
● Alternatives a.1 使用者按其他按鈕,則切換到其他按鈕的 mode。

UseCase B. Creating a Link
precondition: 適用 association, generalization 以及 composition 三個按鈕。
definition: connection link 的建立是連結在基本物件的 connection ports 上。 基
本物件 ,如下圖 rect 有 8 個,oval 有 4 個 connection ports。

Case:
1. 使用者在編輯地區的某個 rect 或 oval 物件範圍內按下 mouse 的左鍵,但
是不放開(mouse pressed)。
2. 使用者不放開左鍵,進行拖曳(drag)的動作。
3. 使用者拖曳到另外一個 rect 或 oval 物件範圍內,放開左鍵 (mouse
released)
4. 在編輯區內,建立一個 link 的物件。連接兩個物件。依照 connection link
的種類,將各種箭頭繪製於終點的物件。
● Alternative b.1
使用者 mouse pressed 的座標,不在任何 rect 或 oval 物件,則從 mouse
pressed -> mouse drag -> mouse released 都不會 有任何作用。
● Alternative b.2
使用者 mouse released 的座標,不在任何 rect 或 oval 物件,則不建立任
何 connection link 物件。
也就是說,當使用者於步驟 1 或步驟 3 按下或放開 mouse 左鍵時,請判
斷該座標位於基本物件的那個範圍內。請注意,本規則不適用 composite
物件。

UseCase C. Select / Unselect a single objects
precondition: 適用按鈕 select 被按下的情況。
definition: 當一個基本物件被處於被 select 的狀態,我們會將所有 connection
ports 明確顯示出來,如上圖,以表示基本物件處於被 select 的狀 態。相反的
若基本物件處於不被 select 的狀態,則 connection ports 是隱藏的。

Case 1.
1. 使用者點選某基本物件。
2. 若有其他物件處於被 select 的狀態,取消它們被 select 的狀態。
3. 將此基本物件的 connection ports 做明確的顯示 。
● Alternative c.1 使用者點選的座標,不在任何基本物件內。
● Alternative c.2 若有其他物件處於被 select 的狀態,取消它們被 select 的狀
態。

Case 2.
1. 使用者在編輯區座標 x1,y1 按住 mouse 左鍵不放,x1,y1 不屬於任何基本
物件的範圍內。
2. 若原本其他物件處於被 select 的狀態,取消它們被 select 的狀態。
3. 使用者不放開左鍵,進行拖曳(drag)的動作。
4. 使用者拖曳到另外一個座標 x2,y2,放開左鍵 (mouse released) 。
5. (x1,y1,x2,y2) 形成一個四方形的區域。在該區域內的基本物件若完全落於
此四方形區域,則處於被 select 的狀態 。
● Alternative c.3
(x1,y1,x2,y2) 形成一個四方形的區域。在該區域內的沒有基本物件完全落
於此四方形區域。則本情境等於 unselect 所有之前處於被 select 的狀態。

UseCase D. Group objects
precondition: 適用於按鈕 select 被按下的情況。
Case 1.
1. 使用者到 Edit Menu 選取 Group 的功能
2. 將處於被選取狀態的基本物件合併成一個 composite 物件
Case 2.
1. 當唯一 1 個 composite 物件處於被 select 的狀態時。
2. 使用者到 Edit Menu 選取 UnGroup 的功能。
3. composite 物件解構一層。

UseCase E. Move objects
precondition: 按鈕 select 被按下的情況。
definition: x,y 座標有可能落在某個物件的範圍內,這種情況該基本物件在 x,y 的
座標上繪製會重疊其他物件。基本上物件重疊時,請按照物件深度的次 序來繪
製。
Case:
1. 使用者在編輯地區的某個基本物件(包含 composite 物件)範圍內按 下
mouse 的左鍵,但是不放開(mouse pressed) 。
2. 使用者不放開左鍵,進行拖曳(drag)的動作。
3. 使用者拖曳到另外一個座標 x,y 放開左鍵 (mouse released) 。
4. 該基本物件被移動到新座標 x,y 。
5. 所有連結到該基本物件的 connection links 全部重新繪製。

UseCase F. Customize Label Style
precondition: 當某一個基本物件處於被 select 的狀態時。
Case:
1. 使用者到 Edit Menu 點選 label。
2. 彈跳視窗 Custom label Style,提供以下選項:
a. 標籤名稱 (label name):輸入新名稱,變更顯示標籤內容。
b. 標籤形狀(Label Shape):可選擇矩形、橢圓兩種不同形狀。
c. 顏色與樣式(Color & FontSize):變更標籤背景色、字體大小。
3. 使用者可以按 OK,Object 上的 label 即時更新,或是選擇 Cancel 維持原
設定。
4. 圖例如下(情況也適用於 Oval):
除了 Name 需使用 input 外,其餘設定可以選擇用 Select 的方式呈現。
