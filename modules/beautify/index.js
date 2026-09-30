// ============================================================
// beautify — визуальный окрас игры (StarredHeltix JS-модуль)
// ------------------------------------------------------------
// Архитектура: index.js — ТОЛЬКО диспетчер (конфиг + единый
// onTick + /beautify). Каждая фича — отдельный файл в features/,
// экспортирует { tick, status }.
// Гарантии перфа (RULES.md): ОДИН onTick-триггер на весь модуль.
//
// ВАЖНО про require: пути всегда ОТ КОРНЯ модуля
// (require("./features/killfx.js"), require("./core/helpers.js")) —
// движок резолвит их относительно папки модуля (SHScriptEngine).
//
// Состав: только Kill-эффекты и Трейсер телепортов.
// ============================================================

var H = require("./core/helpers.js");
config.define(H.DEFAULTS, H.SCHEMA);

// Порядок tick: сначала кэш сущностей (killfx), затем остальное.
var TICKS = [
    require("./features/killfx.js"),
    require("./features/tracer.js")
];

var STATUS = TICKS;

events.onTick(function() {
    for (var i = 0; i < TICKS.length; i++) {
        var f = TICKS[i];
        if (f.tick) { try { f.tick(); } catch (e) {} }
    }
});

// ============================================================
// /beautify — статус
// ============================================================
command.register("beautify", function(args) {
    chat.display("§d[Beautify] §7визуальные фичи (настройки: /sh → Beautify):");
    for (var i = 0; i < STATUS.length; i++) {
        var f = STATUS[i];
        if (f.status) {
            try {
                var s = f.status();
                if (!s) continue;
                var lines = String(s).split("\n");
                for (var j = 0; j < lines.length; j++) {
                    if (lines[j]) chat.display(lines[j]);
                }
            } catch (e) {}
        }
    }
    chat.display("§8Kill-эффекты: harvest/divine/void/inferno/storm (см. /sh → Beautify → Тема).");
});
