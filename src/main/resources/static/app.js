(function () {
  "use strict";

  var tabButtons = document.querySelectorAll(".tab-button");
  tabButtons.forEach(function (button) {
    button.addEventListener("click", function () {
      var panelId = button.getAttribute("data-panel");
      tabButtons.forEach(function (tab) {
        var isSelected = tab === button;
        tab.classList.toggle("is-active", isSelected);
        tab.setAttribute("aria-selected", String(isSelected));
      });
      document.querySelectorAll(".panel").forEach(function (panel) {
        panel.classList.toggle("is-active", panel.id === panelId);
      });
    });
  });

  function toIsoUtc(datetimeLocalValue) {
    if (!datetimeLocalValue) {
      return null;
    }
    var localDate = new Date(datetimeLocalValue);
    if (isNaN(localDate.getTime())) {
      return null;
    }
    return localDate.toISOString();
  }

  function clearFieldErrors(prefix) {
    var fields = ["originalUrl", "customAlias", "expiresAt"];
    fields.forEach(function (field) {
      var el = document.getElementById(prefix + "-" + field);
      if (el) {
        el.textContent = "";
      }
    });
  }

  function setFormMessage(el, text, type) {
    el.textContent = text || "";
    el.classList.remove("error", "success");
    if (type) {
      el.classList.add(type);
    }
  }

  function applyApiError(messageEl, fieldErrorPrefix, status, errorBody) {
    var message = "Request failed.";
    if (errorBody && errorBody.message) {
      message = errorBody.message;
    } else if (status === 409) {
      message = "That alias is already in use.";
    } else if (status === 410) {
      message = "This short link has expired.";
    } else if (status === 404) {
      message = "Short code not found.";
    }
    setFormMessage(messageEl, status + ": " + message, "error");

    if (fieldErrorPrefix && errorBody && errorBody.fieldErrors) {
      Object.keys(errorBody.fieldErrors).forEach(function (field) {
        var el = document.getElementById(fieldErrorPrefix + "-" + field);
        if (el) {
          el.textContent = errorBody.fieldErrors[field];
        }
      });
    }
  }

  async function parseJsonSafe(response) {
    try {
      return await response.json();
    } catch (err) {
      return null;
    }
  }

  function formatInstant(value) {
    if (!value) {
      return "—";
    }
    return value;
  }

  // Create short URL
  var createForm = document.getElementById("create-form");
  var createMessage = document.getElementById("create-message");
  var createResult = document.getElementById("create-result");

  createForm.addEventListener("submit", async function (event) {
    event.preventDefault();
    setFormMessage(createMessage, "", null);
    clearFieldErrors("error");
    createResult.hidden = true;

    var originalUrl = document.getElementById("original-url").value.trim();
    var customAlias = document.getElementById("custom-alias").value.trim();
    var expiresAtLocal = document.getElementById("expires-at").value;

    var payload = {
      originalUrl: originalUrl
    };
    if (customAlias) {
      payload.customAlias = customAlias;
    }
    if (expiresAtLocal) {
      var isoExpiresAt = toIsoUtc(expiresAtLocal);
      if (!isoExpiresAt) {
        setFormMessage(createMessage, "Invalid expiration date/time.", "error");
        return;
      }
      payload.expiresAt = isoExpiresAt;
    }

    try {
      var response = await fetch("/api/v1/urls", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        var errorBody = await parseJsonSafe(response);
        applyApiError(createMessage, "error", response.status, errorBody);
        return;
      }

      var data = await response.json();

      document.getElementById("result-shortUrl").textContent = data.shortUrl;
      document.getElementById("result-shortCode").textContent = data.shortCode;
      document.getElementById("result-createdAt").textContent = formatInstant(data.createdAt);
      document.getElementById("result-expiresAt").textContent = formatInstant(data.expiresAt);

      var openLink = document.getElementById("open-link");
      openLink.href = data.shortUrl;

      createResult.hidden = false;
      setFormMessage(createMessage, "Short URL created.", "success");
      loadManagedLinks();
    } catch (err) {
      setFormMessage(createMessage, "Network error while creating the short URL.", "error");
    }
  });

  var copyButton = document.getElementById("copy-button");
  copyButton.addEventListener("click", async function () {
    var shortUrl = document.getElementById("result-shortUrl").textContent;
    if (!shortUrl) {
      return;
    }
    try {
      await navigator.clipboard.writeText(shortUrl);
      var original = copyButton.textContent;
      copyButton.textContent = "Copied!";
      setTimeout(function () {
        copyButton.textContent = original;
      }, 1500);
    } catch (err) {
      setFormMessage(createMessage, "Could not copy to clipboard.", "error");
    }
  });

  var managedLinks = [];
  var linksTableBody = document.getElementById("links-table-body");
  var linksEmpty = document.getElementById("links-empty");
  var manageMessage = document.getElementById("manage-message");
  var linksSearch = document.getElementById("links-search");
  var analyticsSelect = document.getElementById("analytics-short-code");

  function displayDate(value) {
    return value ? new Date(value).toLocaleString() : "-";
  }

  function populateAnalyticsSelect(links) {
    analyticsSelect.innerHTML = '<option value="">-- select --</option>';
    links.forEach(function (link) {
      var option = document.createElement("option");
      option.value = link.shortCode;
      option.textContent = link.shortCode + " (" + link.originalUrl + ")";
      analyticsSelect.appendChild(option);
    });
  }

  function renderManagedLinks() {
    var query = linksSearch.value.trim().toLowerCase();
    var visibleLinks = managedLinks.filter(function (link) {
      return !query || link.shortCode.toLowerCase().includes(query)
        || link.originalUrl.toLowerCase().includes(query);
    });
    linksTableBody.innerHTML = "";
    linksEmpty.hidden = visibleLinks.length !== 0;
    visibleLinks.forEach(function (link) {
      var row = document.createElement("tr");
      var shortUrlCell = document.createElement("td");
      var shortUrl = document.createElement("a");
      shortUrl.href = link.shortUrl;
      shortUrl.target = "_blank";
      shortUrl.rel = "noopener noreferrer";
      shortUrl.textContent = link.shortUrl;
      shortUrlCell.appendChild(shortUrl);
      row.appendChild(shortUrlCell);
      [link.originalUrl, link.clickCount, displayDate(link.createdAt), displayDate(link.expiresAt)]
        .forEach(function (value) {
          var cell = document.createElement("td");
          cell.textContent = String(value);
          row.appendChild(cell);
        });
      var statusCell = document.createElement("td");
      var status = document.createElement("span");
      status.className = "status-badge " + (link.active ? "status-active" : "status-inactive");
      status.textContent = link.active ? "Active" : "Disabled";
      statusCell.appendChild(status);
      row.insertBefore(statusCell, row.children[1]);
      var actions = document.createElement("td");
      actions.className = "action-cell";
      actions.appendChild(actionButton("Edit", "edit", link.shortCode));
      if (link.active) {
        actions.appendChild(actionButton("Disable", "disable", link.shortCode));
      }
      actions.appendChild(actionButton("Delete", "delete", link.shortCode));
      row.appendChild(actions);
      linksTableBody.appendChild(row);
    });
  }

  function actionButton(label, action, shortCode) {
    var button = document.createElement("button");
    button.type = "button";
    button.className = "table-action " + (action === "delete" ? "danger-action" : "");
    button.dataset.action = action;
    button.dataset.shortCode = shortCode;
    button.textContent = label;
    return button;
  }

  async function loadManagedLinks() {
    try {
      var response = await fetch("/api/v1/urls");
      if (!response.ok) {
        throw new Error("Unable to load links");
      }
      managedLinks = await response.json();
      renderManagedLinks();
      populateAnalyticsSelect(managedLinks);
    } catch (err) {
      setFormMessage(manageMessage, "Unable to load shortened links.", "error");
    }
  }

  document.getElementById("refresh-links-button").addEventListener("click", loadManagedLinks);
  linksSearch.addEventListener("input", renderManagedLinks);
  linksTableBody.addEventListener("click", async function (event) {
    var button = event.target.closest("button[data-action]");
    if (!button) {
      return;
    }
    var action = button.dataset.action;
    var shortCode = encodeURIComponent(button.dataset.shortCode);
    if (action === "edit") {
      var existingLink = managedLinks.find(function (link) {
        return link.shortCode === button.dataset.shortCode;
      });
      var updatedUrl = window.prompt("Destination URL", existingLink.originalUrl);
      if (!updatedUrl) {
        return;
      }
      var updatedExpiry = window.prompt("Expiration (ISO-8601, optional)", existingLink.expiresAt || "");
      try {
        var editResponse = await fetch("/api/v1/urls/" + shortCode, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            originalUrl: updatedUrl,
            expiresAt: updatedExpiry || null
          })
        });
        if (!editResponse.ok) {
          throw new Error("Request failed");
        }
        await loadManagedLinks();
      } catch (err) {
        setFormMessage(manageMessage, "Could not edit this link.", "error");
      }
      return;
    }
    if (action === "delete" && !window.confirm("Delete this short link permanently?")) {
      return;
    }
    try {
      var response = await fetch("/api/v1/urls/" + shortCode + (action === "delete" ? "/permanent" : ""), {
        method: "DELETE"
      });
      if (!response.ok) {
        throw new Error("Request failed");
      }
      await loadManagedLinks();
    } catch (err) {
      setFormMessage(manageMessage, "Could not update this link.", "error");
    }
  });

  tabButtons.forEach(function (button) {
    button.addEventListener("click", function () {
      if (button.dataset.panel === "manage-panel" || button.dataset.panel === "analytics-panel") {
        loadManagedLinks();
      }
    });
  });

  var analyticsMessage = document.getElementById("analytics-message");
  var analyticsContent = document.getElementById("analytics-content");
  var analyticsEmpty = document.getElementById("analytics-empty");

  function renderDistribution(chartId, legendId, values) {
    var chart = document.getElementById(chartId);
    var legend = document.getElementById(legendId);
    var entries = Object.keys(values || {}).sort(function (a, b) {
      return values[b] - values[a];
    });
    var total = entries.reduce(function (sum, key) { return sum + values[key]; }, 0);
    var colors = ["#1429bd", "#5267db", "#8291e2", "#aeb8ed", "#d2d8f6"];
    var position = 0;
    var stops = [];
    legend.innerHTML = "";
    entries.forEach(function (key, index) {
      var next = position + (values[key] / Math.max(total, 1)) * 100;
      stops.push(colors[index % colors.length] + " " + position + "% " + next + "%");
      position = next;
      var item = document.createElement("li");
      item.innerHTML = '<span class="legend-swatch" style="background:' + colors[index % colors.length] + '"></span>'
        + '<span>' + key + '</span><strong>' + values[key] + '</strong>';
      legend.appendChild(item);
    });
    chart.style.background = entries.length ? "conic-gradient(" + stops.join(", ") + ")" : "#e5e8f4";
    if (!entries.length) {
      legend.innerHTML = "<li>No data yet.</li>";
    }
  }

  function renderClickChart(total) {
    var chart = document.getElementById("click-chart");
    chart.innerHTML = "";
    var bar = document.createElement("div");
    bar.className = "click-bar";
    bar.style.height = Math.max(total ? 12 : 2, Math.min(100, total * 24)) + "%";
    bar.title = total + " click(s)";
    var label = document.createElement("span");
    label.textContent = new Date().toISOString().slice(0, 10);
    bar.appendChild(label);
    chart.appendChild(bar);
  }

  async function loadAnalytics(shortCode) {
    shortCode = shortCode || document.getElementById("analytics-short-code").value.trim();
    if (!shortCode) {
      analyticsContent.hidden = true;
      analyticsEmpty.hidden = false;
      return;
    }

    try {
      var response = await fetch("/api/v1/urls/" + encodeURIComponent(shortCode) + "/click-analytics");

      if (!response.ok) {
        var errorBody = await parseJsonSafe(response);
        applyApiError(analyticsMessage, null, response.status, errorBody);
        return;
      }

      var data = await response.json();
      document.getElementById("analytics-total-clicks").textContent = String(data.totalEvents);
      document.getElementById("analytics-unique-visitors").textContent = String(data.uniqueVisitors);
      renderClickChart(data.totalEvents);
      renderDistribution("device-chart", "device-legend", data.byDevice);
      renderDistribution("browser-chart", "browser-legend", data.byBrowser);
      renderDistribution("os-chart", "os-legend", data.byOperatingSystem);
      renderDistribution("country-chart", "country-legend", data.byCountry);
      analyticsContent.hidden = false;
      analyticsEmpty.hidden = true;
      setFormMessage(analyticsMessage, "", null);
    } catch (err) {
      setFormMessage(analyticsMessage, "Network error while fetching analytics.", "error");
    }
  }

  analyticsSelect.addEventListener("change", function () {
    loadAnalytics(analyticsSelect.value);
  });

  loadManagedLinks();
})();
