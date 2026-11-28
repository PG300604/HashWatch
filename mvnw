#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------
set -e

WRAPPER_DIR="$(cd "$(dirname "$0")" && pwd)"
PROPS_FILE="$WRAPPER_DIR/.mvn/wrapper/maven-wrapper.properties"
DIST_URL="$(grep '^distributionUrl=' "$PROPS_FILE" | cut -d'=' -f2- | tr -d '\r')"
ZIP_NAME="$(basename "$DIST_URL")"
BASE_NAME="$(echo "$ZIP_NAME" | sed 's/-bin\.zip$//')"
M2_WRAPPER_DIR="$HOME/.m2/wrapper/dists/$BASE_NAME"
MVN_CMD="$M2_WRAPPER_DIR/$BASE_NAME/bin/mvn"

# 1. If Maven is already downloaded and executable in wrapper cache, execute it
if [ -x "$MVN_CMD" ]; then
    exec "$MVN_CMD" "$@"
fi

# 2. If system Maven is available in PATH, prefer it to prevent HTTP 429 rate limits on CI/shared IPs
if command -v mvn >/dev/null 2>&1; then
    exec mvn "$@"
fi

# 3. Fallback: Download Maven Wrapper with mirror redundancy and retry
mkdir -p "$M2_WRAPPER_DIR"
MIRRORS="$DIST_URL https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/$ZIP_NAME https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/$ZIP_NAME"

DOWNLOAD_SUCCESS=0
for URL in $MIRRORS; do
    echo "[Maven Wrapper] Attempting download from $URL ..."
    if curl --retry 3 --retry-delay 2 -fsSL "$URL" -o "$M2_WRAPPER_DIR/$ZIP_NAME" 2>/dev/null; then
        DOWNLOAD_SUCCESS=1
        break
    else
        echo "[Maven Wrapper] Download failed or rate-limited from $URL, trying next mirror..."
    fi
done

if [ "$DOWNLOAD_SUCCESS" -eq 1 ] && [ -f "$M2_WRAPPER_DIR/$ZIP_NAME" ]; then
    unzip -q -o "$M2_WRAPPER_DIR/$ZIP_NAME" -d "$M2_WRAPPER_DIR"
    rm -f "$M2_WRAPPER_DIR/$ZIP_NAME"
    chmod +x "$MVN_CMD"
    exec "$MVN_CMD" "$@"
fi

echo "[Maven Wrapper] Error: Unable to download Maven from any mirror and system 'mvn' not found." >&2
exit 1
