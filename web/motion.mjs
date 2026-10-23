// Decorative motion never blocks navigation or the profile builder.
const reduced = matchMedia('(prefers-reduced-motion: reduce)');
if (!reduced.matches && 'IntersectionObserver' in window) {
  const observer = new IntersectionObserver(entries => {
    for (const entry of entries) if (entry.isIntersecting) {
      entry.target.classList.add('is-visible'); observer.unobserve(entry.target);
    }
  }, {threshold: 0.08});
  document.querySelectorAll('.steps article, .section-heading, .setting-section, .map-toolbar, .closing').forEach((element, i) => {
    element.classList.add('reveal'); element.style.setProperty('--reveal-delay', `${(i % 3) * 65}ms`); observer.observe(element);
  });
  document.documentElement.classList.add('motion-ready');
  reduced.addEventListener('change', () => document.documentElement.classList.remove('motion-ready'));
}
document.addEventListener('pointerdown', event => {
  const button = event.target.closest('.button, .assign-actions button');
  if (!button || button.disabled || reduced.matches) return;
  const rect = button.getBoundingClientRect(), ripple = document.createElement('span');
  ripple.className = 'tap-ripple'; ripple.setAttribute('aria-hidden', 'true');
  ripple.style.left = `${event.clientX - rect.left}px`; ripple.style.top = `${event.clientY - rect.top}px`;
  button.append(ripple); ripple.addEventListener('animationend', () => ripple.remove(), {once: true});
});
