/**
 * [StarredHeltix] Cookie Clicker — затягивающая мини-игра-кликер.
 *
 * Открытие: /cookieclicker
 * Сброс:    /cookieclicker reset
 *
 * Механики:
 *  - Большая печенька: клики, пульсация, крошки, всплывающие "+N"
 *  - 10 построек (курсор -> машина времени), цена x1.15 за каждую купленную
 *  - Улучшения: сила клика, множители CPS
 *  - Золотые печеньки: Удача (+15% банка), Безумие x7 на 15с, Клик-безумие x77 на 8с
 *  - Печеньки капают даже при закрытом экране, оффлайн-доход 50% (до 2 часов)
 *  - Прогресс сохраняется в data.json модуля (storage)
 */

var MODULE = "cookieclicker";

// ==================== Баланс ====================

var BUILDINGS = [
    { id: "cursor",  name: "Курсор",           icon: "minecraft:stick",            base: 15,      cps: 0.1 },
    { id: "grandma", name: "Бабушка",          icon: "minecraft:cookie",           base: 100,     cps: 1 },
    { id: "farm",    name: "Ферма",            icon: "minecraft:wheat",            base: 1100,    cps: 8 },
    { id: "mine",    name: "Шахта",            icon: "minecraft:iron_pickaxe",     base: 12000,   cps: 47 },
    { id: "factory", name: "Завод",            icon: "minecraft:piston",           base: 130000,  cps: 260 },
    { id: "bank",    name: "Банк",             icon: "minecraft:gold_ingot",       base: 1400000, cps: 1400 },
    { id: "temple",  name: "Храм",             icon: "minecraft:enchanting_table", base: 20000000, cps: 7800 },
    { id: "tower",   name: "Волшебная башня",  icon: "minecraft:blaze_rod",        base: 330000000, cps: 44000 },
    { id: "portal",  name: "Портал",           icon: "minecraft:obsidian",         base: 51000000000, cps: 260000 },
    { id: "time",    name: "Машина времени",   icon: "minecraft:clock",            base: 7100000000000, cps: 1600000 }
];

var UPGRADES = [
    { id: "click1",   name: "Крепкие пальцы",         cost: 100,      clickAdd: 1 },
    { id: "click2",   name: "Стальные мышцы",         cost: 1000,     clickAdd: 2 },
    { id: "click3",   name: "Кибер-перчатка",         cost: 15000,    clickAdd: 5 },
    { id: "clickX2",  name: "Квантовый палец",        cost: 50000,    clickMult: 2 },
    { id: "cpsX2a",   name: "Секретный рецепт",       cost: 25000,    cpsMult: 2 },
    { id: "sync",     name: "Синхронизация курсоров", cost: 5000,     cursorMult: 2 },
    { id: "cpsX2b",   name: "Промышленная магия",     cost: 1000000,  cpsMult: 2 },
    { id: "clickX2b", name: "Двойное печенье",        cost: 500000,   clickMult: 2 },
    { id: "cpsX2c",   name: "Мультивселенная выпечки", cost: 50000000, cpsMult: 2 },
    { id: "clickX3",  name: "Палец богов",            cost: 10000000, clickMult: 3 }
];

var FRENZY_MULT = 7, FRENZY_MS = 15000;
var CLICK_FRENZY_MULT = 77, CLICK_FRENZY_MS = 8000;
var GOLDEN_TTL = 13000;
var SAVE_INTERVAL_TICKS = 400; // ~20 секунд

// ==================== Состояние ====================

function freshState() {
    return {
        cookies: 0,
        totalEarned: 0,
        totalClicks: 0,
        goldenClicked: 0,
        buildings: {},
        upgrades: {},
        lastSeen: Date.now()
    };
}

var state = freshState();

// Runtime-состояние (не сохраняется)
var rt = {
    open: false,
    w: 400, h: 300,
    mx: -1, my: -1,
    scrollRight: 0, scrollLeft: 0,
    floaters: [],   // {x, y, text, color, born, life}
    crumbs: [],     // {x, y, vx, vy, born, life, color}
    golden: null,   // {x, y, born}
    nextGoldenAt: Date.now() + 30000 + Math.floor(Math.random() * 45000),
    frenzyUntil: 0,
    clickFrenzyUntil: 0,
    offline: null,  // {amount, until}
    pulse: 0,
    lastFrame: Date.now(),
    saveCounter: 0,
    resetArm: 0
};
// [chunk-1-end]

