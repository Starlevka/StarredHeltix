// ============================================================
// core/helpers.js — общие утилиты beautify
// Пути в require всегда ОТ КОРНЯ модуля: require("./core/helpers.js")
// ============================================================
var DEFAULTS = {
    killFxEnabled: true,
    killFxTheme: "divine",
    killFxSkipPlayers: true,
    killFxSound: true,
    killFxSoundRange: 16,
    killFxSoundVolume: 0.8,
    tracerEnabled: true,
    tracerThreshold: 5,
    tracerParticle: "portal",
    tracerRequireAspect: true,
    tracerItemName: "Аспект Энда"
};

var SCHEMA = {
    killFxEnabled: { type: "switch", description: "Эффекты при убийстве мобов (энтити и display)" },
    killFxTheme: { type: "dropdown", options: ["harvest", "divine", "void", "inferno", "storm"], description: "Тема: harvest=Кровавая жатва, divine=Священное пламя, void=Пустота, inferno=Инферно, storm=Гроза" },
    killFxSkipPlayers: { type: "switch", description: "Не показывать эффект на игроках" },
    killFxSound: { type: "switch", description: "Звук руны при убийстве моба поблизости (как Meow Music Rune, но свои .ogg)" },
    killFxSoundRange: { type: "slider", min: 4, max: 32, step: 1, description: "Радиус, в котором проигрывать звук смерти" },
    killFxSoundVolume: { type: "slider", min: 0, max: 1, step: 0.05, description: "Громкость звука руны (0.0–1.0)" },
    tracerEnabled: { type: "switch", description: "Шлейф телепортации (AOTE/AOTV)" },
    tracerThreshold: { type: "slider", min: 4, max: 16, step: 1, description: "Порог скачка дистанции, блоков за тик" },
    tracerParticle: { type: "dropdown", options: ["portal", "end_rod", "flame"], description: "Частица шлейфа телепортации" },
    tracerRequireAspect: { type: "switch", description: "Шлейф только когда в руках предмет с нужным названием (Аспект Энда)" },
    tracerItemName: { type: "text", description: "Подстрока названия предмета в руках (без учёта регистра и §-кодов)" }
};

function cfg(key) { return config.get(key, DEFAULTS[key]); }
function inWorld() {
    try { return client.isInWorld() === true; } catch (e) { return false; }
}
function nowMs() { return utils.getSystemTime(); }
function part(name) { return name.indexOf(":") === -1 ? "minecraft:" + name : name; }

// ===== условие «в руках нужный предмет» =====
// Очищенное имя: без §-кодов, схлопнутые пробелы, нижний регистр
function cleanName(s) {
    try { return String(s).replace(/§./g, "").replace(/\s+/g, " ").trim().toLowerCase(); } catch (e) { return ""; }
}

// Когда последний раз аспект был в руках (для grace-периода после ТП)
var lastAspectAt = 0;

/**
 * true, если в основной руке или оффхенде предмет, в названии которого
 * есть подстрока tracerItemName. graceMs — считать условие выполненным
 * ещё N мс после того, как предмет был в руках (смена на лету после Аоте).
 */
function holdingAspect(graceMs) {
    var needle = cleanName(cfg("tracerItemName"));
    if (!needle) return true; // пустая строка — условие снято
    var held = null, off = null;
    try { held = item.getHeld(); } catch (e) {}
    try { off = item.getOffHand(); } catch (e) {}
    var main = held ? cleanName(held.getName()) : "";
    var offName = off ? cleanName(off.getName()) : "";
    if (main.indexOf(needle) !== -1 || offName.indexOf(needle) !== -1) {
        lastAspectAt = nowMs();
        return true;
    }
    return (nowMs() - lastAspectAt) <= (graceMs || 1500);
}

module.exports = {
    DEFAULTS: DEFAULTS, SCHEMA: SCHEMA,
    cfg: cfg, inWorld: inWorld, nowMs: nowMs, part: part,
    cleanName: cleanName, holdingAspect: holdingAspect
};
