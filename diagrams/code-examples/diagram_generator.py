#!/usr/bin/env python3
"""
HaramBlur Diagram Generator - Python Version
Generate and manipulate Mermaid diagrams programmatically
"""

import os
import json
import subprocess
from typing import Dict, List, Optional
from dataclasses import dataclass
from enum import Enum


class DiagramType(Enum):
    """Available diagram types"""
    FLOWCHART = "flowchart"
    GRAPH = "graph"
    SEQUENCE = "sequenceDiagram"
    STATE = "stateDiagram-v2"
    CLASS = "classDiagram"


class ColorScheme(Enum):
    """Color schemes for different diagram elements"""
    PROTECTION = "#e1f5fe"
    ISLAMIC = "#f3e5f5"
    PRIVACY = "#e8f5e8"
    PERFORMANCE = "#fff3e0"
    UX = "#fce4ec"
    CUSTOMIZATION = "#f1f8e9"


@dataclass
class DiagramNode:
    """Represents a node in a Mermaid diagram"""
    id: str
    label: str
    shape: str = "rectangle"  # rectangle, circle, diamond, etc.
    style_class: Optional[str] = None


@dataclass
class DiagramEdge:
    """Represents an edge/connection in a Mermaid diagram"""
    from_node: str
    to_node: str
    label: Optional[str] = None
    style: str = "-->"  # -->, -.-> , ==> , etc.