// ==================== Утилиты ====================

var UNITS = ["", "k", "m", "b", "t", "qa", "qi"];

function fmt(n) {
    if (!isFinite(n)) return "∞";
    n = Math.floor(n * 10) / 10;
    if (n < 1000) {
        if (Math.abs(n - Math.round(n)) < 0.05) return String(Math.round(n));
        return n.toFixed(1);
    }
    var i = 0, v = n;
    while (v >= 1000 && i < UNITS.length - 1) { v /= 1000; i++; }
    var s = v.toFixed(v < 10 ? 2 : (v < 100 ? 1 : 0));
    if (s.indexOf(".") >= 0) s = s.replace(/\.?0+$/, "");
    return s + UNITS[i];
}

function bCount(id) { return state.buildings[id] || 0; }

function costOf(b) {
    return Math.ceil(b.base * Math.pow(1.15, bCount(b.id)));
}

function inRect(x, y, rx, ry, rw, rh) {
    return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
}

function nowFrenzy() { return Date.now() < rt.frenzyUntil ? FRENZY_MULT : 1; }
function nowClickFrenzy() { return Date.now() < rt.clickFrenzyUntil ? CLICK_FRENZY_MULT : 1; }

function cpsMult() {
    var m = 1;
    for (var i = 0; i < UPGRADES.length; i++) {
        var u = UPGRADES[i];
        if (u.cpsMult && state.upgrades[u.id]) m *= u.cpsMult;
    }
    return m;
}

function cursorMult() {
    var m = 1;
    for (var i = 0; i < UPGRADES.length; i++) {
        var u = UPGRADES[i];
        if (u.cursorMult && state.upgrades[u.id]) m *= u.cursorMult;
    }
    return m;
}

function totalCps() {
    var sum = 0;
    var cm = cursorMult();
    for (var i = 0; i < BUILDINGS.length; i++) {
        var b = BUILDINGS[i];
        var per = b.cps;
        if (b.id === "cursor") per *= cm;
        sum += per * bCount(b.id);
    }
    return sum * cpsMult();
}

function clickPower() {
    var add = 1, mult = 1;
    for (var i = 0; i < UPGRADES.length; i++) {
        var u = UPGRADES[i];
        if (!state.upgrades[u.id]) continue;
        if (u.clickAdd) add += u.clickAdd;
        if (u.clickMult) mult *= u.clickMult;
    }
    return add * mult * nowClickFrenzy();
}

function unlockedUpgrades() {
    var list = [];
    for (var i = 0; i < UPGRADES.length; i++) {
        var u = UPGRADES[i];
        if (state.upgrades[u.id]) continue;
        if (state.totalEarned >= u.cost / 4) list.push(u);
    }
    list.sort(function (a, b) { return a.cost - b.cost; });
    return list;
}
// [chunk-2-end]

// ==================== Эффекты ====================

function addFloater(x, y, text, color, life) {
    rt.floaters.push({ x: x, y: y, text: text, color: color, born: Date.now(), life: life || 1.2 });
    if (rt.floaters.length > 40) rt.floaters.shift();
}

function addCrumbs(x, y, count) {
    for (var i = 0; i < count; i++) {
        var a = Math.random() * Math.PI * 2;
        var sp = 40 + Math.random() * 90;
        var colors = [0xFFC68642, 0xFF6B4423, 0xFFDFA860];
        rt.crumbs.push({
            x: x, y: y,
            vx: Math.cos(a) * sp, vy: Math.sin(a) * sp - 60,
            born: Date.now(), life: 0.6 + Math.random() * 0.5,
            color: colors[Math.floor(Math.random() * colors.length)]
        });
    }
    if (rt.crumbs.length > 120) rt.crumbs.splice(0, rt.crumbs.length - 120);
}

function playClick() {
    try {
        sound.play("minecraft:ui.button.click", 0.5, 0.9 + Math.random() * 0.3);
    } catch (e) { /* звук не критичен */ }
}

