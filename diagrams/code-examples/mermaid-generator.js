/**
 * HaramBlur Mermaid Diagram Generator
 * Programmatically generate Mermaid diagrams for the HaramBlur app
 */

class HaramBlurDiagramGenerator {
    constructor() {
        this.colors = {
            protection: '#e1f5fe',
            islamic: '#f3e5f5',
            privacy: '#e8f5e8',
            performance: '#fff3e0',
            ux: '#fce4ec',
            customization: '#f1f8e9',
            input: '#e3f2fd',
            processing: '#f3e5f5',
            storage: '#e8f5e8',
            logic: '#fff3e0',
            external: '#fce4ec',
            monitoring: '#f1f8e9',
            output: '#ede7f6'
        };
    }

    /**
     * Generate App Features Overview diagram
     */
    generateFeaturesOverview() {
        return `
graph TD
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

    classDef protection fill:${this.colors.protection}
    classDef islamic fill:${this.colors.islamic}
    classDef privacy fill:${this.colors.privacy}

    class A,B,C,D,E,F,G protection
    class H,I,J,K,L,M,N islamic
    class O,P,Q,R,S,T,U privacy
`;
    }

    /**
     * Generate System Architecture diagram
     */
    generateSystemArchitecture() {
        return `
graph TB
    subgraph "📱 User Interface Layer"
        UI1[MainActivity] --> UI2[Navigation Controller]
        UI2 --> UI3[HomeScreenResponsive]
        UI2 --> UI4[SettingsScreen]
        UI2 --> UI5[IslamicFeaturesScreen]
    end

    subgraph "🛠️ Service Layer"
        S1[HaramBlurAccessibilityService] --> S2[ScreenCaptureManager]
        S1 --> S3[BlurOverlayManager]
        S1 --> S4[ContentDetectionEngine]
        S1 --> S5[DhikrManager]
    end

    subgraph "🔍 Detection Layer"
        D1[MLModelManager] --> D2[TensorFlow Lite Models]
        D3[FaceDetectionManager] --> D4[Google ML Kit]
        D5[SiteBlockingManager] --> D6[BlockingCategory]
    end

    UI3 --> S1
    S2 --> S4
    S4 --> D1
    S4 --> D3

    classDef ui fill:${this.colors.ux}
    classDef services fill:${this.colors.performance}
    classDef detection fill:${this.colors.protection}

    class UI1,UI2,UI3,UI4,UI5 ui
    class S1,S2,S3,S4,S5 services
    class D1,D2,D3,D4,D5,D6 detection
`;
    }

    /**
     * Generate Content Detection Workflow
     */
    generateDetectionWorkflow() {
        return `
flowchart TD
    A[📱 Screen Capture] --> B{Service Active?}
    B -->|Yes| C[🖼️ Bitmap Analysis]
    B -->|No| D[⏸️ Service Inactive]
    
    C --> E[👤 Face Detection]
    C --> F[🚫 NSFW Analysis]
    C --> G[⚧️ Gender Classification]
    
    E --> H{Faces Found?}
    F --> I{NSFW Content?}
    G --> J{Gender Specific?}
    
    H -->|Yes| K[Apply Face Blur]
    I -->|Yes| L[Apply Content Blur]
    J -->|Yes| M[Apply Gender Blur]
    
    K --> N[📖 Show Quranic Guidance]
    L --> N
    M --> N
    
    N --> O[🔄 Continue Monitoring]
    O --> A

    classDef processing fill:${this.colors.processing}
    classDef islamic fill:${this.colors.islamic}
    
    class A,B,C,E,F,G processing
    class N islamic
`;
    }

    /**
     * Generate Islamic Features Integration
     */
    generateIslamicIntegration() {
        return `
graph TD
    subgraph "📖 Quranic Content"
        Q1[Verse Database] --> Q2[Category Mapping]
        Q2 --> Q3[Contextual Selection]
        Q3 --> Q4[Multi-language Display]
    end
    
    subgraph "💎 Dhikr System"
        D1[Time-based Triggers] --> D2[Prayer Time Dhikr]
        D1 --> D3[General Reminders]
        D2 --> D4[Overlay Display]
        D3 --> D4
    end
    
    subgraph "🕐 Prayer Integration"
        P1[Location Service] --> P2[Prayer Time Calculation]
        P2 --> P3[Notification System]
        P3 --> P4[Qibla Direction]
    end
    
    Q4 --> I[Islamic Guidance UI]
    D4 --> I
    P4 --> I

    classDef quranic fill:${this.colors.islamic}
    classDef dhikr fill:${this.colors.customization}
    classDef prayer fill:${this.colors.performance}
    
    class Q1,Q2,Q3,Q4 quranic
    class D1,D2,D3,D4 dhikr
    class P1,P2,P3,P4 prayer
`;
    }

