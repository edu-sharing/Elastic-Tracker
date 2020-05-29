package org.edu_sharing.elasticsearch.edu_sharing.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SortColumnV2 {
		private String id;
		private String mode;

		public SortColumnV2() {

		}
		
		
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}
}

