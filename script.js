// Mobile navigation
const menuButton = document.querySelector(".menu-toggle");
const nav = document.querySelector(".topbar nav");
menuButton?.addEventListener("click", () => {
  const isOpen = nav.classList.toggle("open");
  menuButton.setAttribute("aria-expanded", String(isOpen));
});
nav?.querySelectorAll("a").forEach(link => link.addEventListener("click", () => {
  nav.classList.remove("open");
  menuButton?.setAttribute("aria-expanded", "false");
}));

// Privacy-friendly public counter. The key is specific to this website.
(async function countPageView() {
  const output = document.getElementById("view-count");
  if (!output) return;
  try {
    const response = await fetch("https://api.counterapi.dev/v1/shivangsagar-presencia/site-views/up");
    if (!response.ok) throw new Error("Counter unavailable");
    const data = await response.json();
    const count = data.count ?? data.value ?? data;
    output.textContent = Number(count).toLocaleString("en-IN");
  } catch {
    output.textContent = "—";
    output.title = "The view counter service is temporarily unavailable.";
  }
})();
