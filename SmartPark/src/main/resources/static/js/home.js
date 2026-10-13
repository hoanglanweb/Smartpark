(() => {
  const slots = Array.from(document.querySelectorAll(".parking-slot"));
  const filters = Array.from(document.querySelectorAll("[data-filter]"));
  const selection = new Map();
  const selectedList = document.getElementById("selected-list");
  const selectedBadge = document.getElementById("selected-badge");
  const clearButton = document.getElementById("clear-selected");

  function renderSelection() {
    selectedList.replaceChildren();
    selectedBadge.textContent = String(selection.size);
    clearButton.disabled = selection.size === 0;

    if (selection.size === 0) {
      const emptyMessage = document.createElement("p");
      emptyMessage.className = "empty-selection";
      emptyMessage.textContent = "Chọn một vị trí còn trống trên sơ đồ.";
      selectedList.append(emptyMessage);
    } else {
      selection.forEach((slot) => {
        const item = document.createElement("div");
        item.className = "selected-item";

        const label = document.createElement("span");
        label.textContent = `${slot.code} | Khu ${slot.zone}`;

        const removeButton = document.createElement("button");
        removeButton.type = "button";
        removeButton.setAttribute("aria-label", `Bỏ chọn ${slot.code}`);
        removeButton.textContent = "×";
        removeButton.addEventListener("click", () => setSelected(slot, false));

        item.append(label, removeButton);
        selectedList.append(item);
      });
    }

    slots.forEach((slot) => {
      const isSelected = selection.has(slot.dataset.code);
      slot.classList.toggle("is-selected", isSelected);
      slot.setAttribute("aria-pressed", String(isSelected));
    });
  }

  function setSelected(slot, selected) {
    if (selected) {
      selection.set(slot.dataset.code, {
        code: slot.dataset.code,
        zone: slot.dataset.zone,
      });
    } else {
      selection.delete(slot.dataset.code);
    }
    renderSelection();
  }

  function applyFilter(filter) {
    slots.forEach((slot) => {
      const matches = filter === "all"
        || (filter === "zoneA" && slot.dataset.zone.toLowerCase() === "a")
        || (filter === "zoneB" && slot.dataset.zone.toLowerCase() === "b")
        || (filter === "vip" && slot.dataset.type === "VIP")
        || (filter === "empty" && slot.dataset.status === "AVAILABLE");

      slot.classList.toggle("is-filtered-out", !matches);
    });

    document.querySelectorAll("[data-parking-zone]").forEach((zone) => {
      const visibleSlots = zone.querySelectorAll(".parking-slot:not(.is-filtered-out)");
      zone.classList.toggle("is-filtered-out", visibleSlots.length === 0);
    });

    filters.forEach((button) => {
      const isActive = button.dataset.filter === filter;
      button.classList.toggle("is-active", isActive);
      button.setAttribute("aria-pressed", String(isActive));
    });
  }

  slots.forEach((slot) => {
    slot.addEventListener("click", () => {
      if (slot.dataset.status === "AVAILABLE") {
        setSelected(slot, !selection.has(slot.dataset.code));
      }
    });
  });

  filters.forEach((button) => {
    button.addEventListener("click", () => applyFilter(button.dataset.filter));
  });

  clearButton.addEventListener("click", () => {
    selection.clear();
    renderSelection();
  });

  renderSelection();
})();
