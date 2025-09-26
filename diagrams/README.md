# HaramBlur - Architecture & Workflow Diagrams 📊

This folder contains comprehensive Mermaid diagrams that visualize the HaramBlur app's features, architecture, and workflows. These diagrams provide a deep understanding of how the Islamic content filtering system works.

## 📋 Diagram Index

### 1. [App Features Overview](01-app-features-overview.mmd) 🌟
**Purpose**: High-level overview of all HaramBlur features
- **Smart Content Protection**: Real-time detection and blurring
- **Islamic Integration**: Quranic guidance and spiritual features
- **Privacy & Security**: On-device processing and data protection
- **Performance Features**: GPU acceleration and battery optimization
- **User Experience**: Modern UI and customization options
- **Customization**: Personalization and configuration options

### 2. [System Architecture](02-system-architecture.mmd) 🏗️
**Purpose**: Detailed technical architecture showing all layers and components
- **User Interface Layer**: Activities, screens, and ViewModels
- **UI Components Layer**: Reusable components and widgets
- **Service Layer**: Accessibility service and background workers
- **Detection Layer**: AI/ML models and content analysis
- **Data Layer**: Repositories, databases, and data models
- **External Integrations**: APIs and third-party services

### 3. [Content Detection Workflow](03-content-detection-workflow.mmd) 🔍
**Purpose**: Complete process from screen capture to content filtering
- **Screen Capture**: Accessibility service triggering
- **Performance Modes**: Ultra-fast to high-quality processing
- **AI Analysis Pipeline**: Face, NSFW, and gender detection
- **Blur Application**: Different blur styles and overlay generation
- **Islamic Guidance**: Contextual Quranic verse display
- **Error Handling**: Robust error management and recovery

### 4. [Islamic Features Integration](04-islamic-features-integration.mmd) 🕌
**Purpose**: How Islamic content and features are integrated throughout the app
- **Quranic Database**: 500+ categorized verses with translations
- **Content-Aware Guidance**: Contextual verse selection
- **Dhikr System**: Time-based Islamic remembrance
- **Prayer Notifications**: Location-based prayer times
- **UI Integration**: Dialogs, overlays, and widgets
- **Personalization**: Configuration and customization

### 5. [User Journey Workflow](05-user-journey-workflow.mmd) 👤
**Purpose**: Complete user experience from installation to daily usage
- **Installation & Setup**: Permissions and initial configuration
- **Daily Usage Flow**: Typical browsing and protection scenarios
- **Islamic Features Usage**: Prayer times and Dhikr interactions
- **Customization Journey**: Settings configuration and testing
- **Emergency & Troubleshooting**: Problem resolution workflows

### 6. [Data Flow Architecture](06-data-flow-architecture.mmd) 📊
**Purpose**: How data moves through the system components
- **Input Sources**: Screen capture, user interactions, external APIs
- **Processing Pipeline**: Image analysis and ML processing
- **Storage Layer**: Databases and caching systems
- **Business Logic**: Decision making and filtering logic
- **External Services**: Third-party API integrations
- **Analytics & Monitoring**: Performance and usage tracking
- **Output Interfaces**: UI displays and user feedback

## 🖥️ How to View These Diagrams

### Option 1: GitHub/GitLab (Recommended)
GitHub and GitLab automatically render Mermaid diagrams. Simply view the `.mmd` files directly in your repository.

### Option 2: Mermaid Live Editor
1. Go to [mermaid.live](https://mermaid.live/)
2. Copy the content from any `.mmd` file
3. Paste it into the editor
4. View the rendered diagram

### Option 3: VS Code Extension
1. Install the "Mermaid Markdown Syntax Highlighting" extension
2. Install the "Markdown Preview Mermaid Support" extension
3. Open any `.mmd` file and use the preview function

### Option 4: Local Mermaid CLI
```bash
# Install Mermaid CLI
npm install -g @mermaid-js/mermaid-cli

# Generate PNG from any diagram
mmdc -i 01-app-features-overview.mmd -o app-features.png

# Generate SVG
mmdc -i 02-system-architecture.mmd -o architecture.svg
```

### Option 5: Online Mermaid Editors
- [Mermaid Live Editor](https://mermaid.live/)
- [Draw.io](https://app.diagrams.net/) (supports Mermaid import)
- [Lucidchart](https://www.lucidchart.com/) (has Mermaid support)

## 📖 Understanding the Diagrams

### Color Coding
Each diagram uses consistent color coding:
- **Blue tones**: User interface and interaction components
- **Purple tones**: Islamic and spiritual features
- **Green tones**: Privacy and security aspects
- **Orange tones**: Performance and processing
- **Pink tones**: User experience and customization
- **Light green**: Configuration and settings

### Symbols and Icons
- 📱 Mobile/App related
- 🕌 Islamic features
- 🔍 Detection and analysis
- ⚡ Performance and speed
- 🔒 Privacy and security
- 🎨 UI and customization
- 📊 Analytics and monitoring
- 🌐 External services

## 🚀 Using These Diagrams

### For Developers
- **Architecture Reference**: Understand component relationships
- **Implementation Guide**: See how features interact
- **Debugging Aid**: Trace data flow and processes
- **Code Review**: Verify implementation against design

### For Stakeholders
- **Feature Overview**: Understand app capabilities
- **Technical Insight**: See the complexity and sophistication
- **Islamic Integration**: Appreciate spiritual aspects
- **Privacy Assurance**: Understand data protection measures

### For Users
- **App Understanding**: Learn how HaramBlur works
- **Feature Discovery**: See all available capabilities
- **User Journey**: Understand setup and daily usage
- **Troubleshooting**: Follow workflow for problem resolution

## 🔄 Diagram Updates

These diagrams are living documents that should be updated when:
- New features are added
- Architecture changes are made
- Workflows are modified
- Islamic features are enhanced
- Performance optimizations are implemented

## 📚 Related Documentation

- [README.md](../README.md) - Main project documentation
- [wiki/](../wiki/) - Detailed user and feature guides
- [TESTING_GUIDE.md](../TESTING_GUIDE.md) - Testing procedures
- [app/src/main/](../app/src/main/) - Source code implementation

## 🤝 Contributing to Diagrams

When updating diagrams:
1. Follow the established color scheme and styling
2. Use consistent terminology and naming
3. Include relevant emojis and icons
4. Update this README if adding new diagrams
5. Test diagrams in multiple viewers
6. Maintain readability at different zoom levels

---

<div align="center">

**Made with ❤️ for the Muslim Ummah**

*These diagrams help visualize how technology serves faith in HaramBlur*

**الحمد لله رب العالمين**

</div>