function playBuy() {
    try {
        sound.play("minecraft:entity.experience_orb.pickup", 0.7, 1.0 + Math.random() * 0.2);
    } catch (e) { }
}

function playGolden() {
    try {
        sound.play("minecraft:entity.player.levelup", 0.8, 1.4);
    } catch (e) { }
}

function playDeny() {
    try {
        sound.play("minecraft:block.note_block.bass", 0.4, 0.8);
    } catch (e) { }
}

// ==================== Сохранение ====================

function loadState() {
    try {
        if (storage.has("save")) {
            var raw = storage.get("save", null);
            if (raw) {
                var loaded = JSON.parse(raw);
                var fresh = freshState();
                for (var k in fresh) {
                    if (loaded[k] !== undefined) fresh[k] = loaded[k];
                }
                state = fresh;
            }
        }
        // Оффлайн-доход: 50% от CPS, максимум 2 часа
        var elapsed = Math.min((Date.now() - (state.lastSeen || Date.now())) / 1000, 7200);
        if (elapsed > 60) {
            var gain = totalCps() * elapsed * 0.5;
            if (gain >= 1) {
                state.cookies += gain;
                state.totalEarned += gain;
                rt.offline = { amount: gain, until: Date.now() + 12000 };
            }
        }
    } catch (e) {
        state = freshState();
    }
}

function save() {
    try {
        state.lastSeen = Date.now();
        storage.set("save", JSON.stringify(state));
        storage.save();
    } catch (e) { }
}

function doReset() {
    state = freshState();
    rt.floaters = [];
    rt.crumbs = [];
    rt.golden = null;
    rt.frenzyUntil = 0;
    rt.clickFrenzyUntil = 0;
    rt.scrollLeft = 0;
    rt.scrollRight = 0;
    save();
}
// [chunk-3-end]

// ==================== Игровая логика (тик) ====================

events.onTick(function () {
    var now = Date.now();

    // Пассивный доход — идёт даже с закрытым экраном
    var perTick = totalCps() * nowFrenzy() / 20;
    if (perTick > 0) {
        state.cookies += perTick;
        state.totalEarned += perTick;
    }

    // Золотая печенька: спавн и исчезновение
    if (rt.golden && now - rt.golden.born > GOLDEN_TTL) {
        rt.golden = null;
        rt.nextGoldenAt = now + 30000 + Math.floor(Math.random() * 45000);
    }
    if (!rt.golden && now >= rt.nextGoldenAt && rt.w > 420) {
        var margin = 120;
        rt.golden = {
            x: margin + Math.floor(Math.random() * Math.max(60, rt.w - margin * 2)),
            y: 40 + Math.floor(Math.random() * Math.max(60, rt.h - 90)),
            born: now
        };
        try { sound.play("minecraft:block.note_block.chime", 0.8, 1.6); } catch (e) { }
    }

    // Автосохранение
    rt.saveCounter++;
    if (rt.saveCounter >= SAVE_INTERVAL_TICKS) {
        rt.saveCounter = 0;
        save();
    }
});

// ==================== Золотая печенька ====================

function goldenEffect(x, y) {
    var now = Date.now();
    state.goldenClicked++;
    playGolden();
    var roll = Math.random();
    if (roll < 0.45) {
        // Удача: +15% банка или 10 минут CPS (что больше)
        var gain = Math.max(state.cookies * 0.15, totalCps() * 600, 13);
        state.cookies += gain;
        state.totalEarned += gain;
        addFloater(x, y, "+" + fmt(gain), 0xFFFFD700, 2.0);
        addFloater(x, y - 12, "Удача!", 0xFFFFD700, 2.0);
    } else if (roll < 0.9) {
        rt.frenzyUntil = now + FRENZY_MS;
        addFloater(x, y, "БЕЗУМИЕ x7!", 0xFFFF5555, 2.2);
    } else {
        rt.clickFrenzyUntil = now + CLICK_FRENZY_MS;
        addFloater(x, y, "КЛИК-БЕЗУМИЕ x77!", 0xFF55FFFF, 2.2);
    }
    addCrumbs(x, y, 20);
    rt.golden = null;
    rt.nextGoldenAt = now + 30000 + Math.floor(Math.random() * 45000);
}

// ==================== Действия ====================

