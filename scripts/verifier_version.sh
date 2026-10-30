#!/usr/bin/env bash
# Olauncher+ : verifie que la VERSION (versionName dans app/build.gradle) a la
# forme vnn[lettre]_AAAA-MM-JJ et, si un fichier est passe en argument, que son
# nom reprend exactement cette version.
set -euo pipefail
cd "$(dirname "$0")/.."
VERSION=$(sed -n 's/.*versionName "\([^"]*\)".*/\1/p' app/build.gradle | head -1)
if ! [[ "$VERSION" =~ ^v[0-9]{2}[a-z]?_[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "ERREUR : VERSION '$VERSION' ne respecte pas la forme vnn[lettre]_AAAA-MM-JJ" >&2
  exit 1
fi
for f in "$@"; do
  case "$(basename "$f")" in
    *"-$VERSION."*) ;;
    *) echo "ERREUR : '$f' ne porte pas la version $VERSION" >&2; exit 1 ;;
  esac
done
echo "$VERSION"
