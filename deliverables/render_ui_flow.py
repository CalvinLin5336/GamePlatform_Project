from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import textwrap

OUT = Path(__file__).with_name("簡易遊戲平台_UI使用者流程圖_實際畫面版.jpg")
SCREEN_DIR = Path(__file__).with_name("screens")
W, H = 2600, 1650
BG = "#F8FAFF"
NAVY = "#10255E"
TEXT = "#172554"
MUTED = "#64748B"
BLUE = "#246BFD"
BLUE_SOFT = "#EEF5FF"
GREEN = "#25A55F"
GREEN_SOFT = "#EFFAF3"
ORANGE = "#F39A36"
ORANGE_SOFT = "#FFF8ED"
PURPLE = "#7657D5"
PURPLE_SOFT = "#F5F1FF"
DARK = "#0F172A"
DARK2 = "#1E293B"
WHITE = "#FFFFFF"
BORDER = "#CBD5E1"

FONT_REG = "/System/Library/Fonts/STHeiti Light.ttc"
FONT_BOLD = "/System/Library/Fonts/STHeiti Medium.ttc"

def font(size, bold=False):
    return ImageFont.truetype(FONT_BOLD if bold else FONT_REG, size)

im = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(im)

def rr(box, radius=14, fill=WHITE, outline=None, width=2):
    d.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)

def line_text(x, y, value, size=24, color=TEXT, bold=False, anchor=None):
    d.text((x, y), value, font=font(size, bold), fill=color, anchor=anchor)

def wrapped(x, y, value, width_chars, size=20, color=MUTED, bold=False, gap=5):
    lines = []
    for para in value.split("\n"):
        lines.extend(textwrap.wrap(para, width=width_chars, break_long_words=True) or [""])
    for ln in lines:
        line_text(x, y, ln, size, color, bold)
        y += size + gap
    return y