function clickCookie(cx, cy, r) {
    var dx = rt.mx - cx, dy = rt.my - cy;
    if (dx * dx + dy * dy > r * r) return;
    var gain = clickPower();
    state.cookies += gain;
    state.totalEarned += gain;
    state.totalClicks++;
    rt.pulse = 1;
    addFloater(rt.mx - 8 + Math.random() * 16, rt.my - 14, "+" + fmt(gain), 0xFFFFFFFF, 1.0);
    addCrumbs(rt.mx, rt.my, 5);
    playClick();
}

function buyBuilding(b) {
    var cost = costOf(b);
    if (state.cookies < cost) {
        playDeny();
        return;
    }
    state.cookies -= cost;
    state.buildings[b.id] = bCount(b.id) + 1;
    playBuy();
    addFloater(rt.mx, rt.my - 10, "- " + fmt(cost), 0xFFFF8888, 0.9);
}

function buyUpgrade(u) {
    if (state.cookies < u.cost) {
        playDeny();
        return;
    }
    state.cookies -= u.cost;
    state.upgrades[u.id] = true;
    playBuy();
    addFloater(rt.mx, rt.my - 10, u.name, 0xFF55FFFF, 1.4);
}
// [chunk-4-end]

// ==================== Геометрия экрана ====================

var PANEL_W = 170;

function layout() {
    var w = rt.w, h = rt.h;
    var left = { x: 4, y: 26, w: PANEL_W, h: h - 30 };
    var right = { x: w - PANEL_W - 4, y: 26, w: PANEL_W, h: h - 30 };
    var cx = Math.floor((left.x + left.w + right.x) / 2);
    var cy = Math.floor(h / 2) + 6;
    var r = Math.max(20, Math.min(34, Math.floor((right.x - (left.x + left.w)) / 6)));
    return { left: left, right: right, cx: cx, cy: cy, r: r };
}

// ==================== Отрисовка ====================

var CHIP_OFFSETS = [[-8, -7], [7, -10], [11, 4], [-4, 10], [0, -2], [-13, 6], [4, 14], [-9, -14]];

function drawBigCookie(ctx, cx, cy, r, scale) {
    // Тень под печенькой
    ctx.drawCircle(cx, cy + r + 3, Math.floor(r * 0.8), 0x50000000, 0x00000000);
    ctx.pushTransform();
    ctx.translate(cx, cy);
    ctx.scale(scale, scale);
    ctx.translate(-cx, -cy);
    // Тело
    ctx.drawCircle(cx, cy, r, 0xFFC68642, 0xFF7A4E24);
    // Светлый блик
    ctx.drawCircle(cx - Math.floor(r / 5), cy - Math.floor(r / 5), Math.floor(r * 0.72), 0xFFD9A05B, 0xFFD9A05B);
    ctx.drawCircle(cx - Math.floor(r / 4), cy - Math.floor(r / 4), Math.floor(r * 0.45), 0xFFE8BC7D, 0xFFE8BC7D);
    // Шоколадные кусочки
    for (var i = 0; i < CHIP_OFFSETS.length; i++) {
        var ch = CHIP_OFFSETS[i];
        ctx.drawCircle(cx + ch[0], cy + ch[1], 2, 0xFF5B3A1E, 0xFF3E2712);
    }
    ctx.popTransform();
}

function drawCursors(ctx, cx, cy, r) {
    var n = Math.min(bCount("cursor"), 12);
    if (n <= 0) return;
    var t = Date.now() / 1000;
    for (var i = 0; i < n; i++) {
        var a = t * 0.9 + i * (Math.PI * 2 / n);
        var px = cx + Math.floor(Math.cos(a) * (r + 11));
        var py = cy + Math.floor(Math.sin(a) * (r + 11) * 0.85);
        ctx.drawCircle(px, py, 2, 0xFFC8C8C8, 0xFF606060);
    }
}

