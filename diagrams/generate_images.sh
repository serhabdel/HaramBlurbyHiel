#!/bin/bash

# Create directories
mkdir -p clean images

echo "🔧 Generating clean mermaid files and images for HaramBlur diagrams..."

# Function to clean mermaid files (remove markdown wrapper)
clean_mermaid_file() {
    local input_file="$1"
    local output_file="$2"
    
    # Remove the ```mermaid and ``` lines, keep only the diagram content
    sed '/^```mermaid$/d; /^```$/d' "$input_file" > "$output_file"
}

# Function to generate image from clean mermaid file
generate_image() {
    local mmd_file="$1"
    local png_file="$2"
    local diagram_name="$3"
    
    echo "📊 Generating $diagram_name..."
    mmdc -i "$mmd_file" -o "$png_file" \
        --width 2400 \
        --height 1800 \
        --backgroundColor white \
        --theme default \
        --scale 2
    
    if [ $? -eq 0 ]; then
        echo "✅ Generated: $png_file"
    else
        echo "❌ Failed to generate: $png_file"
    fi
}

# Array of diagram files and their names
declare -a diagrams=(
    "01-app-features-overview.mmd:App Features Overview"
    "02-system-architecture.mmd:System Architecture"
    "03-content-detection-workflow.mmd:Content Detection Workflow"
    "04-islamic-features-integration.mmd:Islamic Features Integration"
    "05-user-journey-workflow.mmd:User Journey Workflow"
    "06-data-flow-architecture.mmd:Data Flow Architecture"
    "07-performance-optimization-workflow.mmd:Performance Optimization Workflow"
)

# Process each diagram
for diagram_info in "${diagrams[@]}"; do
    # Split the string into filename and name
    IFS=':' read -r filename diagram_name <<< "$diagram_info"
    
    # Define file paths
    original_file="$filename"
    clean_file="clean/$filename"
    image_file="images/${filename%.mmd}.png"
    
    # Generate clean mermaid file
    if [ -f "$original_file" ]; then
        clean_mermaid_file "$original_file" "$clean_file"
        
        # Generate image from clean file
        generate_image "$clean_file" "$image_file" "$diagram_name"
        
        # Also generate SVG version
        svg_file="images/${filename%.mmd}.svg"
        echo "🖼️ Generating SVG for $diagram_name..."
        mmdc -i "$clean_file" -o "$svg_file" \
            --width 2400 \
            --height 1800 \
            --backgroundColor white \
            --theme default
        
        if [ $? -eq 0 ]; then
            echo "✅ Generated: $svg_file"
        fi
    else
        echo "⚠️ File not found: $original_file"
    fi
    
    echo ""
done

echo "🎉 Image generation complete!"
echo ""
echo "📁 Generated files:"
echo "   📂 clean/ - Clean mermaid files for CLI"
echo "   📂 images/ - PNG and SVG images"
echo ""
echo "📖 View the generated images:"
ls -la images/
echo ""
echo "💡 Tip: Use 'eog images/*.png' to view all PNG files"
echo "💡 Tip: Use 'code images/' to open the images folder in VS Code"