class HaramBlurDiagramGenerator:
    """Generate HaramBlur architecture diagrams programmatically"""
    
    def __init__(self):
        self.diagrams_dir = os.path.dirname(os.path.abspath(__file__))
        self.output_dir = os.path.join(self.diagrams_dir, "generated")
        os.makedirs(self.output_dir, exist_ok=True)
    
    def create_app_features_overview(self) -> str:
        """Generate the app features overview diagram"""
        diagram = """graph TD
    subgraph "🛡️ Smart Content Protection"
        A[Real-time Screen Capture] --> B[AI-Powered Analysis]
        B --> C[Face Detection]
        B --> D[NSFW Detection]
        B --> E[Gender Classification]
        C --> F[Intelligent Blurring]
        D --> F
        E --> F
        F --> G[Selective Overlay Application]
    end

    subgraph "🕌 Islamic Integration"
        H[Quranic Verse Database] --> I[Contextual Guidance]
        J[Dhikr System] --> K[Scheduled Reminders]
        L[Prayer Times API] --> M[Prayer Notifications]
        I --> N[Spiritual Support]
        K --> N
        M --> N
    end

    subgraph "🔒 Privacy & Security"
        O[On-Device Processing] --> P[Zero Data Upload]
        Q[Local AI Models] --> R[Offline Capability]
        S[Secure Storage] --> T[Encrypted Settings]
        P --> U[Complete Privacy]
        R --> U
        T --> U
    end

    G --> N
    N --> U

    classDef protection fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef islamic fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef privacy fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px

    class A,B,C,D,E,F,G protection
    class H,I,J,K,L,M,N islamic
    class O,P,Q,R,S,T,U privacy"""
        
        return diagram
    
    def create_content_detection_workflow(self) -> str:
        """Generate detailed content detection workflow"""
        diagram = """flowchart TD
    Start([📱 App Launch]) --> CheckService{Service Active?}
    CheckService -->|No| EnableService[⚙️ Enable Accessibility]
    CheckService -->|Yes| Monitor[👁️ Monitor Screen]
    
    EnableService --> Monitor
    Monitor --> Capture[📸 Screen Capture]
    Capture --> PreProcess[🔧 Image Preprocessing]
    
    PreProcess --> PerformanceCheck{Performance Mode}
    PerformanceCheck -->|Ultra-Fast| LowRes[📱 Quarter Resolution]
    PerformanceCheck -->|Fast| MidRes[📱 Half Resolution]
    PerformanceCheck -->|Balanced| FullRes[📱 Full Resolution]
    PerformanceCheck -->|High Quality| EnhancedRes[📱 Enhanced Resolution]
    
    LowRes --> Analysis[🤖 AI Analysis Pipeline]
    MidRes --> Analysis
    FullRes --> Analysis
    EnhancedRes --> Analysis
    
    Analysis --> FaceDetection[👤 Face Detection]
    Analysis --> NSFWDetection[🚫 NSFW Analysis]
    Analysis --> GenderDetection[⚧️ Gender Classification]
    
    FaceDetection --> FaceResult{Faces Found?}
    NSFWDetection --> NSFWResult{NSFW Content?}
    GenderDetection --> GenderResult{Gender Specific?}
    
    FaceResult -->|Yes| BlurFaces[🟦 Apply Face Blur]
    NSFWResult -->|Yes| BlurNSFW[🟦 Apply Content Blur]
    GenderResult -->|Yes| BlurGender[🟦 Apply Gender Blur]
    
    BlurFaces --> ShowGuidance[📖 Display Islamic Guidance]
    BlurNSFW --> ShowGuidance
    BlurGender --> ShowGuidance
    
    FaceResult -->|No| ContinueMonitor
    NSFWResult -->|No| ContinueMonitor
    GenderResult -->|No| ContinueMonitor
    
    ShowGuidance --> SelectVerse{Content Category}
    SelectVerse -->|Face Content| FaceVerse[👁️ Lower Gaze Verses]
    SelectVerse -->|NSFW Content| PurityVerse[🚫 Purity Verses]
    SelectVerse -->|General| GeneralVerse[🌟 General Guidance]
    
    FaceVerse --> DisplayVerse[📱 Show Verse Dialog]
    PurityVerse --> DisplayVerse
    GeneralVerse --> DisplayVerse
    
    DisplayVerse --> UpdateStats[📊 Update Statistics]
    UpdateStats --> ContinueMonitor[🔄 Continue Monitoring]
    ContinueMonitor --> Monitor

    classDef startEnd fill:#ff9999,stroke:#333,stroke-width:2px
    classDef process fill:#99ccff,stroke:#333,stroke-width:2px
    classDef decision fill:#ffcc99,stroke:#333,stroke-width:2px
    classDef islamic fill:#cc99ff,stroke:#333,stroke-width:2px
    classDef blur fill:#99ff99,stroke:#333,stroke-width:2px

    class Start,ContinueMonitor startEnd
    class Capture,PreProcess,Analysis,FaceDetection,NSFWDetection,GenderDetection process
    class CheckService,PerformanceCheck,FaceResult,NSFWResult,GenderResult,SelectVerse decision
    class ShowGuidance,FaceVerse,PurityVerse,GeneralVerse,DisplayVerse islamic
    class BlurFaces,BlurNSFW,BlurGender blur"""
        
        return diagram
    
    def create_islamic_integration_diagram(self) -> str:
        """Generate Islamic features integration diagram"""
        diagram = """graph TD
    subgraph "📖 Quranic Content System"
        QDB[(Quranic Database<br/>500+ Verses)] --> QCat[Content Categorization]
        QCat --> QLang[Multi-Language Support]
        QLang --> QArabic[Arabic Text]
        QLang --> QEnglish[English Translation]
        QLang --> QFrench[French Translation]
        QLang --> QIndonesian[Indonesian Translation]
    end
    
    subgraph "💎 Dhikr Remembrance System"
        DTime[Time Manager] --> DSchedule{Prayer Time?}
        DSchedule -->|Fajr| DFajr[🌅 Morning Dhikr]
        DSchedule -->|Dhuhr| DDhuhr[☀️ Noon Dhikr]
        DSchedule -->|Asr| DAsr[🌤️ Afternoon Dhikr]
        DSchedule -->|Maghrib| DMaghrib[🌅 Evening Dhikr]
        DSchedule -->|Isha| DIsha[🌙 Night Dhikr]
        DSchedule -->|Other| DGeneral[🔄 General Dhikr]
        
        DFajr --> DDisplay[✨ Overlay Display]
        DDhuhr --> DDisplay
        DAsr --> DDisplay
        DMaghrib --> DDisplay
        DIsha --> DDisplay
        DGeneral --> DDisplay
    end
    
    subgraph "🕐 Prayer Time Integration"
        PLocation[📍 Location Service] --> PCalculate[📐 Prayer Calculation]
        PCalculate --> PAPI[🌐 Aladhan API]
        PAPI --> PTimes[🕐 5 Daily Prayers]
        PTimes --> PNotify[🔔 Prayer Notifications]
        PNotify --> PQibla[🧭 Qibla Direction]
    end
    
    subgraph "🎯 Content-Aware Integration"
        ContentType[Content Detection Result] --> ContentCategory{Category}
        ContentCategory -->|Face| FaceGuidance[👁️ Lower Gaze]
        ContentCategory -->|NSFW| NSFWGuidance[🚫 Purity & Modesty]
        ContentCategory -->|Sites| SiteGuidance[🌐 Digital Ethics]
        ContentCategory -->|General| GeneralGuidance[⚠️ General]
        
        FaceGuidance --> VerseSelection[📚 Verse Selection Engine]
        NSFWGuidance --> VerseSelection
        SiteGuidance --> VerseSelection
        GeneralGuidance --> VerseSelection
    end
    
    subgraph "📱 User Interface Integration"
        UI[Islamic UI Components] --> VerseDialog[📖 Quranic Verse Dialog]
        UI --> DhikrOverlay[💎 Dhikr Overlay]
        UI --> PrayerWidget[🕐 Prayer Times Widget]
        UI --> QiblaCompass[🧭 Qibla Compass]
        UI --> StatsDisplay[📊 Islamic Statistics]
    end
    
    %% Connections between systems
    QArabic --> VerseSelection
    QEnglish --> VerseSelection
    QFrench --> VerseSelection
    QIndonesian --> VerseSelection
    
    VerseSelection --> VerseDialog
    DDisplay --> DhikrOverlay
    PQibla --> QiblaCompass
    PTimes --> PrayerWidget
    
    %% User Settings Integration
    UserSettings[⚙️ Islamic Settings] --> LanguageChoice[🌍 Language Selection]
    UserSettings --> DhikrPrefs[💎 Dhikr Preferences]
    UserSettings --> PrayerPrefs[🕐 Prayer Settings]
    UserSettings --> VersePrefs[📖 Verse Categories]
    
    LanguageChoice --> QLang
    DhikrPrefs --> DTime
    PrayerPrefs --> PLocation
    VersePrefs --> QCat

    classDef quranic fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    classDef dhikr fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef prayer fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
    classDef content fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef ui fill:#fce4ec,stroke:#c2185b,stroke-width:2px
    classDef settings fill:#f1f8e9,stroke:#388e3c,stroke-width:2px

    class QDB,QCat,QLang,QArabic,QEnglish,QFrench,QIndonesian,VerseSelection quranic
    class DTime,DSchedule,DFajr,DDhuhr,DAsr,DMaghrib,DIsha,DGeneral,DDisplay dhikr
    class PLocation,PCalculate,PAPI,PTimes,PNotify,PQibla prayer
    class ContentType,ContentCategory,FaceGuidance,NSFWGuidance,SiteGuidance,GeneralGuidance content
    class UI,VerseDialog,DhikrOverlay,PrayerWidget,QiblaCompass,StatsDisplay ui
    class UserSettings,LanguageChoice,DhikrPrefs,PrayerPrefs,VersePrefs settings"""
        
        return diagram
    
    def create_user_journey_flowchart(self) -> str:
        """Generate user journey workflow diagram"""
        diagram = """flowchart TD
    Start([👤 User Downloads App]) --> Launch[🚀 First Launch]
    Launch --> Welcome[👋 Welcome Screen]
    Welcome --> Permissions[📋 Request Permissions]
    
    Permissions --> AccessibilityPerm{Accessibility Permission}
    AccessibilityPerm -->|Denied| ExplainAccess[❓ Explain Importance]
    ExplainAccess --> AccessibilityPerm
    AccessibilityPerm -->|Granted| OverlayPerm[📍 Overlay Permission]
    
    OverlayPerm --> NotificationPerm[🔔 Notification Permission]
    NotificationPerm --> LocationPerm[📍 Location Permission - Optional]
    LocationPerm --> BasicConfig[⚙️ Basic Configuration]
    
    BasicConfig --> IslamicSetup{Configure Islamic Features?}
    IslamicSetup -->|Yes| VerseCategories[📖 Select Verse Categories]
    VerseCategories --> DhikrTimes[💎 Configure Dhikr Times]
    DhikrTimes --> PrayerSettings[🕐 Set Prayer Notifications]
    PrayerSettings --> BlurPrefs
    
    IslamicSetup -->|Skip| BlurPrefs[🎨 Customize Blur Preferences]
    BlurPrefs --> SetupComplete[✨ Setup Complete]
    
    %% Daily Usage Flow
    SetupComplete --> DailyUse[📱 Daily Usage]
    DailyUse --> ServiceCheck{Service Active?}
    ServiceCheck -->|No| ServiceAlert[⚠️ Service Inactive Alert]
    ServiceAlert --> RestartService[🔧 Restart Service]
    RestartService --> ServiceActive
    ServiceCheck -->|Yes| ServiceActive[✅ Service Active]
    
    ServiceActive --> UserBrowsing[👤 User Browsing]
    UserBrowsing --> ContentScan[📸 Content Scanning]
    ContentScan --> ContentCheck{Inappropriate Content?}
    
    ContentCheck -->|No| ContinueBrowsing[✅ Continue Browsing]
    ContentCheck -->|Yes| ApplyBlur[🚫 Apply Blur Overlay]
    
    ApplyBlur --> ShowGuidance{Show Islamic Guidance?}
    ShowGuidance -->|Yes| DisplayVerse[📖 Display Quranic Verse]
    ShowGuidance -->|No| UpdateStats[📊 Update Statistics Only]
    
    DisplayVerse --> UserReads[🤲 User Reads Guidance]
    UserReads --> UserAction{User Action}
    UserAction -->|Dismiss| ReturnToApp[📱 Return to App]
    UserAction -->|Share| ShareVerse[📤 Share Verse]
    UserAction -->|Learn More| FullVerse[📚 Open Full Verse Context]
    
    ShareVerse --> ReturnToApp
    FullVerse --> VerseContext[📖 Quranic Context]
    VerseContext --> ReturnToApp
    UpdateStats --> ReturnToApp
    
    ReturnToApp --> ContinueMonitoring[🔄 Continue Monitoring]
    ContinueBrowsing --> ContinueMonitoring
    ContinueMonitoring --> UserBrowsing
    
    %% Islamic Features Usage
    ContinueMonitoring --> PrayerTime{Prayer Time?}
    PrayerTime -->|Yes| PrayerNotification[🔔 Prayer Notification]
    PrayerNotification --> PrayerResponse{User Response}
    PrayerResponse -->|Pray Now| QiblaCompass[🧭 Qibla Direction]
    PrayerResponse -->|Remind Later| SnoozeReminder[⏳ Snooze 5 Minutes]
    PrayerResponse -->|Dismiss| ContinueActivity[📱 Continue Activity]
    
    QiblaCompass --> PrayerComplete[🤲 Prayer Complete]
    SnoozeReminder --> PrayerReminder[🔔 Prayer Reminder Again]
    PrayerReminder --> PrayerResponse
    
    ContinueMonitoring --> DhikrTime{Dhikr Time?}
    DhikrTime -->|Yes| ShowDhikr[✨ Show Dhikr Overlay]
    ShowDhikr --> DhikrDisplay{Display Method}
    DhikrDisplay -->|Overlay| ScreenOverlay[📱 Screen Overlay]
    DhikrDisplay -->|Notification| PushNotification[🔔 Push Notification]
    
    ScreenOverlay --> ViewDhikr[👁️ User Views Dhikr]
    PushNotification --> ViewDhikr
    ViewDhikr --> AutoDismiss[🕐 Auto-Dismiss After Duration]
    AutoDismiss --> TrackDhikr[📈 Track Dhikr Interaction]
    
    %% Error Handling & Emergency
    ServiceActive --> EmergencyStop{Emergency Stop?}
    EmergencyStop -->|Yes| EmergencyButton[🔴 Emergency Button]
    EmergencyButton --> TempDisable[⏸️ Temporary Disable]
    TempDisable --> AutoReEnable[⏰ Auto Re-enable Timer]
    AutoReEnable --> ServiceRestored[✅ Service Restored]
    
    ServiceActive --> PerformanceIssue{Performance Issues?}
    PerformanceIssue -->|Battery Drain| UltraFastMode[🔋 Switch to Ultra-Fast]
    PerformanceIssue -->|App Crashes| RestartService
    PerformanceIssue -->|False Positives| AdjustSensitivity[🎯 Adjust Sensitivity]
    PerformanceIssue -->|Slow Performance| LowerQuality[⚡ Lower Detection Quality]
    
    %% Settings Customization
    DailyUse --> OpenSettings[⚙️ Open Settings]
    OpenSettings --> SettingsType{Configuration Type}
    SettingsType -->|Detection| AdjustSensitivity
    SettingsType -->|Performance| ChangePerformanceMode[⚡ Change Performance Mode]
    SettingsType -->|Islamic| ConfigureIslamic[🕌 Configure Islamic Features]
    SettingsType -->|Apps| ManageWhitelist[📱 Manage App Whitelist]
    SettingsType -->|Visual| CustomizeBlur[🎨 Customize Blur Style]
    
    AdjustSensitivity --> TestSettings[🔧 Test New Settings]
    ChangePerformanceMode --> TestSettings
    ConfigureIslamic --> PreviewIslamic[🕌 Preview Islamic Content]
    ManageWhitelist --> SelectApps[📱 Select/Deselect Apps]
    CustomizeBlur --> PreviewBlur[🎨 Preview Blur Effects]
    
    TestSettings --> SaveSettings[✅ Save Settings]
    PreviewIslamic --> SaveSettings
    SelectApps --> SaveSettings
    PreviewBlur --> SaveSettings
    
    SaveSettings --> ApplyChanges[🔄 Apply Changes]
    ApplyChanges --> MonitorPerformance[📊 Monitor Performance]
    MonitorPerformance --> DailyUse

    %% Return connections
    PrayerComplete --> UserBrowsing
    ContinueActivity --> UserBrowsing
    TrackDhikr --> ContinueMonitoring
    ServiceRestored --> UserBrowsing
    UltraFastMode --> ServiceActive
    LowerQuality --> ServiceActive

    classDef start fill:#ff9999,stroke:#333,stroke-width:2px
    classDef setup fill:#99ccff,stroke:#333,stroke-width:2px
    classDef daily fill:#99ff99,stroke:#333,stroke-width:2px
    classDef islamic fill:#cc99ff,stroke:#333,stroke-width:2px
    classDef emergency fill:#ffcccc,stroke:#333,stroke-width:2px
    classDef settings fill:#ffffcc,stroke:#333,stroke-width:2px

    class Start,SetupComplete start
    class Launch,Welcome,Permissions,AccessibilityPerm,OverlayPerm,NotificationPerm,LocationPerm,BasicConfig,IslamicSetup,VerseCategories,DhikrTimes,PrayerSettings,BlurPrefs setup
    class DailyUse,ServiceCheck,ServiceActive,UserBrowsing,ContentScan,ContentCheck,ContinueBrowsing,ApplyBlur,UpdateStats,ReturnToApp,ContinueMonitoring daily
    class ShowGuidance,DisplayVerse,UserReads,UserAction,ShareVerse,FullVerse,VerseContext,PrayerTime,PrayerNotification,PrayerResponse,QiblaCompass,PrayerComplete,DhikrTime,ShowDhikr,DhikrDisplay,ScreenOverlay,PushNotification,ViewDhikr,AutoDismiss,TrackDhikr islamic
    class EmergencyStop,EmergencyButton,TempDisable,AutoReEnable,ServiceRestored,PerformanceIssue,UltraFastMode,LowerQuality emergency
    class OpenSettings,SettingsType,ConfigureIslamic,ManageWhitelist,CustomizeBlur,PreviewIslamic,SelectApps,PreviewBlur,TestSettings,SaveSettings,ApplyChanges,MonitorPerformance,AdjustSensitivity,ChangePerformanceMode settings"""
        
        return diagram
    
    def generate_diagram_file(self, diagram_name: str, content: str) -> str:
        """Generate a clean Mermaid file"""
        output_path = os.path.join(self.output_dir, f"{diagram_name}.mmd")
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"✅ Generated: {output_path}")
        return output_path
    
    def generate_image(self, mmd_file: str, output_format: str = "png") -> Optional[str]:
        """Generate image from Mermaid file using mmdc CLI"""
        if not os.path.exists(mmd_file):
            print(f"❌ Mermaid file not found: {mmd_file}")
            return None
        
        base_name = os.path.splitext(os.path.basename(mmd_file))[0]
        output_file = os.path.join(self.output_dir, f"{base_name}.{output_format}")
        
        cmd = [
            "mmdc",
            "-i", mmd_file,
            "-o", output_file,
            "--width", "2400",
            "--height", "1800",
            "--backgroundColor", "white",
            "--theme", "default"
        ]
        
        if output_format == "png":
            cmd.extend(["--scale", "2"])
        
        try:
            subprocess.run(cmd, check=True, capture_output=True)
            print(f"✅ Generated {output_format.upper()}: {output_file}")
            return output_file
        except subprocess.CalledProcessError as e:
            print(f"❌ Failed to generate {output_format.upper()}: {e}")
            return None
    
    def generate_all_diagrams(self):
        """Generate all HaramBlur diagrams"""
        diagrams = {
            "01-app-features-overview": self.create_app_features_overview(),
            "03-content-detection-workflow": self.create_content_detection_workflow(),
            "04-islamic-features-integration": self.create_islamic_integration_diagram(),
            "05-user-journey-workflow": self.create_user_journey_flowchart()
        }
        
        print("🔧 Generating HaramBlur diagrams...")
        
        generated_files = []
        for name, content in diagrams.items():
            # Generate Mermaid file
            mmd_file = self.generate_diagram_file(name, content)
            generated_files.append(mmd_file)
            
            # Generate PNG and SVG
            png_file = self.generate_image(mmd_file, "png")
            svg_file = self.generate_image(mmd_file, "svg")
            
            if png_file:
                generated_files.append(png_file)
            if svg_file:
                generated_files.append(svg_file)
        
        print(f"\n🎉 Generated {len(generated_files)} files in {self.output_dir}")
        return generated_files
    
    def create_interactive_html(self, output_file: str = "haramblur-diagrams.html"):
        """Create an interactive HTML file with all diagrams"""
        html_content = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>🕌 HaramBlur - Architecture Diagrams</title>
    <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            margin: 0;
            padding: 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            color: #333;
        }
        .container {
            max-width: 1400px;
            margin: 0 auto;
            background: white;
            border-radius: 15px;
            padding: 30px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
        }
        .header {
            text-align: center;
            margin-bottom: 40px;
            padding: 20px;
            background: linear-gradient(45deg, #8e44ad, #3498db);
            color: white;
            border-radius: 10px;
        }
        .header h1 {
            margin: 0;
            font-size: 2.5em;
        }
        .header .subtitle {
            margin: 10px 0 0 0;
            font-style: italic;
            opacity: 0.9;
        }
        .header .arabic {
            margin-top: 15px;
            font-size: 1.2em;
            font-style: italic;
        }
        .diagram-nav {
            display: flex;
            justify-content: center;
            gap: 15px;
            margin-bottom: 30px;
            flex-wrap: wrap;
        }
        .nav-button {
            padding: 10px 20px;
            background: #3498db;
            color: white;
            border: none;
            border-radius: 25px;
            cursor: pointer;
            transition: all 0.3s ease;
            font-weight: bold;
        }
        .nav-button:hover {
            background: #2980b9;
            transform: translateY(-2px);
        }
        .nav-button.active {
            background: #8e44ad;
        }
        .diagram-section {
            margin: 40px 0;
            padding: 25px;
            border: 1px solid #ecf0f1;
            border-radius: 10px;
            background: #fdfdfd;
            display: none;
        }
        .diagram-section.active {
            display: block;
        }
        .diagram-title {
            font-size: 1.8em;
            color: #2c3e50;
            margin-bottom: 20px;
            padding-bottom: 10px;
            border-bottom: 3px solid #3498db;
            display: flex;
            align-items: center;
            gap: 10px;
        }
        .diagram-description {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 8px;
            margin-bottom: 20px;
            border-left: 4px solid #3498db;
        }
        .diagram-container {
            display: flex;
            justify-content: center;
            margin: 30px 0;
            min-height: 400px;
        }
        .mermaid {
            max-width: 100%;
            height: auto;
        }
        .controls {
            text-align: center;
            margin: 20px 0;
        }
        .control-button {
            margin: 0 10px;
            padding: 8px 16px;
            background: #27ae60;
            color: white;
            border: none;
            border-radius: 5px;
            cursor: pointer;
        }
        .control-button:hover {
            background: #229954;
        }
        .footer {
            text-align: center;
            margin-top: 50px;
            padding: 20px;
            background: #f8f9fa;
            border-radius: 10px;
            color: #7f8c8d;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🕌 HaramBlur</h1>
            <p class="subtitle">Islamic Content Filtering - Architecture & Workflow Diagrams</p>
            <p class="arabic">الحمد لله رب العالمين</p>
            <p style="font-size: 0.9em; opacity: 0.8;">"All praise is due to Allah, Lord of the worlds"</p>
        </div>

        <div class="diagram-nav">
            <button class="nav-button active" onclick="showDiagram('features')">🌟 Features Overview</button>
            <button class="nav-button" onclick="showDiagram('workflow')">🔍 Detection Workflow</button>
            <button class="nav-button" onclick="showDiagram('islamic')">🕌 Islamic Integration</button>
            <button class="nav-button" onclick="showDiagram('journey')">👤 User Journey</button>
        </div>

        <div id="features" class="diagram-section active">
            <h2 class="diagram-title">🌟 App Features Overview</h2>
            <div class="diagram-description">
                <p><strong>Purpose:</strong> High-level overview of all HaramBlur capabilities including smart content protection, Islamic integration, privacy features, performance optimization, and user experience enhancements.</p>
            </div>
            <div class="diagram-container">
                <div class="mermaid">
""" + self.create_app_features_overview() + """
                </div>
            </div>
        </div>

        <div id="workflow" class="diagram-section">
            <h2 class="diagram-title">🔍 Content Detection Workflow</h2>
            <div class="diagram-description">
                <p><strong>Purpose:</strong> Detailed workflow showing how HaramBlur detects and filters inappropriate content, from screen capture through AI analysis to Islamic guidance display.</p>
            </div>
            <div class="diagram-container">
                <div class="mermaid">
""" + self.create_content_detection_workflow() + """
                </div>
            </div>
        </div>

        <div id="islamic" class="diagram-section">
            <h2 class="diagram-title">🕌 Islamic Features Integration</h2>
            <div class="diagram-description">
                <p><strong>Purpose:</strong> Shows how Quranic verses, Dhikr reminders, prayer times, and other Islamic features are integrated throughout the app to provide spiritual guidance and support.</p>
            </div>
            <div class="diagram-container">
                <div class="mermaid">
""" + self.create_islamic_integration_diagram() + """
                </div>
            </div>
        </div>

        <div id="journey" class="diagram-section">
            <h2 class="diagram-title">👤 User Journey Workflow</h2>
            <div class="diagram-description">
                <p><strong>Purpose:</strong> Complete user experience from app installation and setup through daily usage, including Islamic features interaction and troubleshooting scenarios.</p>
            </div>
            <div class="diagram-container">
                <div class="mermaid">
""" + self.create_user_journey_flowchart() + """
                </div>
            </div>
        </div>

        <div class="controls">
            <button class="control-button" onclick="downloadDiagram()">📥 Download Current Diagram</button>
            <button class="control-button" onclick="printDiagram()">🖨️ Print Diagram</button>
            <button class="control-button" onclick="shareDiagram()">📤 Share Diagram</button>
        </div>

        <div class="footer">
            <p><strong>Made with ❤️ for the Muslim Ummah</strong></p>
            <p style="font-style: italic;">May these diagrams help in understanding how technology can serve Islamic values while maintaining technical excellence.</p>
            <p style="font-size: 0.9em; margin-top: 15px;">Generated using Python & Mermaid.js</p>
        </div>
    </div>

    <script>
        mermaid.initialize({ 
            startOnLoad: true,
            theme: 'default',
            flowchart: {
                curve: 'basis',
                htmlLabels: true,
                useMaxWidth: true
            },
            sequence: {
                useMaxWidth: true
            },
            gantt: {
                useMaxWidth: true
            }
        });

        function showDiagram(diagramId) {
            // Hide all diagrams
            const sections = document.querySelectorAll('.diagram-section');
            sections.forEach(section => section.classList.remove('active'));
            
            // Hide all nav buttons
            const buttons = document.querySelectorAll('.nav-button');
            buttons.forEach(button => button.classList.remove('active'));
            
            // Show selected diagram
            document.getElementById(diagramId).classList.add('active');
            event.target.classList.add('active');
            
            // Re-render mermaid diagrams
            setTimeout(() => {
                mermaid.init();
            }, 100);
        }

        function downloadDiagram() {
            const activeSection = document.querySelector('.diagram-section.active');
            const diagramTitle = activeSection.querySelector('.diagram-title').textContent;
            alert(`Download functionality for "${diagramTitle}" - Implementation would export as PNG/SVG`);
        }

        function printDiagram() {
            window.print();
        }

        function shareDiagram() {
            const activeSection = document.querySelector('.diagram-section.active');
            const diagramTitle = activeSection.querySelector('.diagram-title').textContent;
            
            if (navigator.share) {
                navigator.share({
                    title: `HaramBlur - ${diagramTitle}`,
                    text: 'Islamic Content Filtering Architecture Diagram',
                    url: window.location.href
                });
            } else {
                navigator.clipboard.writeText(window.location.href);
                alert('Link copied to clipboard!');
            }
        }

        // Initialize on load
        document.addEventListener('DOMContentLoaded', function() {
            mermaid.init();
        });
    </script>
</body>
</html>"""
        
        output_path = os.path.join(self.output_dir, output_file)
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(html_content)
        
        print(f"✅ Generated interactive HTML: {output_path}")
        return output_path


def main():
    """Main function to generate all diagrams"""
    print("🕌 HaramBlur Diagram Generator - Python Edition")
    print("=" * 50)
    
    generator = HaramBlurDiagramGenerator()
    
    # Generate all diagrams
    generated_files = generator.generate_all_diagrams()
    
    # Generate interactive HTML
    html_file = generator.create_interactive_html()
    
    print(f"\n📁 All files generated in: {generator.output_dir}")
    print(f"🌐 Open the HTML file to view interactive diagrams: {html_file}")
    print("\n💡 Tips:")
    print("  • View PNG files: eog generated/*.png")
    print("  • Open HTML file: firefox generated/haramblur-diagrams.html")
    print("  • Edit diagrams: code generated/")


if __name__ == "__main__":
    main()