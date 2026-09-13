#!/usr/bin/env bash
# Downloads compile-only dependencies needed to build copper_age_patch in CI.
# All files go into libs/ which is gitignored.
set -euo pipefail

LIBS_DIR="$(cd "$(dirname "$0")/../.." && pwd)/libs"
mkdir -p "$LIBS_DIR"

download() {
    local url="$1"
    local dest="$LIBS_DIR/$2"
    if [ ! -f "$dest" ]; then
        echo "  Downloading $2..."
        curl -fsSL -o "$dest" "$url"
    else
        echo "  Already present: $2"
    fi
}

echo "=== Downloading Minecraft 1.21.1 client jar (Mojang) ==="
download \
    "https://piston-data.mojang.com/v1/objects/30c73b1c5da787909b2f73340419fdf13b9def88/client.jar" \
    "minecraft-1.21.1.jar"

echo "=== Downloading Copper Age Backport 0.1.4 (Modrinth) ==="
download \
    "https://cdn.modrinth.com/data/a1llHwl4/versions/JEPUV1lF/copperagebackport-neoforge-1.21.1-0.1.4.jar" \
    "copperagebackport-neoforge-1.21.1-0.1.4.jar"

echo "=== Downloading Jade 15.10.6 NeoForge (Modrinth) ==="
download \
    "https://cdn.modrinth.com/data/nvQzSEkH/versions/B1MJm9Pz/Jade-1.21.1-NeoForge-15.10.6.jar" \
    "Jade-1.21.1-NeoForge-15.10.6.jar"

echo "=== Downloading Fabric Loader 0.18.4 ==="
download \
    "https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.18.4/fabric-loader-0.18.4.jar" \
    "fabric-loader-0.18.4.jar"

echo "=== Downloading Mixin 0.8.7 ==="
download \
    "https://repo1.maven.org/maven2/org/spongepowered/mixin/0.8.7/mixin-0.8.7.jar" \
    "mixin-0.8.7.jar"

echo "=== Downloading ASM 9.9 ==="
download \
    "https://repo1.maven.org/maven2/org/ow2/asm/asm/9.9/asm-9.9.jar" \
    "asm-9.9.jar"
download \
    "https://repo1.maven.org/maven2/org/ow2/asm/asm-tree/9.9/asm-tree-9.9.jar" \
    "asm-tree-9.9.jar"

echo "=== Downloading SLF4J 2.0.17 ==="
download \
    "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar" \
    "slf4j-api-2.0.17.jar"

echo "=== Downloading Guava 32.1.2 ==="
download \
    "https://repo1.maven.org/maven2/com/google/guava/guava/32.1.2-jre/guava-32.1.2-jre.jar" \
    "guava-32.1.2-jre.jar"

echo "=== Downloading Gson 2.10.1 ==="
download \
    "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar" \
    "gson-2.10.1.jar"

echo "=== Downloading Commons Lang 3.14.0 ==="
download \
    "https://repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar" \
    "commons-lang3-3.14.0.jar"

echo "=== Downloading Netty ==="
download \
    "https://repo1.maven.org/maven2/io/netty/netty-buffer/4.1.97.Final/netty-buffer-4.1.97.Final.jar" \
    "netty-buffer-4.1.97.Final.jar"
download \
    "https://repo1.maven.org/maven2/io/netty/netty-codec/4.1.97.Final/netty-codec-4.1.97.Final.jar" \
    "netty-codec-4.1.97.Final.jar"
download \
    "https://repo1.maven.org/maven2/io/netty/netty-common/4.1.97.Final/netty-common-4.1.97.Final.jar" \
    "netty-common-4.1.97.Final.jar"

echo "=== Downloading fastutil 8.5.12 ==="
download \
    "https://repo1.maven.org/maven2/it/unimi/dsi/fastutil/8.5.12/fastutil-8.5.12.jar" \
    "fastutil-8.5.12.jar"

echo "=== Downloading JOML 1.10.5 ==="
download \
    "https://repo1.maven.org/maven2/org/joml/joml/1.10.5/joml-1.10.5.jar" \
    "joml-1.10.5.jar"

echo "=== Downloading Brigadier 1.3.10 (Mojang) ==="
download \
    "https://libraries.minecraft.net/com/mojang/brigadier/1.3.10/brigadier-1.3.10.jar" \
    "brigadier-1.3.10.jar"

echo "=== Downloading Authlib 6.0.54 (Mojang) ==="
download \
    "https://libraries.minecraft.net/com/mojang/authlib/6.0.54/authlib-6.0.54.jar" \
    "authlib-6.0.54.jar"

echo "=== Downloading DataFixerUpper 8.0.16 (Mojang) ==="
download \
    "https://libraries.minecraft.net/com/mojang/datafixerupper/8.0.16/datafixerupper-8.0.16.jar" \
    "datafixerupper-8.0.16.jar"

echo "=== Downloading Mojang Logging 1.2.7 ==="
download \
    "https://libraries.minecraft.net/com/mojang/logging/1.2.7/logging-1.2.7.jar" \
    "logging-1.2.7.jar"

echo "=== Downloading NeoForge SPI 9.0.2 (compile-only stub for loader-4.0.44) ==="
download \
    "https://maven.neoforged.net/releases/net/neoforged/neoforgespi/9.0.2/neoforgespi-9.0.2.jar" \
    "loader-4.0.44.jar"

echo "=== Downloading NeoForge MergeTool API 2.0.7 ==="
download \
    "https://maven.neoforged.net/releases/net/neoforged/mergetool/mergetool-api/2.0.7/mergetool-api-2.0.7.jar" \
    "mergetool-2.0.7-api.jar"

echo ""
echo "All dependencies downloaded to libs/"
ls -lh "$LIBS_DIR/"