function drawGoldenCookie(ctx, g) {
    var now = Date.now();
    var age = now - g.born;
    // Мигание последние 4 секунды
    if (age > GOLDEN_TTL - 4000 && Math.floor(age / 200) % 2 === 0) return;
    var pulse = Math.floor(3 * Math.sin(now / 150));
    var r = 11 + pulse;
    // Свечение
    ctx.drawCircle(g.x, g.y, r + 6, 0x40FFD700, 0x00000000);
    ctx.drawCircle(g.x, g.y, r, 0xFFFFD700, 0xFFB8860B);
    ctx.drawCircle(g.x - 2, g.y - 2, r - 4, 0xFFFFF59D, 0xFFFFF59D);
    ctx.drawCircle(g.x + 3, g.y + 1, 2, 0xFFE6A817, 0xFFB8860B);
    ctx.drawCircle(g.x - 1, g.y + 4, 1, 0xFFE6A817, 0xFFB8860B);
}

function drawHeader(ctx, w) {
    ctx.drawRect(0, 0, w, 22, 0x99000000);
    ctx.drawHLine(0, w, 22, 0xFF8A5A2B);
    ctx.drawItem("minecraft:cookie", 4, 3);
    ctx.drawString("§6§lCOOKIE CLICKER", 24, 7, 0xFFFFFFFF);
    ctx.drawCenteredString("§7ESC — выход §8| §7/cookieclicker reset — сброс", Math.floor(w / 2) + 60, 7, 0xFFFFFFFF);
    ctx.drawCenteredString("§8v1.0.0", w - 28, 7, 0xFFFFFFFF);
}
// [chunk-5-end]

var dtGlobal = 0.016;

function drawCenter(ctx, L, dt) {
    var cx = L.cx, cy = L.cy, r = L.r;
    var cps = totalCps() * nowFrenzy();
    var cp = clickPower();

    // Счётчик печенек над печенькой
    ctx.drawCenteredString("§6§l" + fmt(state.cookies), cx, cy - r - 34, 0xFFFFFFFF);
    ctx.drawCenteredString("§a" + fmt(cps) + "/сек  §8•  §e+" + fmt(cp) + " за клик", cx, cy - r - 20, 0xFFFFFFFF);

    // Пульс + наведение
    rt.pulse = Math.max(0, rt.pulse - dt * 4);
    var scale = 1 + 0.15 * rt.pulse;
    var dx = rt.mx - cx, dy = rt.my - cy;
    if (dx * dx + dy * dy <= r * r) scale += 0.04;

    drawCursors(ctx, cx, cy, r);
    drawBigCookie(ctx, cx, cy, r, scale);
}

function drawEffects(ctx) {
    var now = Date.now();
    // Крошки
    for (var i = rt.crumbs.length - 1; i >= 0; i--) {
        var c = rt.crumbs[i];
        var age = (now - c.born) / 1000;
        if (age > c.life) { rt.crumbs.splice(i, 1); continue; }
        c.x += c.vx * dtGlobal;
        c.y += c.vy * dtGlobal;
        c.vy += 220 * dtGlobal;
        ctx.drawRect(Math.floor(c.x), Math.floor(c.y), 2, 2, c.color);
    }
    // Всплывающие тексты
    for (var j = rt.floaters.length - 1; j >= 0; j--) {
        var f = rt.floaters[j];
        var a = (now - f.born) / 1000;
        if (a > f.life) { rt.floaters.splice(j, 1); continue; }
        var alpha = Math.max(0, 1 - a / f.life);
        var col = (Math.floor(alpha * 255) << 24) | (f.color & 0xFFFFFF);
        var fy = Math.floor(f.y - a * 28);
        ctx.drawCenteredString(f.text, Math.floor(f.x), fy, col);
    }
}

