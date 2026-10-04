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

if [ ! -x "$MVN_CMD" ]; then
    echo "[Maven Wrapper] Downloading $DIST_URL ..."
    mkdir -p "$M2_WRAPPER_DIR"
    curl -fsSL "$DIST_URL" -o "$M2_WRAPPER_DIR/$ZIP_NAME"
    unzip -q -o "$M2_WRAPPER_DIR/$ZIP_NAME" -d "$M2_WRAPPER_DIR"
    rm -f "$M2_WRAPPER_DIR/$ZIP_NAME"
    chmod +x "$MVN_CMD"
fi

exec "$MVN_CMD" "$@"
