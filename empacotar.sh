#!/usr/bin/env bash
# Empacota resourcepack/ num zip pronto para o cliente e imprime o SHA-1 do arquivo.
#
#   ./empacotar.sh
#
# O zip sai em target/ItensDaniel-resourcepack.zip com o pack.mcmeta na raiz, que e como o
# Minecraft espera. Use o SHA-1 no server.properties (resource-pack-sha1) para o cliente saber
# quando o pack mudou; sem ele, o jogador pode ficar com a versao antiga em cache.
set -euo pipefail

cd "$(dirname "$0")"

ZIP="$PWD/target/ItensDaniel-resourcepack.zip"
mkdir -p target
rm -f "$ZIP"

(cd resourcepack && zip -q -r -X "$ZIP" pack.mcmeta assets)

echo "zip: $ZIP"
echo "sha1: $(shasum -a 1 "$ZIP" | cut -d' ' -f1)"
echo
echo "server.properties:"
echo "  resource-pack=<url-do-zip>"
echo "  resource-pack-sha1=$(shasum -a 1 "$ZIP" | cut -d' ' -f1)"
echo "  require-resource-pack=false"
