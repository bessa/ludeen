#!/usr/bin/env bash
#
# Builds the Ludeen TV app and publishes it as a GitHub release on bessa/ludeen.
#
# Run on the build machine, from the repository root:
#
#     tools/release-ludeen.sh
#
# What it does, in order, and it stops at the first thing that fails:
#
#   1. Brings this checkout up to date with origin/ludeen (fast-forward only, never discards work).
#   2. Builds the signed release APK and runs the TV and core tests.
#   3. Names it ludeen-tv-v<versionName>.apk, writes its .sha256 beside it.
#   4. Creates the release v<versionName> on GitHub and uploads both files.
#
# The app's own update check reads the newest release of bessa/ludeen and installs the
# ludeen-tv-*.apk it finds there — only after the .sha256 matches. A release without the
# checksum is refused by the app, which is why this script always uploads both.
#
# Before running: raise versionCode and versionName in app-tv/build.gradle.kts and push that.
# A release whose tag already exists is refused rather than overwritten.
#
# Needs: a GitHub token with Contents read/write on bessa/ludeen in $TOKEN_FILE, and
# keystore.properties beside this repository's root for signing.

set -euo pipefail

REPO="bessa/ludeen"
BRANCH="ludeen"
TOKEN_FILE="${TOKEN_FILE:-/root/.github_token}"
export ANDROID_HOME="${ANDROID_HOME:-/root/Android/Sdk}"

cd "$(git rev-parse --show-toplevel)"

TOKEN="$(tr -d '[:space:]' < "$TOKEN_FILE")"
AUTH=(-H "Authorization: Bearer $TOKEN" -H "Accept: application/vnd.github+json")
GIT_AUTH=(-c "http.https://github.com/.extraheader=AUTHORIZATION: basic $(printf 'x-access-token:%s' "$TOKEN" | base64 -w0)")

echo "== Syncing with origin/$BRANCH"
if [[ -n "$(git status --porcelain --untracked-files=no)" ]]; then
    echo "This checkout has uncommitted changes. Commit and push them first:" >&2
    git status --short --untracked-files=no >&2
    exit 1
fi
git "${GIT_AUTH[@]}" fetch -q origin "$BRANCH"
git checkout -q "$BRANCH"
git merge -q --ff-only "origin/$BRANCH"
echo "   at $(git log --oneline -1)"

VERSION="$(sed -n 's/^ *versionName = "\(.*\)"/\1/p' app-tv/build.gradle.kts | head -1)"
TAG="v$VERSION"
NAME="ludeen-tv-$TAG.apk"
[[ -n "$VERSION" ]] || { echo "No versionName in app-tv/build.gradle.kts" >&2; exit 1; }

if curl -sf -o /dev/null "${AUTH[@]}" "https://api.github.com/repos/$REPO/releases/tags/$TAG"; then
    echo "Release $TAG already exists on $REPO. Raise versionName (and versionCode) first." >&2
    exit 1
fi

echo "== Building and testing $VERSION"
./gradlew --console=plain -q \
    :app-tv:assembleRelease :app-tv:testDebugUnitTest \
    :core:model:test :core:network:testDebugUnitTest :core:database:testDebugUnitTest \
    :core:data:testDebugUnitTest :feature:browse:testDebugUnitTest

OUT="app-tv/build/outputs/apk/release"
cp "$OUT/app-tv-release.apk" "$OUT/$NAME"
(cd "$OUT" && sha256sum "$NAME" > "$NAME.sha256")
echo "   $OUT/$NAME ($(du -h "$OUT/$NAME" | cut -f1))"

echo "== Publishing $TAG on $REPO"
NOTES="Ludeen $VERSION for Android TV. Install ludeen-tv-$TAG.apk, or update from Settings → Updates in the app."
RELEASE_JSON="$(python3 -c 'import json,sys; print(json.dumps({"tag_name":sys.argv[1],"target_commitish":sys.argv[2],"name":"Ludeen "+sys.argv[3],"body":sys.argv[4]}))' \
    "$TAG" "$(git rev-parse HEAD)" "$VERSION" "$NOTES")"
UPLOAD_URL="$(curl -sf "${AUTH[@]}" -X POST "https://api.github.com/repos/$REPO/releases" -d "$RELEASE_JSON" \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["upload_url"].split("{")[0])')"

for FILE in "$NAME" "$NAME.sha256"; do
    TYPE="application/vnd.android.package-archive"
    [[ "$FILE" == *.sha256 ]] && TYPE="text/plain"
    curl -sf "${AUTH[@]}" -H "Content-Type: $TYPE" --data-binary @"$OUT/$FILE" \
        "$UPLOAD_URL?name=$FILE" -o /dev/null
    echo "   uploaded $FILE"
done

echo "== Done: https://github.com/$REPO/releases/tag/$TAG"
