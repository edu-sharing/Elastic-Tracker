package org.edu_sharing.elasticsearch.edu_sharing.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupV2 {
		private String id;
		private List<String> views;
	
		public GroupV2(){}

		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}

		public List<String> getViews() {
			return views;
		}
		public void setViews(List<String> views) {
			this.views = views;
		}
		
	}