function drawBanner(ctx, w) {
    var now = Date.now();
    var L = layout();
    var text = null, bgColor = 0, until = 0, totalMs = 0;
    if (now < rt.clickFrenzyUntil) {
        text = "§b§lКЛИК-БЕЗУМИЕ x77 — " + Math.ceil((rt.clickFrenzyUntil - now) / 1000) + "с";
        bgColor = 0xB0550066; until = rt.clickFrenzyUntil; totalMs = CLICK_FRENZY_MS;
    } else if (now < rt.frenzyUntil) {
        text = "§6§lБЕЗУМИЕ x7 — " + Math.ceil((rt.frenzyUntil - now) / 1000) + "с";
        bgColor = 0xB0664400; until = rt.frenzyUntil; totalMs = FRENZY_MS;
    }
    if (text) {
        ctx.drawRect(Math.floor(w / 2) - 90, 26, 180, 16, bgColor);
        ctx.drawBorder(Math.floor(w / 2) - 90, 26, 180, 16, 0xFFFFD700);
        ctx.drawCenteredString(text, Math.floor(w / 2), 30, 0xFFFFFFFF);
        var remain = Math.max(0, Math.min(1, (until - now) / totalMs));
        ctx.drawRect(Math.floor(w / 2) - 90, 40, Math.floor(180 * remain), 2, 0xFFFFD700);
    }
    // Баннер оффлайн-дохода
    if (rt.offline && now < rt.offline.until) {
        var oa = Math.max(0, Math.min(1, (rt.offline.until - now) / 3000));
        var ocol = (Math.floor(oa * 255) << 24) | 0xFFFFD700;
        ctx.drawCenteredString("§eПока тебя не было испечено: §6§l+" + fmt(rt.offline.amount) + " §eпеченек!",
            Math.floor(w / 2), L.cy - L.r - 52, ocol);
    }
}
// [chunk-6-end]

// ==================== Панели ====================

var ROW_H = 30, UP_ROW_H = 24;

function drawRightPanel(ctx, L) {
    var p = L.right;
    ctx.drawRect(p.x, p.y, p.w, p.h, 0x88000000);
    ctx.drawBorder(p.x, p.y, p.w, p.h, 0xFF8A5A2B);
    ctx.drawRect(p.x, p.y, p.w, 13, 0x66000000);
    ctx.drawCenteredString("§6§lПЕКАРНЯ", p.x + p.w / 2, p.y + 3, 0xFFFFFFFF);

    var listY = p.y + 16;
    var listH = p.y + p.h - listY;
    var maxScroll = Math.max(0, BUILDINGS.length * ROW_H - listH);
    rt.scrollRight = Math.max(0, Math.min(rt.scrollRight, maxScroll));

    ctx.pushClip(p.x + 1, listY, p.w - 2, listH);
    for (var i = 0; i < BUILDINGS.length; i++) {
        var b = BUILDINGS[i];
        var y = listY + i * ROW_H - rt.scrollRight;
        if (y + ROW_H < listY || y > listY + listH) continue;
        var cost = costOf(b);
        var afford = state.cookies >= cost;
        var hovered = inRect(rt.mx, rt.my, p.x + 1, y, p.w - 2, ROW_H);

        if (hovered) ctx.drawRect(p.x + 1, y, p.w - 2, ROW_H, 0x44FFD700);
        ctx.drawHLine(p.x + 1, p.x + p.w - 1, y + ROW_H, 0x338A5A2B);

        ctx.drawItem(b.icon, p.x + 5, y + 7);
        var nameCol = afford ? "§f" : "§7";
        ctx.drawString(nameCol + "§l" + b.name, p.x + 26, y + 4, 0xFFFFFFFF);
        ctx.drawString("§a+" + fmt(b.cps * (b.id === "cursor" ? cursorMult() : 1) * cpsMult()) + "/сек", p.x + 26, y + 14, 0xFFFFFFFF);
        ctx.drawString((afford ? "§e" : "§c") + fmt(cost), p.x + 26, y + 22, 0xFFFFFFFF);
        var owned = bCount(b.id);
        if (owned > 0) {
            ctx.drawCenteredString("§6§l" + owned, p.x + p.w - 14, y + 11, 0xFFFFFFFF);
        }
    }
    ctx.popClip();

    if (maxScroll > 0) {
        // Мини-скроллбар
        var barH = Math.max(10, Math.floor(listH * listH / (BUILDINGS.length * ROW_H)));
        var barY = listY + Math.floor((listH - barH) * rt.scrollRight / maxScroll);
        ctx.drawRect(p.x + p.w - 3, barY, 2, barH, 0x66FFD700);
    }
}
// [chunk-7-end]

