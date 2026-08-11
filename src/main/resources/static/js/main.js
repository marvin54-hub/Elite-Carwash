document.addEventListener('DOMContentLoaded', () => {
    // Smooth scrolling for navigation links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            document.querySelector(this.getAttribute('href')).scrollIntoView({
                behavior: 'smooth'
            });
        });
    });

    // Simple scroll reveal
    const observerOptions = {
        threshold: 0.1
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('animate-up');
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);

    document.querySelectorAll('.service-card').forEach(card => {
        observer.observe(card);
    });

    // Form submission feedback
    const form = document.querySelector('.booking-form');
    if (form) {
        form.addEventListener('submit', () => {
            const btn = form.querySelector('button');
            btn.innerHTML = 'Processing...';
            btn.style.opacity = '0.7';
        });
    }
});
