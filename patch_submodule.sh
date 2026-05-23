#!/bin/bash

for file in external/KeePassDX/{crypto,database}/build.gradle; do
    if [ -f "$file" ]; then
        sed -i "/id 'kotlin-android'/d" "$file"
        sed -i "/apply plugin: 'kotlin-android'/d" "$file"
        sed -i '/kotlinOptions {/,/}/d' "$file"
    fi
done
