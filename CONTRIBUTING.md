# 🤝 Katkıda Bulunma Rehberi

Aile Takip Sistemi'ne katkıda bulunduğunuz için teşekkür ederiz! Bu rehber, projeye nasıl katkıda bulunabileceğinizi açıklar.

## 🚀 Hızlı Başlangıç

### Gereksinimler
- Node.js 16+
- npm 8+
- Git
- Gmail hesabı (email testi için)

### Geliştirme Ortamı Kurulumu
```bash
# 1. Repository'yi fork edin ve klonlayın
git clone https://github.com/YOUR_USERNAME/aile-takip-sistemi.git
cd aile-takip-sistemi

# 2. Tüm bağımlılıkları yükleyin
npm run install:all

# 3. Environment dosyasını oluşturun
cp backend/.env.example backend/.env
# .env dosyasını Gmail bilgilerinizle düzenleyin

# 4. Geliştirme sunucularını başlatın
npm run dev
```

## 📋 Katkı Türleri

### 🐛 Bug Reports
- **GitHub Issues** kullanın
- **Detaylı açıklama** ekleyin
- **Adım adım reproduksiyon** sağlayın
- **Ekran görüntüleri** ekleyin
- **Browser/OS bilgisi** belirtin

### ✨ Feature Requests
- **Use case** açıklayın
- **Mockup/wireframe** ekleyin
- **Teknik detaylar** sağlayın
- **Alternatif çözümler** önerin

### 🔧 Code Contributions
- **Feature branch** oluşturun
- **Clean commits** yapın
- **Tests** ekleyin
- **Documentation** güncelleyin

## 🏗️ Development Workflow

### Branch Naming
```
feature/new-feature-name
bugfix/issue-description
hotfix/critical-fix
docs/documentation-update
```

### Commit Messages
```
feat: add user profile management
fix: resolve email sending issue
docs: update API documentation
style: improve button hover effects
refactor: optimize database queries
test: add unit tests for auth service
```

### Pull Request Process
1. **Fork** repository
2. **Create** feature branch
3. **Make** changes
4. **Test** thoroughly
5. **Update** documentation
6. **Submit** PR with description

## 🧪 Testing Guidelines

### Backend Testing
```bash
cd backend
npm test                    # Run all tests
npm run test:unit          # Unit tests
npm run test:integration   # Integration tests
```

### Frontend Testing
```bash
cd frontend
npm test                   # Run all tests
npm run test:coverage     # Coverage report
```

### Manual Testing Checklist
- [ ] Login/logout functionality
- [ ] CRUD operations for all modules
- [ ] Email sending/receiving
- [ ] Responsive design
- [ ] Cross-browser compatibility
- [ ] Error handling

## 📝 Code Style Guidelines

### TypeScript Standards
- **Strict mode** enabled
- **Interface** over type aliases
- **Explicit return types** for functions
- **Proper error handling**

### React Best Practices
- **Functional components** with hooks
- **Proper state management** with Zustand
- **Memoization** for performance
- **Accessibility** compliance

### CSS/Styling
- **CSS-in-JS** approach
- **Responsive design** first
- **Consistent spacing** (8px grid)
- **Modern animations** (60fps)

### Backend Standards
- **RESTful API** design
- **Proper HTTP status codes**
- **Input validation**
- **Error middleware**
- **Security best practices**

## 🔒 Security Guidelines

### Authentication
- **JWT tokens** for session management
- **Bcrypt** for password hashing
- **Rate limiting** for API endpoints
- **Input sanitization**

### Data Protection
- **SQL injection** prevention
- **XSS protection**
- **CORS** configuration
- **Environment variables** for secrets

## 📚 Documentation Standards

### Code Documentation
- **JSDoc** comments for functions
- **README** updates for new features
- **API documentation** for endpoints
- **Component documentation** for UI

### Commit Documentation
- **Clear commit messages**
- **PR descriptions** with context
- **Breaking changes** highlighted
- **Migration guides** when needed

## 🎨 UI/UX Guidelines

### Design Principles
- **Minimalist** approach
- **Consistent** visual hierarchy
- **Accessible** color contrast
- **Intuitive** navigation

### Component Standards
- **Reusable** components
- **Props interface** documentation
- **Responsive** behavior
- **Loading states**

## 🚀 Performance Guidelines

### Frontend Performance
- **Bundle size** optimization
- **Lazy loading** for routes
- **Image optimization**
- **Caching strategies**

### Backend Performance
- **Database query** optimization
- **Response time** monitoring
- **Memory usage** optimization
- **Concurrent request** handling

## 🌐 Internationalization

### Multi-language Support
- **i18n** implementation ready
- **String externalization**
- **Date/time formatting**
- **RTL support** consideration

## 📱 Mobile Responsiveness

### Responsive Design
- **Mobile-first** approach
- **Touch-friendly** interfaces
- **Viewport** optimization
- **Performance** on mobile

## 🔄 CI/CD Guidelines

### Automated Testing
- **Unit tests** on PR
- **Integration tests** on merge
- **E2E tests** on release
- **Performance tests** periodically

### Deployment Process
- **Staging** environment testing
- **Production** deployment checklist
- **Rollback** procedures
- **Monitoring** setup

## 📞 Getting Help

### Communication Channels
- **GitHub Issues**: Bug reports, feature requests
- **GitHub Discussions**: General questions
- **Email**: erhan.koksal@gmail.com
- **Response Time**: 24-48 hours

### Resources
- **Documentation**: Comprehensive guides
- **Code Examples**: Working samples
- **Best Practices**: Industry standards
- **Community**: Developer support

## 🏆 Recognition

### Contributors
- **GitHub contributors** page
- **Release notes** mentions
- **Special thanks** section
- **Community highlights**

### Rewards
- **Open source** portfolio building
- **Learning** opportunities
- **Networking** with developers
- **Real-world** project experience

## 📋 Checklist Template

### Before Submitting PR
- [ ] Code follows style guidelines
- [ ] Tests pass locally
- [ ] Documentation updated
- [ ] No console errors/warnings
- [ ] Responsive design tested
- [ ] Accessibility checked
- [ ] Performance impact assessed
- [ ] Security implications reviewed

### PR Description Template
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Manual testing completed
- [ ] Cross-browser testing done

## Screenshots
(if applicable)

## Additional Notes
Any additional context or notes
```

---

**Teşekkürler!** 🙏

Katkılarınız projeyi daha iyi hale getiriyor. Her türlü katkı değerlidir - kod, dokümantasyon, test, tasarım, geri bildirim.

*Son güncelleme: 26 Ocak 2026*