#!/usr/bin/env bash
# Gera o que o Bedrock precisa para ver as texturas dos itens, via Geyser:
#
#   ./empacotar-bedrock.sh
#
# Sai em target/:
#   ItensDaniel-bedrock.mcpack           resource pack no formato Bedrock
#   itensdaniel-geyser-mappings.json     mapeamento item_model -> item Bedrock
#
# O pack Java (empacotar.sh) continua valendo para clientes Java. O Bedrock nao
# le pack Java: o Geyser registra um item Bedrock proprio por item_model nosso, e
# as texturas vem deste .mcpack.
#
# Onde cada arquivo vai, no servidor:
#   plugins/Geyser-Spigot/packs/ItensDaniel-bedrock.mcpack
#   plugins/Geyser-Spigot/custom_mappings/itensdaniel-geyser-mappings.json
# E no config do Geyser: enable-custom-content: true
set -euo pipefail

cd "$(dirname "$0")"

OUT="$PWD/target"
WORK="$OUT/bedrockpack"
MCPACK="$OUT/ItensDaniel-bedrock.mcpack"
MAPPINGS="$OUT/itensdaniel-geyser-mappings.json"

SRC_TEX="resourcepack/assets/itensdaniel/textures/item"
[[ -d "$SRC_TEX" ]] || { echo "erro: nao achei $SRC_TEX" >&2; exit 1; }

rm -rf "$WORK" "$MCPACK" "$MAPPINGS"
mkdir -p "$WORK/textures/items"

# Os UUIDs do manifest sao fixos de proposito: trocar faz o cliente tratar como
# outro pack e baixar de novo. Versao acompanha a do plugin.
VERSION="$(sed -n 's|.*<version>\(.*\)</version>.*|\1|p' pom.xml | head -1)"

python3 -I - "$WORK" "$MAPPINGS" "$VERSION" <<'PY'
import json, sys, os

work, mappings_path, version = sys.argv[1], sys.argv[2], sys.argv[3]
ver = [int(p) for p in version.split("-")[0].split(".")][:3]
while len(ver) < 3:
    ver.append(0)

# id no pack Java -> item base, nome, se e segurado na mao, categoria, protecao
ITENS = [
    ("varinha-tempestade",   "blaze_rod",           "Varinha da Tempestade", True,  "items",     0),
    ("cajado-do-vazio",      "ender_eye",           "Cajado do Vazio",       True,  "items",     0),
    ("chama-de-daniel",      "blaze_powder",        "Chama de Daniel",       False, "items",     0),
    ("coracao-de-daniel",    "totem_of_undying",    "Coracao de Daniel",     False, "items",     0),
    ("botas-do-vento",       "netherite_boots",     "Botas do Vento",        False, "equipment", 3),
    ("elmo-do-corujao",      "netherite_helmet",    "Elmo do Corujao",       False, "equipment", 3),
    ("espada-vampira",       "netherite_sword",     "Espada Vampira",        True,  "equipment", 0),
    ("peitoral-espinhoso",   "netherite_chestplate","Peitoral Espinhoso",    False, "equipment", 8),
    ("espada-do-infinito",   "netherite_sword",     "Espada do Infinito",    True,  "equipment", 0),
    ("picareta-do-infinito", "netherite_pickaxe",   "Picareta do Infinito",  True,  "equipment", 0),
]

# Identificador Bedrock nao aceita hifen com seguranca; o pack usa underscore.
def ident(i):
    return i.replace("-", "_")

manifest = {
    "format_version": 2,
    "header": {
        "name": "ItensDaniel",
        "description": "Texturas dos itens especiais do ItensDaniel",
        "uuid": "f80819d5-355c-43a6-a3e3-4081879986a7",
        "version": ver,
        "min_engine_version": [1, 21, 0],
    },
    "modules": [{
        "type": "resources",
        "uuid": "5469437e-2b14-48c4-92f7-79af315fabb7",
        "version": ver,
    }],
}
with open(os.path.join(work, "manifest.json"), "w") as f:
    json.dump(manifest, f, indent=2)

texture_data = {
    f"itensdaniel_{ident(i)}": {"textures": f"textures/items/{ident(i)}"}
    for i, *_ in ITENS
}
with open(os.path.join(work, "textures", "item_texture.json"), "w") as f:
    json.dump({
        "resource_pack_name": "itensdaniel",
        "texture_name": "atlas.items",
        "texture_data": texture_data,
    }, f, indent=2)

items = {}
for i, base, nome, handheld, categoria, protecao in ITENS:
    items.setdefault(f"minecraft:{base}", []).append({
        "type": "definition",
        "model": f"itensdaniel:{i}",
        "bedrock_identifier": f"itensdaniel:{ident(i)}",
        "display_name": nome,
        "bedrock_options": {
            "icon": f"itensdaniel_{ident(i)}",
            "allow_offhand": True,
            "display_handheld": handheld,
            "creative_category": categoria,
            "protection_value": protecao,
        },
    })
with open(mappings_path, "w") as f:
    json.dump({"format_version": 2, "items": items}, f, indent=2)

print(f"itens mapeados: {len(ITENS)} em {len(items)} itens base")
PY

# As texturas sao as mesmas do pack Java, so com underscore no nome.
for png in "$SRC_TEX"/*.png; do
    nome="$(basename "$png" .png)"
    cp "$png" "$WORK/textures/items/${nome//-/_}.png"
done

(cd "$WORK" && zip -q -r -X "$MCPACK" manifest.json textures)
rm -rf "$WORK"

echo "mcpack:   $MCPACK"
echo "mappings: $MAPPINGS"
echo
echo "No servidor:"
echo "  cp ItensDaniel-bedrock.mcpack           plugins/Geyser-Spigot/packs/"
echo "  cp itensdaniel-geyser-mappings.json     plugins/Geyser-Spigot/custom_mappings/"
echo "  # e em plugins/Geyser-Spigot/config.yml:  enable-custom-content: true"
echo "  # depois reinicie o servidor"
