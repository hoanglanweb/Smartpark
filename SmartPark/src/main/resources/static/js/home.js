(() => {
  const dialog = document.getElementById("guest-reservation-dialog");
  const bookingForm = document.getElementById("guest-reservation-form");
  const bookingButton = document.getElementById("open-booking");
  const checkInDialog = document.getElementById("check-in-dialog");
  const checkInButton = document.getElementById("open-check-in");
  const checkOutButton = document.getElementById("submit-check-out");
  const checkOutSlotId = document.getElementById("check-out-slot-id");
  const checkInSlotId = document.getElementById("check-in-slot-id");
  const checkInSlotCode = document.getElementById("check-in-slot-code");
  const closeCheckInButton = document.getElementById("close-check-in");
  const cancelCheckInButton = document.getElementById("cancel-check-in");
  const closeButton = document.getElementById("close-booking");
  const cancelButton = document.getElementById("cancel-booking");
  const submitButton = document.getElementById("submit-booking");
  const slotIdInput = document.getElementById("booking-slot-id");
  const selectedSlotLabel = document.getElementById("selected-slot-label");
  const selectionHint = document.getElementById("slot-selection-hint");
  const dialogSlotCode = document.getElementById("dialog-slot-code");
  const errorMessage = document.getElementById("booking-error");
  const successMessage = document.getElementById("booking-success");
  const slotButtons = [
    ...document.querySelectorAll(".parking-slot[data-slot-id]"),
  ];

  if (
    !dialog ||
    !bookingForm ||
    !bookingButton ||
    !slotIdInput
  ) {
    return;
  }

  let selectedSlot = null;

  // Hiển thị thông báo
  const displayMessage = (element, message) => {
    if (!element) return;

    element.textContent = message;
    element.hidden = false;
  };

  // Xóa thông báo cũ
  const clearResponseMessages = () => {
    if (errorMessage) {
      errorMessage.hidden = true;
      errorMessage.textContent = "";
    }

    if (successMessage) {
      successMessage.hidden = true;
      successMessage.textContent = "";
    }
  };

  // Bỏ chọn ô hiện tại
  const deselectSlot = () => {
    slotButtons.forEach((slotButton) => {
      slotButton.setAttribute("aria-pressed", "false");
    });

    selectedSlot = null;

    slotIdInput.value = "";
    if (checkOutSlotId) {
      checkOutSlotId.value = "";
    }
    if (checkInSlotId) {
      checkInSlotId.value = "";
    }

    if (dialogSlotCode) {
      dialogSlotCode.textContent = "";
    }
    if (checkInSlotCode) {
      checkInSlotCode.textContent = "";
    }

    if (selectedSlotLabel) {
      selectedSlotLabel.textContent = "";
      selectedSlotLabel.hidden = true;
    }

    if (selectionHint) {
      selectionHint.hidden = false;
    }

    // Không cho đặt chỗ khi chưa chọn ô
    bookingButton.disabled = true;
    if (checkInButton) {
      checkInButton.disabled = true;
    }
    if (checkOutButton) {
      checkOutButton.disabled = true;
    }
  };

  // Chọn hoặc bỏ chọn ô đỗ
  const selectSlot = (button) => {
    if (button.disabled) {
      return;
    }

    const slotId = button.dataset.slotId;

    // Click lần thứ hai vào chính ô đang chọn -> bỏ chọn
    if (selectedSlot && selectedSlot.id === slotId) {
      deselectSlot();
      return;
    }

    // Chuyển lựa chọn sang ô mới
    slotButtons.forEach((slotButton) => {
      slotButton.setAttribute(
        "aria-pressed",
        String(slotButton === button)
      );
    });

    const slotCodeElement = button.querySelector(".slot-code");

    selectedSlot = {
      id: slotId,
      code: slotCodeElement
        ? slotCodeElement.textContent.trim()
        : slotId,
      status: button.classList.contains("status--occupied")
        ? "OCCUPIED"
        : "AVAILABLE",
    };

    slotIdInput.value = selectedSlot.id;
    if (checkOutSlotId) {
      checkOutSlotId.value = selectedSlot.id;
    }
    if (checkInSlotId) {
      checkInSlotId.value = selectedSlot.id;
    }

    if (dialogSlotCode) {
      dialogSlotCode.textContent = selectedSlot.code;
    }
    if (checkInSlotCode) {
      checkInSlotCode.textContent = selectedSlot.code;
    }

    if (selectedSlotLabel) {
      selectedSlotLabel.textContent =
        `Vị trí đã chọn: ${selectedSlot.code}`;
      selectedSlotLabel.hidden = false;
    }

    if (selectionHint) {
      selectionHint.hidden = true;
    }

    bookingButton.disabled = selectedSlot.status !== "AVAILABLE";
    if (checkInButton) {
      checkInButton.disabled = selectedSlot.status !== "AVAILABLE";
    }
    if (checkOutButton) {
      checkOutButton.disabled = selectedSlot.status !== "OCCUPIED";
    }

    clearResponseMessages();
  };

  // Click trực tiếp vào ô trên sơ đồ
  document.querySelector(".parking-map")?.addEventListener(
    "click",
    (event) => {
      const target = event.target;

      if (!(target instanceof Element)) {
        return;
      }

      const button = target.closest(
        ".parking-slot[data-slot-id]"
      );

      if (button instanceof HTMLButtonElement) {
        selectSlot(button);
      }
    }
  );

  // Mở modal đặt chỗ
  bookingButton.addEventListener("click", () => {
    if (!selectedSlot) {
      if (selectionHint) {
        displayMessage(
          selectionHint,
          "Vui lòng chọn một ô đỗ trước khi đặt chỗ."
        );
      }

      return;
    }

    if (typeof dialog.showModal !== "function") {
      displayMessage(
        errorMessage,
        "Trình duyệt không hỗ trợ modal đặt chỗ."
      );
      return;
    }

    clearResponseMessages();

    // Đồng bộ lại ID ô được chọn
    slotIdInput.value = selectedSlot.id;

    if (dialogSlotCode) {
      dialogSlotCode.textContent = selectedSlot.code;
    }

    initializeBookingTimes();
    dialog.showModal();

    bookingForm
      .querySelector("input:not([type='hidden'])")
      ?.focus();
  });

  checkInButton?.addEventListener("click", () => {
    if (!selectedSlot || selectedSlot.status !== "AVAILABLE") {
      return;
    }
    if (
      !checkInDialog ||
      !checkInSlotId ||
      !checkInSlotCode ||
      typeof checkInDialog.showModal !== "function"
    ) {
      displayMessage(selectionHint, "Trình duyệt không hỗ trợ cửa sổ ghi nhận xe vào.");
      return;
    }

    checkInSlotId.value = selectedSlot.id;
    checkInSlotCode.textContent = selectedSlot.code;
    checkInDialog.showModal();
    document.getElementById("check-in-plate")?.focus();
  });

  const closeCheckInDialog = () => {
    if (checkInDialog?.open) {
      checkInDialog.close();
    }
  };
  closeCheckInButton?.addEventListener("click", closeCheckInDialog);
  cancelCheckInButton?.addEventListener("click", closeCheckInDialog);
  checkInDialog?.addEventListener("close", () => checkInButton?.focus());

  // Đóng modal
  const closeDialog = () => {
    if (dialog.open) {
      dialog.close();
    }
  };

  closeButton?.addEventListener("click", closeDialog);
  cancelButton?.addEventListener("click", closeDialog);

  // Khi đóng modal, trả focus về nút đặt chỗ
  dialog.addEventListener("close", () => {
    bookingButton.focus();
  });

  // Hiển thị thông báo sau khi đặt chỗ thành công
  const notice = sessionStorage.getItem(
    "smartpark-reservation-notice"
  );

  const pageNotice = document.getElementById(
    "reservation-notice"
  );

  if (notice && pageNotice) {
    pageNotice.textContent = notice;
    pageNotice.hidden = false;

    sessionStorage.removeItem("smartpark-reservation-notice");
  }

  // Không cho chọn ngày trong quá khứ
  const dateInput = document.getElementById("guest-date");

  if (dateInput) {
    const today = new Date();

    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, "0");
    const day = String(today.getDate()).padStart(2, "0");

    dateInput.min = `${year}-${month}-${day}`;
  }

  const initializeBookingTimes = () => {
    const startInput = document.getElementById("guest-start");
    const endInput = document.getElementById("guest-end");

    if (!dateInput || !startInput || !endInput) {
      return;
    }

    const now = new Date();
    const today = [
      now.getFullYear(),
      String(now.getMonth() + 1).padStart(2, "0"),
      String(now.getDate()).padStart(2, "0"),
    ].join("-");
    const currentTime = [
      String(now.getHours()).padStart(2, "0"),
      String(now.getMinutes()).padStart(2, "0"),
    ].join(":");

    if (!dateInput.value) {
      dateInput.value = today;
    }
    if (!startInput.value) {
      startInput.value = currentTime;
    }
    if (!endInput.value) {
      const end = new Date(now.getTime() + 60 * 60 * 1000);
      const endDate = [
        end.getFullYear(),
        String(end.getMonth() + 1).padStart(2, "0"),
        String(end.getDate()).padStart(2, "0"),
      ].join("-");

      if (endDate === today) {
        endInput.value = [
          String(end.getHours()).padStart(2, "0"),
          String(end.getMinutes()).padStart(2, "0"),
        ].join(":");
      }
    }
  };

  // Gửi form Guest Reservation
  bookingForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    clearResponseMessages();

    if (!selectedSlot) {
      displayMessage(
        errorMessage,
        "Vui lòng chọn ô đỗ trước khi gửi yêu cầu."
      );
      return;
    }

    if (!bookingForm.reportValidity()) {
      return;
    }

    const startInput = document.getElementById("guest-start");
    const endInput = document.getElementById("guest-end");

    if (!startInput || !endInput) {
      displayMessage(
        errorMessage,
        "Không tìm thấy trường thời gian đặt chỗ."
      );
      return;
    }

    const start = startInput.value;
    const end = endInput.value;

    if (!start || !end) {
      displayMessage(
        errorMessage,
        "Vui lòng nhập đầy đủ thời gian bắt đầu và kết thúc."
      );
      return;
    }

    if (end <= start) {
      displayMessage(
        errorMessage,
        "Giờ kết thúc phải sau giờ bắt đầu."
      );
      return;
    }

    if (!bookingForm.action || !bookingForm.method) {
      displayMessage(
        errorMessage,
        "Form chưa được cấu hình địa chỉ gửi yêu cầu."
      );
      return;
    }

    submitButton.disabled = true;
    submitButton.textContent = "Đang gửi...";

    try {
      // Đảm bảo form gửi đúng ID ô đang chọn
      slotIdInput.value = selectedSlot.id;

      const response = await fetch(bookingForm.action, {
        method: bookingForm.method.toUpperCase(),
        headers: {
          Accept: "application/json",
          "Content-Type":
            "application/x-www-form-urlencoded;charset=UTF-8",
        },
        body: new URLSearchParams(
          new FormData(bookingForm)
        ),
        credentials: "same-origin",
      });

      const contentType =
        response.headers.get("content-type") || "";

      // Backend phải trả JSON; không parse trang lỗi HTML như JSON
      if (!contentType.includes("application/json")) {
        throw new Error(
          "Máy chủ không trả về JSON hợp lệ. Vui lòng kiểm tra lỗi backend."
        );
      }

      const result = await response.json();

      if (!response.ok || !result.success) {
        displayMessage(
          errorMessage,
          result.message || "Không thể gửi yêu cầu đặt chỗ."
        );
        return;
      }

      // Lưu thông báo để hiển thị sau khi tải lại trang
      sessionStorage.setItem(
        "smartpark-reservation-notice",
        result.message || "Đặt chỗ thành công."
      );

      window.location.reload();
    } catch (error) {
      console.error("Guest reservation error:", error);

      displayMessage(
        errorMessage,
        error instanceof Error
          ? error.message
          : "Không thể kết nối đến máy chủ. Vui lòng thử lại."
      );
    } finally {
      submitButton.disabled = false;
      submitButton.textContent = "Gửi yêu cầu";
    }
  });

  // Khởi tạo trạng thái ban đầu: chưa chọn ô
  deselectSlot();
})(); 