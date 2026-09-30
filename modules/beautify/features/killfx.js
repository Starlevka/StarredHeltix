// ============================================================
// features/killfx.js — эффекты при убийстве мобов
// Мобы Heltix: настоящие энтити и display-сущности (тайтлы/хелсбары).
// Позиции берём из собственного кэша (onEntityDeath не отдаёт позицию).
//
// ВАЖНО про particles-API: движок спавнит ТОЛЬКО SimpleParticleType
// (см. JSParticles.resolve). Пыль/ноты/блок/крит-типы с доп. данными
// вернут null и будут спамить в лог. Все частицы ниже — «простые».
// ============================================================
var H = require("./core/helpers.js");
var cfg = H.cfg, inWorld = H.inWorld, nowMs = H.nowMs;

// 5 тем. Поля:
//   flash  — частица ударного кольца (step 0)
//   ring   — тип самого кольца-ударной волны
//   spiral  — орбитальные частицы (спираль)
//   column — восходящий столб
//   burst  — финальный всплеск (последний шаг)
//   dur    — длительность в тиках; rise — скорость подъёма;
//   spin   — угловая скорость спирали; baseR/rGrow — стартовый радиус/прирост
//           (rGrow<0 → «схлопывание» — имплозия).
var THEMES = {
    // Кровавая жатва — алые споры, падающее сердце, сгустки урона.
    harvest: { flash: "minecraft:damage_indicator", ring: "minecraft:crimson_spore", spiral: "minecraft:crimson_spore",
               column: "minecraft:crimson_spore", burst: "minecraft:heart", dur: 14, rise: 0.20, spin: 0.70, baseR: 0.50, rGrow: 0.13 },
    // Священное пламя — белые end_rod-орбы, золотое пламя, изумрудные искры доброты.
    divine:  { flash: "minecraft:totem_of_undying", ring: "minecraft:end_rod", spiral: "minecraft:end_rod",
               column: "minecraft:flame", burst: "minecraft:happy_villager", dur: 16, rise: 0.24, spin: 0.55, baseR: 0.60, rGrow: 0.10 },
    // Пустота — портал-воронка со сжатием (implosion) и тёмным выхлопом.
    void:    { flash: "minecraft:large_smoke", ring: "minecraft:portal", spiral: "minecraft:portal",
               column: "minecraft:reverse_portal", burst: "minecraft:poof", dur: 15, rise: 0.10, spin: 0.95, baseR: 1.20, rGrow: -0.05 },
    // ИНФЕРНО — лава по орбите, дым, синее soul-пламя в столбе, чёрный дым-всплеск.
    inferno: { flash: "minecraft:flame", ring: "minecraft:flame", spiral: "minecraft:lava",
               column: "minecraft:soul_flame", burst: "minecraft:large_smoke", dur: 15, rise: 0.26, spin: 0.60, baseR: 0.70, rGrow: 0.12 },
    // ГРОЗА — разрядная волна sonic_boom, электрические искры, облако-вспышка.
    storm:   { flash: "minecraft:sonic_boom", ring: "minecraft:electric_spark", spiral: "minecraft:electric_spark",
               column: "minecraft:electric_spark", burst: "minecraft:cloud", dur: 13, rise: 0.30, spin: 1.10, baseR: 0.90, rGrow: 0.09 }
};

var entityCache = {}; // uuid -> {x, y, z, t}
var killFx = [];
var recentKills = []; // {x, y, z, t} — дедуп и частиц, и звука (моб роняет 1-3 display-сущности)
var cacheTick = 0;

// Звуки руны из assets модуля (через sound.playFile — читает из папки модуля в песочнице).
var KILL_SOUNDS = ["assets/rune_01.ogg", "assets/rune_02.ogg", "assets/rune_03.ogg"];

// Один и тот же моб на ~500 мс/2.25 блока — один эффект и один звук.
function isDuplicate(x, y, z, t) {
    for (var i = 0; i < recentKills.length; i++) {
        var r = recentKills[i];
        var dx = r.x - x, dy = r.y - y, dz = r.z - z;
        if (dx * dx + dy * dy + dz * dz < 2.25 && t - r.t < 500) return true;
    }
    return false;
}

events.onEntityDeath(function(name, typeId, uuid) {
    var visual = cfg("killFxEnabled");
    var snd = cfg("killFxSound");
    if (!visual && !snd) return;
    if (cfg("killFxSkipPlayers") && typeId === "minecraft:player") return;
    var p = entityCache[uuid];
    if (!p) return;

    var t = nowMs();
    if (isDuplicate(p.x, p.y, p.z, t)) return;
    if (recentKills.length > 16) recentKills.shift();
    recentKills.push({ x: p.x, y: p.y, z: p.z, t: t });

    if (visual) {
        // Жёсткий потолок живых эффектов — держим FPS при зачистке роя мобов.
        if (killFx.length > 8) killFx.shift();
        killFx.push({ x: p.x, y: p.y, z: p.z, step: 0 });
    }
    if (snd) playKillSound(p);
});

