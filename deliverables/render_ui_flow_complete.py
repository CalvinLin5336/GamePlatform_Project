from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

BASE = Path(__file__).resolve().parent
SCREENS = BASE / "screens"
OUT = BASE / "簡易遊戲平台_UI使用者流程圖_完整真實畫面版.jpg"

W, H = 3600, 2480
BG = "#F7F9FD"
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


def txt(x, y, value, size=24, color=TEXT, bold=False, anchor=None):
    d.text((x, y), value, font=font(size, bold), fill=color, anchor=anchor)


def arrow(x1, y1, x2, y2, color=BLUE, dashed=False):
    import math
    if dashed:
        total = max(1, int(((x2 - x1) ** 2 + (y2 - y1) ** 2) ** 0.5))
        for start in range(0, total, 20):
            end = min(total, start + 11)
            d.line((x1 + (x2-x1)*start/total, y1 + (y2-y1)*start/total,
                    x1 + (x2-x1)*end/total, y1 + (y2-y1)*end/total), fill=color, width=5)
    else:
        d.line((x1, y1, x2, y2), fill=color, width=5)
    angle = math.atan2(y2-y1, x2-x1)
    for off in (2.55, -2.55):
        d.line((x2, y2, x2 + 15*math.cos(angle+off), y2 + 15*math.sin(angle+off)), fill=color, width=5)


def pill(x, y, label, color):
    f = font(18, True)
    box = d.textbbox((0, 0), label, font=f)
    w = box[2] - box[0] + 24
    rr((x, y, x+w, y+32), 16, WHITE, color, 2)
    txt(x+w/2, y+16, label, 18, color, True, "mm")


def screenshot(box, filename):
    x1, y1, x2, y2 = map(int, box)
    src = Image.open(SCREENS / filename).convert("RGB")
    tw, th = x2-x1, y2-y1
    target_ratio = tw / th
    source_ratio = src.width / src.height
    if source_ratio > target_ratio:
        crop_w = int(src.height * target_ratio)
        left = (src.width - crop_w) // 2
        src = src.crop((left, 0, left+crop_w, src.height))
    else:
        crop_h = int(src.width / target_ratio)
        top = max(0, (src.height-crop_h)//2)
        src = src.crop((0, top, src.width, top+crop_h))
    src = src.resize((tw, th), Image.Resampling.LANCZOS)
    mask = Image.new("L", (tw, th), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, tw-1, th-1), 10, fill=255)
    im.paste(src, (x1, y1), mask)
    d.rounded_rectangle((x1, y1, x2, y2), 10, outline="#334155", width=2)


def card(x, y, number, title, subtitle, filename, color, fill, role):
    w, h = 520, 420
    rr((x, y, x+w, y+h), 15, fill, color, 2)
    txt(x+16, y+12, f"{number}. {title}", 22, TEXT, True)
    pill(x+w-105, y+9, role, color)
    screenshot((x+14, y+52, x+w-14, y+330), filename)
    txt(x+17, y+347, subtitle, 16, MUTED)
    txt(x+w-17, y+386, "實際頁面截圖", 14, color, True, "ra")


# Header
txt(28, 20, "簡易遊戲平台－完整 UI 使用者流程圖 V5（全真實畫面）", 46, NAVY, True)
txt(30, 82, "Game Platform · Login / Lobby / Team Recruitment / Games / Member / Chat / Shop / Admin", 21, TEXT, True)

# Left legend / module rail
rr((22, 132, 285, 570), 15, WHITE, "#AFC8FF", 2)
txt(42, 154, "圖例說明", 25, TEXT, True)
arrow(48, 205, 118, 205, BLUE)
txt(138, 192, "主要流程", 19, TEXT, True)
arrow(48, 250, 118, 250, BLUE, True)
txt(138, 237, "分支／跳轉", 19, TEXT, True)
for i, (c, label) in enumerate(((BLUE, "登入／大廳"), (GREEN, "組隊公告"), (ORANGE, "遊戲／其他"), (PURPLE, "跨模組／後台"))):
    yy = 300 + i*54
    rr((45, yy, 108, yy+32), 6, WHITE, c, 2)
    txt(128, yy+4, label, 18, TEXT)

rr((22, 595, 285, 1700), 15, PURPLE_SOFT, "#C9B7FF", 2)
txt(42, 620, "實際截圖範圍", 24, PURPLE, True)
modules = [
    ("會員", "登入／註冊／資料"),
    ("大廳", "遊戲選擇／建房"),
    ("等待室", "單人／組隊滿員"),
    ("組隊", "公告／申請／審核"),
    ("互動", "收藏／通知／聊天"),
    ("遊戲", "田忌撲克／問答"),
    ("其他", "商城維護頁"),
    ("管理", "Dashboard／Users／Logs"),
]
for i, (name, desc) in enumerate(modules):
    yy = 675 + i*112
    rr((42, yy, 265, yy+88), 8, WHITE, "#D8CCFF", 1)
    txt(58, yy+12, name, 18, PURPLE, True)
    txt(58, yy+47, desc, 15, MUTED)