    /**
     * Generate all diagrams and return as object
     */
    generateAll() {
        return {
            featuresOverview: this.generateFeaturesOverview(),
            systemArchitecture: this.generateSystemArchitecture(),
            detectionWorkflow: this.generateDetectionWorkflow(),
            islamicIntegration: this.generateIslamicIntegration()
        };
    }

    /**
     * Export diagram to file
     */
    exportDiagram(diagramName, content) {
        const fs = require('fs');
        const path = require('path');
        
        const outputPath = path.join(__dirname, `generated-${diagramName}.mmd`);
        fs.writeFileSync(outputPath, content.trim());
        
        console.log(`✅ Exported: ${outputPath}`);
        return outputPath;
    }

    /**
     * Generate HTML preview with all diagrams
     */
    generateHTMLPreview() {
        const diagrams = this.generateAll();
        
        const html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>HaramBlur - Architecture Diagrams</title>
    <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            margin: 0;
            padding: 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
        }
        .container {
            max-width: 1400px;
            margin: 0 auto;
            background: white;
            border-radius: 15px;
            padding: 30px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
        }
        h1 {
            text-align: center;
            color: #2c3e50;
            margin-bottom: 10px;
            font-size: 2.5em;
        }
        .subtitle {
            text-align: center;
            color: #7f8c8d;
            margin-bottom: 40px;
            font-style: italic;
        }
        .diagram-section {
            margin: 40px 0;
            padding: 20px;
            border: 1px solid #ecf0f1;
            border-radius: 10px;
            background: #fdfdfd;
        }
        .diagram-title {
            font-size: 1.5em;
            color: #2c3e50;
            margin-bottom: 15px;
            padding-bottom: 10px;
            border-bottom: 2px solid #3498db;
        }
        .diagram-container {
            display: flex;
            justify-content: center;
            margin: 20px 0;
        }
        .mermaid {
            max-width: 100%;
            height: auto;
        }
        .islamic-header {
            text-align: center;
            margin: 20px 0;
            padding: 15px;
            background: linear-gradient(45deg, #8e44ad, #3498db);
            color: white;
            border-radius: 10px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="islamic-header">
            <h1>🕌 HaramBlur - Islamic Content Filtering</h1>
            <p class="subtitle">Architecture & Workflow Diagrams</p>
            <p style="font-style: italic;">الحمد لله رب العالمين - "All praise is due to Allah, Lord of the worlds"</p>
        </div>

        <div class="diagram-section">
            <h2 class="diagram-title">🌟 App Features Overview</h2>
            <div class="diagram-container">
                <div class="mermaid">${diagrams.featuresOverview}</div>
            </div>
        </div>

        <div class="diagram-section">
            <h2 class="diagram-title">🏗️ System Architecture</h2>
            <div class="diagram-container">
                <div class="mermaid">${diagrams.systemArchitecture}</div>
            </div>
        </div>

        <div class="diagram-section">
            <h2 class="diagram-title">🔍 Content Detection Workflow</h2>
            <div class="diagram-container">
                <div class="mermaid">${diagrams.detectionWorkflow}</div>
            </div>
        </div>

        <div class="diagram-section">
            <h2 class="diagram-title">🕌 Islamic Features Integration</h2>
            <div class="diagram-container">
                <div class="mermaid">${diagrams.islamicIntegration}</div>
            </div>
        </div>
    </div>

    <script>
        mermaid.initialize({ 
            startOnLoad: true,
            theme: 'default',
            flowchart: {
                curve: 'basis',
                htmlLabels: true
            }
        });
    </script>
</body>
</html>
`;
        
        return html;
    }
}

// Usage examples
if (typeof module !== 'undefined' && module.exports) {
    module.exports = HaramBlurDiagramGenerator;
} else {
    // Browser usage
    window.HaramBlurDiagramGenerator = HaramBlurDiagramGenerator;
}

// Example usage in Node.js
if (typeof require !== 'undefined') {
    const generator = new HaramBlurDiagramGenerator();
    
    // Generate all diagrams
    const diagrams = generator.generateAll();
    console.log('📊 Generated all HaramBlur diagrams');
    
    // Export HTML preview
    const fs = require('fs');
    const html = generator.generateHTMLPreview();
    fs.writeFileSync('haramblur-diagrams-preview.html', html);
    console.log('✅ Generated HTML preview: haramblur-diagrams-preview.html');
}