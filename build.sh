#!/bin/bash
# ============================================
# Pacman AI Project - Build Script (Unix/Mac)
# ============================================

echo ""
echo "========================================"
echo "   Building Pacman AI Project"
echo "========================================"
echo ""

# Create directories
echo "[1/5] Creating directories..."
mkdir -p dist
mkdir -p build/classes

# Clean previous build
echo "[2/5] Cleaning previous build..."
rm -rf build/classes/*

# Compile all Java files
echo "[3/5] Compiling Java files..."
javac -cp "src:libs/*" -d build/classes src/client/*.java src/server/*.java
if [ $? -ne 0 ]; then
    echo "ERROR: Compilation failed!"
    exit 1
fi

# Copy resources to build directory
echo "[4/5] Copying resources..."
mkdir -p build/classes/resources
cp -f resources/*.png build/classes/resources/ 2>/dev/null || true
mkdir -p build/classes/resources/sounds
cp -f resources/sounds/* build/classes/resources/sounds/ 2>/dev/null || true

# Create JAR file
echo "[5/5] Creating JAR file..."
cd build/classes
jar cfm ../../dist/PacmanAI.jar ../../MANIFEST.MF *
cd ../..

# Copy resources to dist for running from dist folder
mkdir -p dist/resources
cp -f resources/*.png dist/resources/ 2>/dev/null || true
mkdir -p dist/resources/sounds
cp -f resources/sounds/* dist/resources/sounds/ 2>/dev/null || true
cp -f Readme.md dist/ 2>/dev/null || true
cp -f RUNNING_GUIDE.md dist/ 2>/dev/null || true

# Copy source code to dist
echo "Creating source archive..."
mkdir -p dist/src/client
mkdir -p dist/src/server
cp -f src/client/*.java dist/src/client/ 2>/dev/null || true
cp -f src/server/*.java dist/src/server/ 2>/dev/null || true
cp -f src/server/README.md dist/src/server/ 2>/dev/null || true

# Copy test files
mkdir -p dist/test
cp -f test/*.java dist/test/ 2>/dev/null || true

# Copy libs
mkdir -p dist/libs
cp -f libs/*.jar dist/libs/ 2>/dev/null || true

echo ""
echo "========================================"
echo "   Build Complete!"
echo "========================================"
echo ""
echo "Output files in 'dist' folder:"
echo "  - PacmanAI.jar (executable)"
echo "  - src/client/ (client source code)"
echo "  - src/server/ (server source code)"
echo "  - test/ (test files)"
echo "  - libs/ (dependencies)"
echo "  - resources/ (images and sounds)"
echo ""
echo "To run: java -jar dist/PacmanAI.jar"
echo ""
