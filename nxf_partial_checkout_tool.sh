#!/usr/bin/env bash

# Configuration
REPO_URL="https://github.com/DevFranzen/nexusfin-ecosystem.git"
PARENT_DIR=".."
TARGET_BASE_DIR="$PARENT_DIR/nexusfin-ecosystem-partial-co"
APPS_DIR="apps"

echo "--- NexusFin Ecosystem: Advanced Partial Checkout Tool ---"

# 1. Check if apps directory exists
if [ ! -d "$APPS_DIR" ]; then
    echo "Error: Directory '$APPS_DIR' not found. Please run this script from the project root."
    exit 1
fi

# 2. Find all services (two levels deep under /apps)
echo "Scanning for services..."

# Portable alternative to mapfile - works on both macOS and Linux
app_paths=()
while IFS= read -r line; do
    app_paths+=("$line")
done < <(find "$APPS_DIR" -mindepth 2 -maxdepth 2 -type d | sort)

if [ ${#app_paths[@]} -eq 0 ]; then
    echo "No services found in subdirectories of '$APPS_DIR'."
    exit 1
fi

# Function to perform the checkout
perform_checkout() {
    local selected_path=$1
    local service_name=$(basename "$selected_path")
    local dest_path="$TARGET_BASE_DIR/$service_name"

    echo "Checking service: $service_name..."

    if [ -d "$dest_path" ]; then
        echo ">> Skip: '$dest_path' already exists."
        return
    fi

    echo ">> Downloading $selected_path..."
    mkdir -p "$dest_path"

    # Use subshell (cd ...) to avoid changing script's working directory
    (
        cd "$dest_path" || exit
        git clone --filter=blob:none --no-checkout "$REPO_URL" . >/dev/null 2>&1
        git sparse-checkout init --cone >/dev/null 2>&1
        git sparse-checkout set "$selected_path" >/dev/null 2>&1
        git checkout main >/dev/null 2>&1
    )

    # Capture exit status of subshell
    local exit_code=$?

    if [ $exit_code -eq 0 ]; then
        echo ">> Success!"
    else
        echo ">> Error: Checkout failed for $service_name."
    fi
}

# 3. List options
echo "Available services:"
for i in "${!app_paths[@]}"; do
    display_name=${app_paths[$i]#$APPS_DIR/}
    echo "[$i] $display_name"
done
echo "[a] ALL services"

# 4. User selection
read -p "Select a service number or 'a' for all: " choice

# 5. Execute selection
mkdir -p "$TARGET_BASE_DIR"

if [[ "$choice" == "a" || "$choice" == "A" ]]; then
    echo "Starting bulk checkout for all services..."
    for path in "${app_paths[@]}"; do
        perform_checkout "$path"
    done
elif [[ -n "${app_paths[$choice]}" ]]; then
    perform_checkout "${app_paths[$choice]}"
else
    echo "Invalid selection."
    exit 1
fi

echo "---"
echo "Process finished. Checkouts are located in: $TARGET_BASE_DIR"