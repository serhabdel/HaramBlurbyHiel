# Contributing to HaramBlur

Thank you for your interest in contributing to HaramBlur! This document provides guidelines for contributing to the project.

## 🤝 Code of Conduct

This project follows Islamic principles of:
- **Respect** - Treat all contributors with respect
- **Honesty** - Be truthful in your contributions
- **Excellence** - Strive for high-quality work (Ihsan)

## 🚀 How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported
2. Create a new issue with:
   - Clear description
   - Steps to reproduce
   - Expected vs actual behavior
   - Device info (Android version, RAM)
   - Screenshots if applicable

### Suggesting Features

1. Open an issue with the "Feature Request" label
2. Describe the feature and its Islamic benefit
3. Explain how it helps users maintain their faith

### Pull Requests

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

## 📝 Code Style

### Kotlin Style Guide

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable names
- Add documentation for public APIs
- Keep functions focused and small

### Example

```kotlin
/**
 * Analyzes bitmap content for inappropriate material
 * @param bitmap The screenshot to analyze
 * @param settings Current app settings
 * @return Analysis result with detection confidence
 */
suspend fun analyzeContent(
    bitmap: Bitmap,
    settings: AppSettings
): ContentAnalysisResult {
    // Implementation
}
```

## 🧪 Testing

### Unit Tests

```bash
./gradlew testDebugUnitTest
```

### Before Submitting PR

- [ ] All tests pass
- [ ] Code follows style guidelines
- [ ] Documentation updated
- [ ] No sensitive data exposed
- [ ] Tested on real device

## 🔒 Security

### Reporting Security Issues

**DO NOT** open public issues for security vulnerabilities.

Instead, email: security@haramblur.app

Include:
- Description of vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

## 📚 Areas for Contribution

### High Priority
- Performance optimization
- Battery usage reduction
- ML model improvements
- Accessibility enhancements

### Medium Priority
- Additional language support
- UI/UX improvements
- Documentation
- Test coverage

### Documentation
- User guides
- API documentation
- Translation of app content
- Video tutorials

## 🎯 Review Process

1. All PRs require review from maintainers
2. Automated tests must pass
3. Code review for quality and security
4. Approval from at least 1 maintainer

## 💬 Communication

- Be respectful and constructive
- Focus on the code, not the person
- Ask questions if unclear
- Help other contributors

## 🙏 Recognition

Contributors will be:
- Listed in CONTRIBUTORS.md
- Mentioned in release notes
- Thanked in the app (optional)

---

**May Allah reward your efforts**

*For questions, contact: contribute@haramblur.app*
