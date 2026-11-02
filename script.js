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


// Page View Counter — Abacus API
(async function countPageView() {
  const output = document.getElementById("view-count");
  if (!output) return;

  try {
    const response = await fetch(
      "https://abacus.jasoncameron.dev/hit/shivangsagar.github.io/presencia",
      { cache: "no-store" }
    );

    if (!response.ok) throw new Error("Counter unavailable");

    const data = await response.json();
    output.textContent = Number(data.value).toLocaleString("en-IN");

  } catch (error) {
    console.error("Page view counter error:", error);
    output.textContent = "—";
  }
})();
