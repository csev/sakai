import { SakaiElement } from "@sakai-ui/sakai-element";
import { html } from "lit";

export class SakaiLTIPopup extends SakaiElement {

  static properties = {

    preLaunchText: { attribute: "pre-launch-text", type: String },
    postLaunchText: { attribute: "post-launch-text", type: String },
    launchUrl: { attribute: "launch-url", type: String },
    auto: { attribute: "auto-launch", type: Boolean },
  };

  constructor() {

    super();

    const randomId = Math.floor(Math.random() * 1000000);
    this.randomId = randomId;
    this.preLaunchText = null;
    this.postLaunchText = null;
    this.auto = false;
    this.popped = false;
    this.loadTranslations("lti").then(t => {

      this._i18n = t;
      if ( this.preLaunchText == null ) this.preLaunchText = this._i18n.pre_launch_text;
      if ( this.postLaunchText == null ) this.postLaunchText = this._i18n.post_launch_text;
    });
  }

  launchPopup() {
    window.open(this.launchUrl, "_blank");
    document.getElementById("sakai-lti-popup-" + this.randomId).style.display = "none";
    document.getElementById("sakai-lti-popup-hidden-" + this.randomId).style.display = "block";
    return false;
  }

  shouldUpdate(changedProperties) {
    return changedProperties.has("preLaunchText") || changedProperties.has("postLaunchText") ||
          changedProperties.has("launchUrl") || changedProperties.has("auto");
  }

  firstUpdated() {
    if ( this.auto && ! this.popped ) {
      this.popped = true;
      setTimeout(this.launchPopup(), 1000);
    }
  }

  render() {

    return html`
      <div>
        <button id="sakai-lti-popup-${this.randomId}"
            class="btn btn-primary"
            @click=${this.launchPopup}>
          ${this.preLaunchText}
        </button>
        <button id="sakai-lti-popup-hidden-${this.randomId}"
            class="btn btn-primary"
            disabled style="display:none;">
          ${this.postLaunchText}
        </button>
      </div>
    `;
  }
}