function drawLeftPanel(ctx, L) {
    var p = L.left;
    ctx.drawRect(p.x, p.y, p.w, p.h, 0x88000000);
    ctx.drawBorder(p.x, p.y, p.w, p.h, 0xFF8A5A2B);

    // --- Статистика ---
    ctx.drawCenteredString("§6§lСТАТИСТИКА", p.x + p.w / 2, p.y + 4, 0xFFFFFFFF);
    var sy = p.y + 17;
    var cps = totalCps() * nowFrenzy();
    ctx.drawString("§7Испечено: §f" + fmt(state.cookies), p.x + 6, sy, 0xFFFFFFFF);
    ctx.drawString("§7В секунду: §a" + fmt(cps), p.x + 6, sy + 10, 0xFFFFFFFF);
    ctx.drawString("§7За клик: §e" + fmt(clickPower()), p.x + 6, sy + 20, 0xFFFFFFFF);
    ctx.drawString("§7Всего кликов: §f" + fmt(state.totalClicks), p.x + 6, sy + 30, 0xFFFFFFFF);
    ctx.drawString("§7Всего испечено: §f" + fmt(state.totalEarned), p.x + 6, sy + 40, 0xFFFFFFFF);
    ctx.drawString("§7Золотых поймано: §6" + fmt(state.goldenClicked), p.x + 6, sy + 50, 0xFFFFFFFF);

    var upHeaderY = sy + 62;
    ctx.drawCenteredString("§b§lУЛУЧШЕНИЯ", p.x + p.w / 2, upHeaderY, 0xFFFFFFFF);

    // --- Улучшения (со скроллом) ---
    var listY = upHeaderY + 13;
    var resetH = 16;
    var listH = p.y + p.h - resetH - 4 - listY;
    var ups = unlockedUpgrades();
    var maxScroll = Math.max(0, ups.length * UP_ROW_H - listH);
    rt.scrollLeft = Math.max(0, Math.min(rt.scrollLeft, maxScroll));

    ctx.pushClip(p.x + 1, listY, p.w - 2, listH);
    for (var i = 0; i < ups.length; i++) {
        var u = ups[i];
        var y = listY + i * UP_ROW_H - rt.scrollLeft;
        if (y + UP_ROW_H < listY || y > listY + listH) continue;
        var afford = state.cookies >= u.cost;
        var hovered = inRect(rt.mx, rt.my, p.x + 1, y, p.w - 2, UP_ROW_H);

        if (hovered && afford) ctx.drawRect(p.x + 1, y, p.w - 2, UP_ROW_H, 0x4455FFFF);
        else if (hovered) ctx.drawRect(p.x + 1, y, p.w - 2, UP_ROW_H, 0x33FF5555);
        ctx.drawHLine(p.x + 1, p.x + p.w - 1, y + UP_ROW_H - 1, 0x33555555);

        var desc = u.clickAdd ? ("§e+" + u.clickAdd + " за клик")
            : u.clickMult ? ("§ex" + u.clickMult + " к клику")
            : u.cpsMult ? ("§ex" + u.cpsMult + " к CPS")
            : "§ex" + u.cursorMult + " к курсорам";
        ctx.drawString((afford ? "§b" : "§8") + "§l" + u.name, p.x + 6, y + 3, 0xFFFFFFFF);
        ctx.drawString(desc + "  §8•  " + (afford ? "§a" : "§c") + fmt(u.cost), p.x + 6, y + 13, 0xFFFFFFFF);
    }
    ctx.popClip();

    if (maxScroll > 0) {
        var barH = Math.max(8, Math.floor(listH * listH / (ups.length * UP_ROW_H)));
        var barY = listY + Math.floor((listH - barH) * rt.scrollLeft / maxScroll);
        ctx.drawRect(p.x + p.w - 3, barY, 2, barH, 0x665588FF);
    }

    // --- Кнопка сброса (двухшаговое подтверждение) ---
    var bx = p.x + 4, by = p.y + p.h - resetH - 3, bw = p.w - 8;
    var armed = (Date.now() - rt.resetArm) < 3000;
    var hov = inRect(rt.mx, rt.my, bx, by, bw, resetH);
    ctx.drawRect(bx, by, bw, resetH, hov ? 0x66FF5555 : 0x44000000);
    ctx.drawBorder(bx, by, bw, resetH, armed ? 0xFFFF5555 : 0xFF666666);
    ctx.drawCenteredString(armed ? "§c§lТОЧНО СБРОСИТЬ?!" : "§7Сбросить прогресс", bx + bw / 2, by + 4, 0xFFFFFFFF);
}
// [chunk-8-end]

