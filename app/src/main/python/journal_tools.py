import json

def summarize_entries(entries_json: str) -> str:
    try:
        entries = json.loads(entries_json)
    except Exception:
        return "No se pudo resumir el diario."

    if not entries:
        return "Todavía no hay acontecimientos destacados en la aventura."

    lines = []
    for entry in entries[:8]:
        title = (entry.get("title") or "").strip()
        summary = (entry.get("summary") or "").strip()
        if title and summary:
            lines.append(f"- {title}: {summary}")
        elif title:
            lines.append(f"- {title}")
        elif summary:
            lines.append(f"- {summary}")

    if not lines:
        return "Todavía no hay acontecimientos destacados en la aventura."

    return "Resumen reciente de la aventura:\n" + "\n".join(lines)


def make_chapter_title(entries_json: str) -> str:
    try:
        entries = json.loads(entries_json)
    except Exception:
        return "Crónicas del viaje"

    if not entries:
        return "Crónicas del viaje"

    combat_count = 0
    loot_count = 0
    story_count = 0

    for entry in entries:
        etype = (entry.get("type") or "").lower()
        if etype == "combat":
            combat_count += 1
        elif etype == "loot":
            loot_count += 1
        else:
            story_count += 1

    if combat_count >= loot_count and combat_count >= story_count:
        return "Capítulo de sangre y acero"
    if loot_count >= combat_count and loot_count >= story_count:
        return "Capítulo de botín y hallazgos"
    return "Capítulo de viaje y decisiones"