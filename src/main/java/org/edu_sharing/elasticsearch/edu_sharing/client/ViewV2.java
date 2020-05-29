package org.edu_sharing.elasticsearch.edu_sharing.client;


public class ViewV2 {
	private String id,caption,icon,html;
	private String rel;
	private boolean hideIfEmpty;

		public ViewV2(){}

		
		public String getCaption() {
			return caption;
		}
		public void setCaption(String caption) {
			this.caption = caption;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getHtml() {
			return html;
		}
		public void setHtml(String html) {
			this.html = html;
		}
		public String getIcon() {
			return icon;
		}
		public void setIcon(String icon) {
			this.icon = icon;
		}
		public String getRel() {
			return rel;
		}
		public void setRel(String rel) {
			this.rel = rel;
		}
		public boolean isHideIfEmpty() {
			return hideIfEmpty;
		}
		public void setHideIfEmpty(boolean hideIfEmpty) {
			this.hideIfEmpty = hideIfEmpty;
		}
}

