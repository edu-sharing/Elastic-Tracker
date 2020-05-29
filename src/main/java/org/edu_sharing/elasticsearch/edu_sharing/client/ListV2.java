package org.edu_sharing.elasticsearch.edu_sharing.client;

import java.util.ArrayList;
import java.util.List;


import com.fasterxml.jackson.annotation.JsonProperty;


public class ListV2 {
		private String id;
		private List<ColumnV2> columns;
	
		public ListV2(){}


	@JsonProperty("id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
		@JsonProperty("columns")
		public List<ColumnV2> getColumns() {
			return columns;
		}
		public void setColumns(List<ColumnV2> columns) {
			this.columns = columns;
		}
		
		
	}

