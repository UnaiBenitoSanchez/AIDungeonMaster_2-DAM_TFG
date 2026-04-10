import json
import re
from collections import Counter, defaultdict


def _safe_load(data):
    try:
        return json.loads(data)
    except Exception:
        return None


def _clean(text: str) -> str:
    return re.sub(r"\s+", " ", (text or "").strip())


def _norm(text: str) -> str:
    return _clean(text).lower()


def _entry_label(entry: dict) -> str:
    title = _clean(entry.get("title", ""))
    summary = _clean(entry.get("summary", ""))
    if title and summary:
        return f"{title}: {summary}"
    return title or summary or "Suceso sin nombre"


def build_repeat_group_key(entry_json: str) -> str:
    entry = _safe_load(entry_json)
    if not entry:
        return ""

    etype = _norm(entry.get("type", "story"))
    location = _norm(entry.get("locationName", ""))
    enemy = _norm(entry.get("enemyName", ""))
    title = _norm(entry.get("title", ""))

    if etype == "combat":
        return f"combat|{location}|{enemy or title}"
    if etype == "loot":
        return f"loot|{location}|{title}"
    if etype == "location":
        return f"location|{location or title}"
    return f"{etype}|{location}|{title}"


def summarize_entries(entries_json: str) -> str:
    entries = _safe_load(entries_json)
    if not entries:
        return "Todavía no hay acontecimientos destacados en la aventura."

    entries = sorted(entries, key=lambda e: e.get("timestamp", 0))
    recent = entries[-8:]

    combat = []
    loot = []
    travel = []
    quest = []
    other = []

    for e in recent:
        etype = _norm(e.get("type", "story"))
        text = _entry_label(e)

        if etype == "combat":
            combat.append(text)
        elif etype == "loot":
            loot.append(text)
        elif etype in ("location", "travel"):
            travel.append(text)
        elif etype == "quest":
            quest.append(text)
        else:
            other.append(text)

    parts = []

    if travel:
        parts.append(
            "Durante el trayecto, la compañía avanzó entre parajes inciertos. "
            + " ".join(travel[:2])
        )

    if combat:
        parts.append(
            "No faltaron los choques de acero y los peligros del camino. "
            + " ".join(combat[:3])
        )

    if loot:
        parts.append(
            "Entre las penurias también hubo hallazgos dignos de memoria. "
            + " ".join(loot[:2])
        )

    if quest or other:
        misc = (quest + other)[:3]
        parts.append(
            "Así quedaron sellados varios momentos clave de la jornada. "
            + " ".join(misc)
        )

    result = "\n\n".join(_clean(p) for p in parts if _clean(p))
    return result or "Todavía no hay acontecimientos destacados en la aventura."


def summarize_entries_by_chapter(entries_json: str) -> str:
    entries = _safe_load(entries_json)
    if not entries:
        return "Todavía no hay capítulos registrados."

    by_chapter = defaultdict(list)
    for e in entries:
        chapter = _clean(e.get("chapter", "")) or "Capítulo sin nombre"
        by_chapter[chapter].append(e)

    blocks = []
    for chapter, chapter_entries in sorted(
            by_chapter.items(),
            key=lambda item: max(x.get("timestamp", 0) for x in item[1])
    ):
        resume = summarize_entries(json.dumps(chapter_entries, ensure_ascii=False))
        blocks.append(f"{chapter}\n{resume}")

    return "\n\n".join(blocks)


def make_chapter_title(entries_json: str) -> str:
    entries = _safe_load(entries_json)
    if not entries:
        return "Crónicas del viaje"

    combat_count = 0
    loot_count = 0
    story_count = 0

    locations = Counter()
    enemies = Counter()

    for entry in entries:
        etype = _norm(entry.get("type", ""))
        if etype == "combat":
            combat_count += 1
        elif etype == "loot":
            loot_count += 1
        else:
            story_count += 1

        loc = _clean(entry.get("locationName", ""))
        enemy = _clean(entry.get("enemyName", ""))
        if loc:
            locations[loc] += 1
        if enemy:
            enemies[enemy] += 1

    main_loc = locations.most_common(1)[0][0] if locations else ""
    main_enemy = enemies.most_common(1)[0][0] if enemies else ""

    if main_enemy and combat_count >= max(loot_count, story_count):
        return f"Capítulo de sangre frente a {main_enemy}"
    if main_loc and story_count >= max(combat_count, loot_count):
        return f"Capítulo de las sendas de {main_loc}"
    if loot_count >= max(combat_count, story_count):
        return "Capítulo de botín y hallazgos"
    return "Capítulo de viaje y decisiones"


def rewrite_entry_epic(entry_json: str) -> str:
    entry = _safe_load(entry_json)
    if not entry:
        return "No se pudo reescribir la entrada."

    title = _clean(entry.get("title", "")) or "Jornada sin nombre"
    summary = _clean(entry.get("summary", ""))
    full_text = _clean(entry.get("fullText", ""))
    location = _clean(entry.get("locationName", ""))
    enemy = _clean(entry.get("enemyName", ""))

    base = full_text or summary or title

    fragments = [f"Bajo el signo de '{title}', la historia avanzó con paso solemne."]
    if location:
        fragments.append(f"Los hechos tuvieron lugar en {location}, donde el destino aguardaba entre sombras y presagios.")
    if enemy:
        fragments.append(f"Allí se alzó {enemy}, adversario de funesta presencia, dispuesto a poner a prueba el valor del aventurero.")
    fragments.append(
        f"{base} Lo ocurrido quedó grabado como una prueba más de coraje, voluntad y supervivencia."
    )

    return " ".join(_clean(x) for x in fragments if _clean(x))