def arrow(x1, y1, x2, y2, color=BLUE, dashed=False, label=None):
    if dashed:
        total = max(1, int(((x2-x1)**2 + (y2-y1)**2) ** .5))
        for start in range(0, total, 18):
            end = min(total, start + 10)
            sx = x1 + (x2-x1) * start / total
            sy = y1 + (y2-y1) * start / total
            ex = x1 + (x2-x1) * end / total
            ey = y1 + (y2-y1) * end / total
            d.line((sx, sy, ex, ey), fill=color, width=4)
    else:
        d.line((x1, y1, x2, y2), fill=color, width=4)
    import math
    a = math.atan2(y2-y1, x2-x1)
    ah = 13
    for off in (2.55, -2.55):
        d.line((x2, y2, x2 + ah*math.cos(a+off), y2 + ah*math.sin(a+off)), fill=color, width=4)
    if label:
        line_text((x1+x2)//2, (y1+y2)//2-20, label, 17, color, True, "mm")

def pill(x, y, value, color=BLUE):
    f = font(16, True)
    bb = d.textbbox((0,0), value, font=f)
    w = bb[2]-bb[0]+20
    rr((x, y, x+w, y+28), 14, WHITE, color, 1)
    d.text((x+w/2, y+14), value, font=f, fill=color, anchor="mm")
    return w

def actual_screen(box, filename, fit="cover"):
    """Place a real browser screenshot inside a rounded frame."""
    x1, y1, x2, y2 = map(int, box)
    path = SCREEN_DIR / filename
    if not path.exists():
        return False
    src = Image.open(path).convert("RGB")
    tw, th = x2 - x1, y2 - y1
    if fit == "contain":
        scale = min(tw / src.width, th / src.height)
        nw, nh = int(src.width * scale), int(src.height * scale)
        resized = src.resize((nw, nh), Image.Resampling.LANCZOS)
        canvas = Image.new("RGB", (tw, th), "#E8EDF5")
        canvas.paste(resized, ((tw - nw) // 2, (th - nh) // 2))
    else:
        target_ratio = tw / th
        source_ratio = src.width / src.height
        if source_ratio > target_ratio:
            crop_w = int(src.height * target_ratio)
            left = (src.width - crop_w) // 2
            src = src.crop((left, 0, left + crop_w, src.height))
        else:
            crop_h = int(src.width / target_ratio)
            top = max(0, (src.height - crop_h) // 2)
            src = src.crop((0, top, src.width, top + crop_h))
        canvas = src.resize((tw, th), Image.Resampling.LANCZOS)
    mask = Image.new("L", (tw, th), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, tw - 1, th - 1), radius=10, fill=255)
    im.paste(canvas, (x1, y1), mask)
    d.rounded_rectangle((x1, y1, x2, y2), radius=10, outline="#334155", width=2)
    return True

def mock_screen(box, kind):
    x1,y1,x2,y2 = box
    rr(box, 10, DARK, "#334155", 2)
    if kind == "login":
        d.rectangle((x1, y1, x1+120, y2), fill="#111D35")
        line_text(x1+60, y1+48, "GP", 34, WHITE, True, "mm")
        line_text(x1+60, y1+82, "GAME", 14, "#93C5FD", True, "mm")
        for yy in (y1+28, y1+68): rr((x1+145,yy,x2-18,yy+28),5,"#F8FAFC",BORDER,1)
        rr((x1+145,y1+112,x2-18,y1+143),5,"#1672E8")
        line_text((x1+145+x2-18)/2,y1+127,"登入",15,WHITE,True,"mm")
        line_text(x1+145,y1+153,"登入｜註冊",13,"#CBD5E1")
    elif kind == "shell":
        d.rectangle((x1,y1,x2,y1+28),fill="#182236")
        line_text(x1+12,y1+7,"遊戲對戰平台",13,WHITE,True)
        line_text(x2-12,y1+7,"Lobby  組隊  商城  會員",12,"#CBD5E1",False,"ra")
        line_text(x1+18,y1+48,"找到你的隊友，一起挑戰遊戲世界！",17,WHITE,True)
        tw=(x2-x1-52)//3
        for i,(lab,c) in enumerate((("田忌撲克","#0F766E"),("問答挑戰","#1D4ED8"),("組隊公告","#5B21B6"))):
            xx=x1+15+i*(tw+10); rr((xx,y1+82,xx+tw,y2-15),8,c)
            line_text(xx+tw/2,(y1+82+y2-15)/2,lab,14,WHITE,True,"mm")
    elif kind == "lobby":
        line_text(x1+15,y1+12,"新建房間   現有房間   進行中",14,"#CBD5E1",True)
        line_text((x1+x2)/2,y1+48,"選擇你的遊戲",18,WHITE,True,"mm")
        tw=(x2-x1-48)//3
        for i,lab in enumerate(("田忌撲克","限時問答","更多遊戲")):
            xx=x1+12+i*(tw+12); rr((xx,y1+75,xx+tw,y2-12),7,DARK2,"#475569",1)
            line_text(xx+tw/2,y1+100,"◇",28,"#60A5FA",True,"mm")
            line_text(xx+tw/2,y2-29,lab,13,WHITE,True,"mm")
    elif kind == "waiting":
        line_text(x1+15,y1+13,"遊戲等待區",18,WHITE,True)
        line_text(x2-15,y1+16,"房號 54FA6780",13,"#93C5FD",False,"ra")
        rr((x1+15,y1+48,x1+120,y2-14),7,"#111827","#334155",1)
        line_text(x1+28,y1+62,"房間設定",14,WHITE,True)
        rr((x1+28,y1+91,x1+107,y1+112),4,"#1E293B")
        rr((x1+137,y1+48,x2-15,y2-14),7,"#111827","#334155",1)
        line_text(x1+150,y1+61,"玩家名單  2 / 2",14,WHITE,True)
        for i,n in enumerate(("Thomas  房主","Amy  玩家")):
            rr((x1+150,y1+87+i*33,x2-28,y1+114+i*33),5,DARK2)
            line_text(x1+160,y1+92+i*33,n,13,"#E2E8F0")
    elif kind == "game":
        line_text(x1+15,y1+12,"田忌撲克  |  限時問答",16,WHITE,True)
        rr((x1+14,y1+43,x2-14,y2-15),8,"#12352D","#2DD4BF",1)
        for i in range(5):
            xx=x1+55+i*38; rr((xx,y1+72,xx+27,y1+116),4,WHITE,"#CBD5E1",1)
        line_text((x1+x2)/2,y2-28,"選牌 → 確認 → 結果／成績",13,WHITE,True,"mm")
    elif kind == "post":
        d.rectangle((x1,y1,x2,y1+25),fill="#15233B")
        line_text(x1+10,y1+6,"組隊公告｜搜尋｜建立隊伍",12,WHITE,True)
        for i,(t,s) in enumerate((("一起來玩撲克牌！","招募中"),("問答高手挑戰！","招募中"),("週末休閒牌局","已滿"))):
            yy=y1+36+i*38; rr((x1+10,yy,x2-10,yy+31),5,WHITE,BORDER,1)
            line_text(x1+18,yy+7,t,12,TEXT,True); line_text(x2-18,yy+7,s,11,GREEN,True,"ra")
    elif kind == "detail":
        line_text(x1+12,y1+10,"一起來玩撲克牌！",16,WHITE,True)
        for i,t in enumerate(("開始  2026/09/04 20:00","目前人數  1 / 2","隊長  Thomas")):
            line_text(x1+18,y1+40+i*24,t,12,"#CBD5E1")
        for i,(lab,c) in enumerate((("我要加入",GREEN),("收藏",ORANGE),("留言",BLUE))):
            xx=x1+14+i*88; rr((xx,y2-38,xx+76,y2-13),5,c)
            line_text(xx+38,y2-25,lab,12,WHITE,True,"mm")

def card(box, step, title, subtitle, color=BLUE, fill=BLUE_SOFT, mock=None, role=None, actual=None):
    x1,y1,x2,y2=box
    rr(box, 14, fill, color, 2)
    line_text(x1+14,y1+10,f"{step}.  {title}",20,TEXT,True)
    if role: pill(x2-108,y1+8,role,color)
    if actual:
        actual_screen((x1+12,y1+43,x2-12,y2-51),actual)
    elif mock:
        mock_screen((x1+12,y1+43,x2-12,y2-51),mock)
    wrapped(x1+14,y2-39,subtitle,30,15,MUTED)

# Title
line_text(30,20,"簡易遊戲平台－完整 UI 使用者流程圖 V4（實際畫面）",42,NAVY,True)
line_text(32,75,"Game Platform · Login / Lobby / Team Recruitment / Games / Member / Admin",20,TEXT,True)

# Left legend and module rail
rr((28,115,288,555),14,WHITE,"#AFC8FF",2)
line_text(48,137,"圖例說明",23,TEXT,True)
arrow(52,184,116,184,BLUE); line_text(132,173,"主要流程",17,TEXT,True)
arrow(52,224,116,224,BLUE,True); line_text(132,213,"分支／跳轉",17,TEXT,True)
for i,(c,t) in enumerate(((BLUE,"玩家主流程"),(GREEN,"組隊／隊長流程"),(ORANGE,"其他功能"),(PURPLE,"管理員／跨模組"))):
    yy=268+i*48; rr((49,yy,108,yy+29),5,"#FFFFFF",c,2); line_text(126,yy+4,t,17,TEXT)

rr((28,578,288,1045),14,PURPLE_SOFT,"#C9B7FF",2)
line_text(48,601,"相關模組串接",22,PURPLE,True)
mods=[("會員模組","登入／註冊\n個人資料／停用"),("大廳模組","遊戲清單／建房\n等待區／房況"),("組隊模組","公告／申請／審核\n收藏／通知／留言"),("聊天模組","大廳聊天\n房間聊天不中斷"),("遊戲模組","田忌撲克三輪\n問答 20 題／排行"),("管理模組","Dashboard／Users\nOperation Logs")]
for i,(name,desc) in enumerate(mods):
    yy=645+i*63; rr((45,yy,270,yy+51),7,WHITE,"#D8CCFF",1); line_text(58,yy+6,name,16,PURPLE,True); wrapped(144,yy+6,desc,15,13,MUTED,gap=2)

# Main top flow
xs=[320,740,1160,1580,2000]; widths=[360,360,360,360,560]
top=[("1","登入／註冊","驗證 JWT，成功後返回原功能", "login","訪客","01-login.jpg"),("2","平台主畫面","共用導覽列＋iframe＋常駐聊天", "shell","共用","02-platform.jpg"),("3","遊戲大廳","選遊戲／模式／人數或加入現有房間", "lobby","玩家","03-lobby.jpg"),("4","房間等待區","同步玩家名單；房主踢人／開始", "waiting","房主/玩家",None),("5","進入遊戲","田忌撲克或限時問答，完成後回 Lobby", "game","PLAYING","05-poker.jpg")]
for i,(st,ti,su,m,r,a) in enumerate(top):
    card((xs[i],115,xs[i]+widths[i],405),st,ti,su,BLUE,BLUE_SOFT,m,r,a)
    if i<len(top)-1: arrow(xs[i]+widths[i]+7,260,xs[i+1]-9,260,BLUE)

# Board route title
line_text(320,440,"組隊公告流程（玩家申請＋隊長審核＋自動建房）",26,GREEN,True)
bxs=[320,695,1070,1445,1820,2195]; bw=335
board=[("6","公告列表","搜尋遊戲、模式、狀態、時間", "post","公開","04-board.jpg"),("7","公告詳情","加入、收藏、留言、分享", "detail","公開",None),("8","送出申請","填寫留言 → 等待審核",None,"玩家",None),("9","隊長管理","同意／拒絕、踢人、編輯公告",None,"隊長",None),("10","滿員建房","核准人數到齊即建立 Lobby 房間",None,"FULL",None),("11","等待／開始","隊長開始後全員導向遊戲",None,"同步",None)]
for i,(st,ti,su,m,r,a) in enumerate(board):
    box=(bxs[i],480,bxs[i]+bw,730)
    card(box,st,ti,su,GREEN,GREEN_SOFT,m,r,a)
    if not m:
        x1,y1,x2,y2=box
        if i==2:
            rr((x1+18,y1+62,x2-18,y1+112),7,WHITE,BORDER,1); line_text(x1+32,y1+77,"申請留言：希望一起遊玩",14,TEXT)
            rr((x2-112,y1+132,x2-18,y1+162),5,GREEN); line_text(x2-65,y1+147,"送出申請",13,WHITE,True,"mm")
        elif i==3:
            for j,n in enumerate(("Amy","Jacky")):
                yy=y1+62+j*46; rr((x1+18,yy,x2-18,yy+36),6,WHITE,BORDER,1); line_text(x1+30,yy+10,n,14,TEXT,True); line_text(x2-30,yy+10,"同意  拒絕",13,GREEN,True,"ra")
        elif i==4:
            line_text((x1+x2)/2,y1+88,"隊伍建立完成！",19,GREEN,True,"mm"); line_text((x1+x2)/2,y1+125,"Thomas ＋ Amy   2 / 2",14,TEXT,False,"mm")
        else:
            line_text((x1+x2)/2,y1+88,"等待區 → START_GAME",17,GREEN,True,"mm"); rr((x1+70,y1+120,x2-70,y1+152),6,GREEN); line_text((x1+x2)/2,y1+136,"開始遊戲",14,WHITE,True,"mm")
    if i<len(board)-1: arrow(bxs[i]+bw+5,604,bxs[i+1]-7,604,GREEN)

# Game lanes
line_text(320,765,"遊戲內 UI 流程",26,BLUE,True)
rr((320,805,1500,1110),14,BLUE_SOFT,"#AFC8FF",2)
line_text(342,826,"12. 田忌撲克（三輪制）",21,TEXT,True)
actual_screen((342,868,825,1075),"05-poker.jpg","cover")
line_text(850,875,"實際牌局畫面",16,BLUE,True)
poker_steps=[("① 選擇對戰","玩家或電腦"),("② 三輪選牌","第 1 輪 3 張；後兩輪 5 張"),("③ 同步開牌","雙方確認後公開結果"),("④ 結算","顯示勝敗並返回 Lobby")]
for i,(a,b) in enumerate(poker_steps):
    yy=910+i*39
    rr((850,yy,1475,yy+32),6,WHITE,"#BDD0F7",1)
    line_text(864,yy+7,a,14,TEXT,True)
    line_text(1100,yy+7,b,13,MUTED)

rr((1530,805,2560,1110),14,BLUE_SOFT,"#AFC8FF",2)
line_text(1552,826,"13. 限時問答挑戰",21,TEXT,True)
actual_screen((1552,868,2000,1075),"06-quiz.jpg","cover")
line_text(2025,875,"實際問答畫面",16,BLUE,True)
quiz_steps=[("① 輸入暱稱","加入限時挑戰"),("② 20 題作答","倒數與進度即時更新"),("③ 結果頁","得分、解析、排行榜"),("④ 延伸動作","再玩一次或返回 Lobby")]
for i,(a,b) in enumerate(quiz_steps):
    yy=910+i*39
    rr((2025,yy,2538,yy+32),6,WHITE,"#BDD0F7",1)
    line_text(2038,yy+7,a,14,TEXT,True)
    line_text(2225,yy+7,b,13,MUTED)

# Auxiliary modules
line_text(320,1145,"跨模組功能與角色分支",26,ORANGE,True)
aux=[("14","會員中心","查看／更新帳號、名稱、頭像、描述；登出或停用帳號",ORANGE,ORANGE_SOFT),("15","我的申請／收藏／通知","查看審核狀態、房間入口、三類通知及未讀數",ORANGE,ORANGE_SOFT),("16","大廳／房間聊天","外層頁面維持 WebSocket；進遊戲後聊天室不中斷",PURPLE,PURPLE_SOFT),("17","管理員後台","Dashboard → Users CRUD → Operation Logs",PURPLE,PURPLE_SOFT),("18","商城","目前維護中，可返回 Lobby",ORANGE,ORANGE_SOFT)]
ax=[320,775,1230,1685,2140]; aw=410
for i,(st,ti,su,c,f) in enumerate(aux):
    actual = "07-admin.jpg" if st == "17" else None
    card((ax[i],1190,ax[i]+aw,1450),st,ti,su,c,f,None,"ADMIN" if st=="17" else "功能",actual)
    x1=ax[i];
    if st=="14":
        for j,t in enumerate(("我的資料","儲存修改","登出／停用")): rr((x1+22,1260+j*42,x1+aw-22,1292+j*42),6,WHITE,BORDER,1); line_text(x1+36,1268+j*42,t,14,TEXT)
    elif st=="15":
        for j,t in enumerate(("我的申請","我的收藏","我的通知")): pill(x1+24+(j%2)*170,1264+(j//2)*42,t,ORANGE)
    elif st=="16":
        rr((x1+25,1265,x1+aw-25,1365),8,DARK,"#475569",1); line_text(x1+42,1280,"大廳聊天  |  房間聊天",14,WHITE,True); line_text(x1+42,1320,"Thomas：準備好了！",13,"#CBD5E1")
    elif st=="17":
        pass
    else:
        line_text(x1+aw/2,1305,"施工維護中",22,ORANGE,True,"mm")

# Footer legend
d.line((30,1500,2570,1500),fill=BORDER,width=2)
legend=[(BLUE,"玩家主流程"),(GREEN,"組隊／隊長流程"),(ORANGE,"其他功能"),(PURPLE,"管理員／跨模組")]
lx=420
for c,t in legend:
    rr((lx,1530,lx+28,1558),5,WHITE,c,2); line_text(lx+40,1534,t,17,TEXT); lx+=300
arrow(1740,1545,1815,1545,BLUE); line_text(1830,1534,"主流程",17,TEXT)
arrow(1990,1545,2065,1545,BLUE,True); line_text(2080,1534,"分支／跳轉",17,TEXT)
line_text(30,1603,"實際頁面截圖：Login、Chat Shell、Lobby、Board、Poker、Quiz、Admin；流程依專案 HTML／JavaScript 行為整理",15,MUTED)

im.save(OUT, "JPEG", quality=95, subsampling=0, optimize=True)
print(OUT)
