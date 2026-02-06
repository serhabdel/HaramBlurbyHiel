#!/bin/bash

# HaramBlur Content Detection Log Analysis Script
# This script helps analyze content detection logs from the HaramBlur app

APP_PACKAGE="com.hieltech.haramblur"
DB_NAME="site_blocking_database"

echo "HaramBlur Content Detection Log Analysis"
echo "========================================"

# Function to check if device is connected
check_device() {
    if ! adb devices | grep -q "device$"; then
        echo "Error: No Android device connected"
        exit 1
    fi
}

# Function to pull database
pull_database() {
    echo "Pulling database from device..."
    if adb shell run-as $APP_PACKAGE ls databases/$DB_NAME >/dev/null 2>&1; then
        adb shell run-as $APP_PACKAGE cat databases/$DB_NAME > haramblur_logs.db
        echo "Database pulled successfully"
        return 0
    else
        echo "Error: Database not found on device"
        return 1
    fi
}

# Function to analyze detection logs
analyze_detection_logs() {
    if [ ! -f "haramblur_logs.db" ]; then
        echo "Error: Database file not found"
        return 1
    fi
    
    echo ""
    echo "Detection Log Summary:"
    echo "====================="
    
    # Count total logs
    total_logs=$(sqlite3 haramblur_logs.db "SELECT COUNT(*) FROM logs;")
    echo "Total logs: $total_logs"
    
    # Count detection logs
    detection_logs=$(sqlite3 haramblur_logs.db "SELECT COUNT(*) FROM logs WHERE category = 'DETECTION';")
    echo "Detection logs: $detection_logs"
    
    # Count by level
    echo ""
    echo "Log levels:"
    sqlite3 haramblur_logs.db "SELECT level, COUNT(*) FROM logs GROUP BY level;"
    
    # Count by category
    echo ""
    echo "Log categories:"
    sqlite3 haramblur_logs.db "SELECT category, COUNT(*) FROM logs GROUP BY category;"
    
    # Show recent detection logs
    if [ $detection_logs -gt 0 ]; then
        echo ""
        echo "Recent Detection Logs (last 10):"
        echo "==============================="
        sqlite3 haramblur_logs.db "SELECT datetime(timestamp/1000, 'unixepoch', 'localtime') as time, tag, message FROM logs WHERE category = 'DETECTION' ORDER BY timestamp DESC LIMIT 10;"
    fi
    
    # Show recent error logs
    echo ""
    echo "Recent Error Logs (last 5):"
    echo "=========================="
    sqlite3 haramblur_logs.db "SELECT datetime(timestamp/1000, 'unixepoch', 'localtime') as time, tag, message FROM logs WHERE level = 'ERROR' ORDER BY timestamp DESC LIMIT 5;"
    
    # Show performance metrics
    echo ""
    echo "Performance Analysis:"
    echo "===================="
    sqlite3 haramblur_logs.db "SELECT message FROM logs WHERE message LIKE '%PERFORMANCE%' ORDER BY timestamp DESC LIMIT 5;"
}

# Function to get real-time logcat logs
get_logcat_logs() {
    echo ""
    echo "Real-time Content Detection Logs (press Ctrl+C to stop):"
    echo "======================================================"
    adb logcat -s "ContentDetectionEngine:D" --format=threadtime
}

# Function to export logs
export_logs() {
    echo ""
    echo "Exporting all logs to text file..."
    timestamp=$(date +"%Y%m%d_%H%M%S")
    output_file="haramblur_logs_$timestamp.txt"
    
    sqlite3 haramblur_logs.db ".headers on" ".mode column" "SELECT datetime(timestamp/1000, 'unixepoch', 'localtime') as timestamp, level, tag, message FROM logs ORDER BY timestamp;" > $output_file
    
    echo "Logs exported to $output_file"
}

# Main menu
show_menu() {
    echo ""
    echo "Select an option:"
    echo "1. Pull and analyze database logs"
    echo "2. View real-time logcat logs"
    echo "3. Export all logs to file"
    echo "4. Exit"
    echo -n "Enter choice (1-4): "
}

# Main execution
main() {
    check_device
    
    while true; do
        show_menu
        read choice
        
        case $choice in
            1)
                pull_database
                analyze_detection_logs
                ;;
            2)
                get_logcat_logs
                ;;
            3)
                pull_database
                export_logs
                ;;
            4)
                echo "Exiting..."
                exit 0
                ;;
            *)
                echo "Invalid choice. Please enter 1-4."
                ;;
        esac
    done
}

# Run main function
main