
document.addEventListener("DOMContentLoaded", () => {
  const targets = document.querySelectorAll(
    ".landing-benefits, .benefit-item, .landing-process, " +
    ".process-list li, .landing-closing"
  );

  if (
    window.matchMedia("(prefers-reduced-motion: reduce)").matches ||
    !("IntersectionObserver" in window)
  ) {
    return;
  }

  targets.forEach((element) => {
    element.classList.add("motion-pending");
  });

  const observer = new IntersectionObserver(
    (entries, currentObserver) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;

        entry.target.classList.add("motion-visible");
        currentObserver.unobserve(entry.target);
      });
    },
    {
      threshold: 0.12,
      rootMargin: "0px 0px -35px 0px",
    }
  );

  targets.forEach((element) => observer.observe(element));
});