// ==================== Главный рендер ====================

function renderFrame(ctx, mx, my) {
    var now = Date.now();
    var dt = Math.min((now - rt.lastFrame) / 1000, 0.05);
    rt.lastFrame = now;
    dtGlobal = dt;
    rt.mx = mx; rt.my = my;
    rt.w = ctx.screenWidth;
    rt.h = ctx.screenHeight;

    var L = layout();

    // Затемнение центральной зоны (между панелями)
    ctx.drawRect(L.left.x + L.left.w + 2, 26, L.right.x - (L.left.x + L.left.w) - 4, rt.h - 30, 0x55000000);

    drawHeader(ctx, rt.w);
    drawLeftPanel(ctx, L);
    drawRightPanel(ctx, L);
    drawCenter(ctx, L, dt);
    drawBanner(ctx, rt.w);

    // Золотая печенька — поверх всего
    if (rt.golden) drawGoldenCookie(ctx, rt.golden);

    drawEffects(ctx);
}

// ==================== Обработка ввода ====================

function handleClick(x, y, button) {
    if (button !== 0) return true;
    rt.mx = x; rt.my = y;
    var L = layout();

    // 1. Золотая печенька (радиус 14)
    if (rt.golden) {
        var gdx = x - rt.golden.x, gdy = y - rt.golden.y;
        if (gdx * gdx + gdy * gdy <= 14 * 14) {
            goldenEffect(rt.golden.x, rt.golden.y);
            return true;
        }
    }

    // 2. Кнопка сброса
    var resetH = 16;
    var bx = L.left.x + 4, by = L.left.y + L.left.h - resetH - 3, bw = L.left.w - 8;
    if (inRect(x, y, bx, by, bw, resetH)) {
        if ((Date.now() - rt.resetArm) < 3000) {
            rt.resetArm = 0;
            doReset();
            playBuy();
        } else {
            rt.resetArm = Date.now();
            playClick();
        }
        return true;
    }

    // 3. Улучшения
    var ups = unlockedUpgrades();
    var listY = L.left.y + 17 + 62 + 13;
    for (var i = 0; i < ups.length; i++) {
        var uy = listY + i * UP_ROW_H - rt.scrollLeft;
        if (inRect(x, y, L.left.x + 1, uy, L.left.w - 2, UP_ROW_H)) {
            buyUpgrade(ups[i]);
            return true;
        }
    }

    // 4. Постройки
    var rListY = L.right.y + 16;
    for (var j = 0; j < BUILDINGS.length; j++) {
        var byy = rListY + j * ROW_H - rt.scrollRight;
        if (inRect(x, y, L.right.x + 1, byy, L.right.w - 2, ROW_H)) {
            buyBuilding(BUILDINGS[j]);
            return true;
        }
    }

    // 5. Большая печенька
    clickCookie(L.cx, L.cy, L.r);
    return true;
}

function handleScroll(x, y, delta) {
    var L = layout();
    var d = delta * 20;
    if (inRect(x, y, L.right.x, L.right.y, L.right.w, L.right.h)) {
        rt.scrollRight = Math.max(0, rt.scrollRight - d);
        return true;
    }
    if (inRect(x, y, L.left.x, L.left.y, L.left.w, L.left.h)) {
        rt.scrollLeft = Math.max(0, rt.scrollLeft - d);
        return true;
    }
    return false;
}

// ==================== Экран и команда ====================

function openGame() {
    if (rt.open) return;
    rt.open = true;
    rt.lastFrame = Date.now();
    gui.openScreen({
        title: "Cookie Clicker",
        pausesGame: false,
        render: renderFrame,
        onClick: handleClick,
        onScroll: handleScroll,
        onClose: function () {
            rt.open = false;
            rt.resetArm = 0;
            save();
        }
    });
}

command.register("cookieclicker", function (args) {
    if (args && args.length > 0 && String(args[0]).toLowerCase() === "reset") {
        doReset();
        chat.display("§6[Cookie Clicker] §fПрогресс сброшен.");
        return;
    }
    openGame();
});

// ==================== Инициализация ====================

loadState();
console.log("[Cookie Clicker] Модуль загружен. Открыть: /cookieclicker");