X0, CW, GAP = 320, 520, 25
xs = [X0 + i*(CW+GAP) for i in range(6)]

rows = [
    (132, "登入、平台與大廳主流程", BLUE, [
        ("1", "登入", "輸入帳號密碼並取得 JWT", "01-login.jpg", "訪客"),
        ("2", "註冊", "建立玩家帳號與個人資料", "24-register.jpg", "訪客"),
        ("3", "平台主畫面", "共用導覽、會員狀態與聊天", "10-platform-auth.jpg", "共用"),
        ("4", "遊戲大廳", "選擇遊戲或查看現有房間", "03-lobby.jpg", "玩家"),
        ("5", "建立房間", "設定模式與遊玩人數", "19-create-room.jpg", "房主"),
        ("6", "房間等待區", "顯示房號、模式與玩家名單", "21-waiting-room-full.jpg", "房主"),
    ]),
    (650, "組隊公告：建立、申請與收藏", GREEN, [
        ("7", "公告列表", "搜尋遊戲、模式、狀態與時間", "13-board-auth.jpg", "公開"),
        ("8", "建立公告", "實際填寫完整組隊資料", "25-board-form-filled.jpg", "玩家"),
        ("9", "公告詳情", "人數、時間、說明與留言區", "26-board-detail-live.jpg", "公開"),
        ("10", "加入申請", "輸入留言後送出隊伍申請", "29-join-request-live.jpg", "申請者"),
        ("11", "我的申請", "查看待審核與歷史申請", "30-applications-live.jpg", "申請者"),
        ("12", "收藏公告", "查看已收藏的組隊公告", "27-favorites-live.jpg", "玩家"),
    ]),
    (1168, "隊長審核、自動建房與進入遊戲", GREEN, [
        ("13", "隊長審核", "同意或拒絕玩家的加入申請", "31-review-pending-live.jpg", "隊長"),
        ("14", "隊伍滿員", "核准後更新為 2/2 並建房", "32-team-full-live.jpg", "FULL"),
        ("15", "組隊等待室", "房主與隊員同步進入房間", "34-waiting-team-live.jpg", "同步"),
        ("16", "田忌撲克", "實際房號、手牌、輪次與對戰區", "35-poker-team-live.jpg", "PLAYING"),
        ("17", "限時問答", "房間內輸入暱稱並開始挑戰", "39-quiz-intro-live.jpg", "PLAYING"),
        ("18", "問答作答", "20 題進度、倒數與選項", "40-quiz-question-live.jpg", "PLAYING"),
    ]),
    (1686, "跨模組功能與管理員後台", PURPLE, [
        ("19", "我的通知", "審核、隊伍與公告事件通知", "33-notifications-live.jpg", "玩家"),
        ("20", "會員中心", "帳號、名稱、頭像與個人描述", "23-player-live.jpg", "PLAYER"),
        ("21", "即時聊天", "平台外層常駐大廳聊天室", "36-chat-live.jpg", "WebSocket"),
        ("22", "商城", "實際專案的維護中畫面", "41-shop-live.jpg", "其他"),
        ("23", "後台儀表板", "系統統計與管理快速入口", "42-admin-dashboard.jpg", "ADMIN"),
        ("24", "操作紀錄", "管理員查看後台操作 Logs", "43-admin-logs.jpg", "ADMIN"),
    ]),
]

for row_index, (heading_y, heading, color, items) in enumerate(rows):
    txt(X0, heading_y, heading, 28, color, True)
    card_y = heading_y + 46
    fill = BLUE_SOFT if color == BLUE else GREEN_SOFT if color == GREEN else PURPLE_SOFT
    for i, item in enumerate(items):
        number, title, subtitle, filename, role = item
        card(xs[i], card_y, number, title, subtitle, filename, color, fill, role)
        if i < len(items)-1:
            arrow(xs[i]+CW+5, card_y+210, xs[i+1]-7, card_y+210, color, dashed=(row_index == 3))

# Downstream connectors between the major rows.
arrow(3518, 588, 3518, 638, BLUE)
arrow(3518, 1106, 3518, 1156, GREEN)
arrow(3518, 1624, 3518, 1674, GREEN)

# Footer
d.line((24, 2218, 3576, 2218), fill=BORDER, width=2)
txt(30, 2250, "主流程：登入／註冊 → 平台 → 大廳 → 建房／組隊 → 等待室 → 遊戲", 21, NAVY, True)
txt(30, 2292, "組隊流程：建立公告 → 玩家申請 → 隊長審核 → 隊伍滿員 → 自動建房 → 開始遊戲", 21, GREEN, True)
txt(30, 2340, "全圖使用實際專案前端頁面與實際操作狀態截圖；截圖帳號為本機測試資料。", 17, MUTED)
txt(3570, 2395, "3600 × 2480 px · JPG", 16, MUTED, False, "ra")

im.save(OUT, "JPEG", quality=96, subsampling=0, optimize=True)
print(OUT)
