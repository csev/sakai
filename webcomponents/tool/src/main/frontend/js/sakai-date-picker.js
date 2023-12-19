import { SakaiElement } from "./sakai-element.js";
import { html } from "./assets/lit-element/lit-element.js";
import { getTimezone } from "./sakai-portal-utils.js";
import { Temporal } from "./assets/@js-temporal/polyfill/dist/index.esm.js";

/**
 * Renders an input which, when clicked, launches a date picker.
 *
 * @example <caption>Usage:</caption>
 * <sakai-date-picker epoch-millis="345922925445" @/>
 *
 * The tag fires the event 'datetime-selected'. You'd handle that with (vanillajs):
 *
 * sakaiDatePicker.addEventListener("datetime-selected", e => console.log(e.detail.epochMillis));
 *
 * @extends LitElement
 * @property {number} [epochMillis] The milliseconds since the unix epoch to set this datetime to
 * @property {string} [isoDate] The ISO8601 string to set this datetime to
 * @property {boolean} [disabled] Disable the date controls
 * @property {string} [label] The a11y label to use for the aria-label and title attributes
 * @fires datetime-selected This event has epochMillis and epochSeconds as the detail object
 */
class SakaiDatePicker extends SakaiElement {

  constructor() {

    super();

    this.hiddenPrefix = "";
  }

  static get properties() {

    return {
      epochMillis: { attribute: "epoch-millis", type: Number },
      isoDate: { attribute: "iso-date", type: String },
      disabled: { attribute: false, type: Boolean },
      label: { type: String },
      addHiddenFields: { attribute: "add-hidden-fields", type: Boolean },
      hiddenPrefix: { attribute: "hidden-prefix", type: String },
    };
  }

  set epochMillis(value) {

    if (value) {
      this._epochMillis = value;
      const instant = Temporal.Instant.fromEpochMilliseconds(value);
      const zonedDateTime = instant.toZonedDateTimeISO({ timeZone: getTimezone() });
      this.isoDate = zonedDateTime.toString().substring(0, 16);
    } else {
      this._epochMillis = null;
      this.isoDate = null;
    }

    const inputDate = this.querySelector("input[type='datetime-local']");
    inputDate && (inputDate.value = this.isoDate);
  }

  get epochMillis() { return this._epochMillis; }

  set isoDate(value) {

    this._isoDate = value;

    if (value) {
      // Pad it out
      const split1 = value.split("T");
      if (split1.length == 2) {
        const dateBits = split1[0].split("-").map(bit => bit.length == 1 ? `0${bit}` : bit);
        const timeBits = split1[1].split(":").map(bit => bit.length == 1 ? `0${bit}` : bit);
        this._isoDate = `${dateBits.join("-")}T${timeBits.join(":")}`;
      }
    }
  }

  get isoDate() { return this._isoDate; }

  disable() { this.disabled = true; }

  enable() { this.disabled = false; }

  reset() {
    this.querySelector("input").value = (new Date()).toISOString().substring(0, 16);
  }

  dateSelected(e) {

    const temporalObj = Temporal.PlainDateTime.from(e.target.value);
    const zonedDateTime = temporalObj.toZonedDateTime({ timeZone: getTimezone() });
    const epochMillis = zonedDateTime.toInstant().epochMilliseconds;

    if (this.addHiddenFields) {
      this.isoDate = zonedDateTime.toString().substring(0, 16);
      this.requestUpdate();
    } else {
      const epochSeconds = epochMillis / 1000;
      this.dispatchEvent(new CustomEvent("datetime-selected", { detail: { epochMillis, epochSeconds }, bubbles: true }));
    }
  }

  _getHiddenFieldValue(hiddenFieldName) {

    const d = new Date(this.isoDate);

    switch (hiddenFieldName) {
      case "year":
        return d.getFullYear();
      case "month":
        return d.getMonth() + 1;
      case "day":
        return d.getDate();
      case "hour":
        return d.getHours();
      case "min":
        return d.getMinutes();
      default:
        return "na";
    }
  }

  render() {

    return html`
      <input type="datetime-local"
          @change=${this.dateSelected}
          .value=${this.isoDate}
          .disabled=${this.disabled}
          aria-label="${this.label}"
          title="${this.label}">
      ${this.addHiddenFields && this.isoDate ? html`
        ${SakaiDatePicker.hiddenFieldNames.map(h => html`
          <input type="hidden" name="${this.hiddenPrefix}${h}" value="${this._getHiddenFieldValue(h)}">
        `)}
      ` : ""}
    `;
  }
}

SakaiDatePicker.hiddenFieldNames = ["year", "month", "day", "hour", "min"];

const tagName = "sakai-date-picker";
!customElements.get(tagName) && customElements.define(tagName, SakaiDatePicker);
