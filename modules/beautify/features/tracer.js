// ============================================================
// features/tracer.js — шлейф телепортации (AOTE / AOTV)
// Условие: шлейф сыпется только когда в руках предмет с нужным
// названием (по дефолту «Аспект Энда»), см. tracerRequireAspect.
// ============================================================
var H = require("./core/helpers.js");
var cfg = H.cfg, inWorld = H.inWorld, part = H.part;

var lastPos = null;

function tick() {
    if (!inWorld()) { lastPos = null; return; }
    if (!cfg("tracerEnabled")) return;
    // Grace 1.5с: после ТП игрок мог моментально сменить аспект в руке
    var allowed = !cfg("tracerRequireAspect") || H.holdingAspect(1500);
    var p = player.getPos();
    if (lastPos && allowed) {
        var d = 0;
        try { d = math.dist3D(lastPos.x, lastPos.y, lastPos.z, p.x, p.y, p.z); } catch (e) { d = 0; }
        if (d > (cfg("tracerThreshold") || 5)) {
            var name = part(cfg("tracerParticle") || "portal");
            try {
                particles.line(name, lastPos.x, lastPos.y + 1, lastPos.z, p.x, p.y + 1, p.z, 15);
                particles.sphere(name, p.x, p.y + 0.8, p.z, 0.8, 12);
            } catch (e2) {}
        }
    }
    lastPos = p;
}

function status() {
    var s = "§7 • Шлейф телепортов: " + (cfg("tracerEnabled") ? "§aвкл" : "§cвыкл");
    if (cfg("tracerEnabled") && cfg("tracerRequireAspect")) {
        s += " §8(нужен \"" + cfg("tracerItemName") + "\" в руках)";
    }
    return s;
}

module.exports = { tick: tick, status: status };