function playKillSound(p) {
    try {
        var dx = p.x - player.getX(), dy = p.y - player.getY(), dz = p.z - player.getZ();
        var range = cfg("killFxSoundRange") || 16;
        if (dx * dx + dy * dy + dz * dz > range * range) return;
        var vol = cfg("killFxSoundVolume");
        if (vol === null || vol === undefined) vol = 0.8;
        var file = KILL_SOUNDS[Math.floor(Math.random() * KILL_SOUNDS.length)];
        // Небольшой разброс тона, как у Meow Music Rune (0.85..1.15).
        var pitch = 0.85 + Math.random() * 0.3;
        var handle = sound.playFile(file, { volume: vol, pitch: pitch, x: p.x, y: p.y + 0.8, z: p.z, category: "players" });
        // Освободить хэндл в реестре движка после проигрывания (ogg короткие, ~1-2с).
        if (handle) setTimeout(function () { try { sound.stopFile(handle); } catch (e) {} }, 2500);
    } catch (e) {}
}

function tick() {
    if (!inWorld()) { entityCache = {}; killFx = []; recentKills = []; return; }
    cacheTick++;
    // Обновляем позиции сущностей каждые 10 тиков (48 блоков радиус).
    if (cacheTick % 10 === 0) {
        var t = nowMs();
        try {
            var es = world.getEntitiesInRadius(player.getX(), player.getY(), player.getZ(), 48);
            for (var i = 0; i < es.length; i++) {
                var e = es[i];
                entityCache[e.getUUID()] = { x: e.getX(), y: e.getY(), z: e.getZ(), t: t };
            }
        } catch (e2) {}
        for (var k in entityCache) {
            if (t - entityCache[k].t > 60000) delete entityCache[k];
        }
    }
    processFx();
}

function safe(fn) { try { fn(); } catch (e) {} }

function processFx() {
    if (!killFx.length) return;
    var theme = THEMES[cfg("killFxTheme")] || THEMES.divine;
    var dur = theme.dur;
    for (var i = killFx.length - 1; i >= 0; i--) {
        var fx = killFx[i];
        var step = fx.step;

        if (step === 0) {
            // Ударное кольцо + короткий столб в момент смерти.
            safe(function () { particles.sphere(theme.ring, fx.x, fx.y + 0.8, fx.z, theme.baseR + 0.3, 18); });
            safe(function () { particles.spawn(theme.flash, fx.x, fx.y + 0.8, fx.z, 0, 0.05, 0, 6); });
            safe(function () { particles.cylinder(theme.column, fx.x, fx.y + 0.2, fx.z, 0.35, 1.0, 8); });
        }

        // Спираль: 3 луча, радиус растёт (или сжимается при rGrow<0), поднимается вверх.
        var progress = step / dur;
        var radius = theme.baseR + theme.rGrow * step;
        if (radius < 0.05) radius = 0.05;
        var y = fx.y + 0.3 + step * theme.rise;
        for (var k = 0; k < 3; k++) {
            var ang = step * theme.spin + k * 2.094; // 120° между лучами
            (function (a, r, yy) {
                safe(function () {
                    particles.spawn(theme.spiral, fx.x + Math.cos(a) * r, yy, fx.z + Math.sin(a) * r,
                        0, 0.02, 0, 1);
                });
            })(ang, radius, y);
        }

        // Точечные частицы вдоль «оси» столба, пока эффект не погас.
        if (step % 2 === 0) {
            (function (yy) {
                safe(function () {
                    particles.spawn(theme.column, fx.x, yy + 0.15, fx.z, 0, 0.06, 0, 2);
                });
            })(y);
        }

        // Финальный всплеск на последнем шаге.
        if (step >= dur) {
            safe(function () { particles.sphere(theme.burst, fx.x, fx.y + 0.8, fx.z, 1.0, 20); });
            killFx.splice(i, 1);
            continue;
        }
        fx.step = step + 1;
    }
}

function status() {
    var name = cfg("killFxTheme") || "divine";
    var label = ({
        harvest: "Кровавая жатва", divine: "Священное пламя", void: "Пустота",
        inferno: "Инферно", storm: "Гроза"
    })[name] || name;
    var vis = cfg("killFxEnabled") ? "§aвкл §8(" + label + ")" : "§cвыкл";
    var snd = cfg("killFxSound") ? "§aвкл §8(" + (cfg("killFxSoundRange") || 16) + " бл.)" : "§cвыкл";
    return "§7 • Kill-эффекты: " + vis + "\n§7 • Звук руны при килле: " + snd;
}

module.exports = { tick: tick, status: status };
