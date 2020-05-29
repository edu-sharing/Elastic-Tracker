package org.edu_sharing.elasticsearch.edu_sharing.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class SortV2 {
	public class SortV2Default{
		private String sortBy;
		private boolean sortAscending;

		public String getSortBy() {
			return sortBy;
		}

		public void setSortBy(String sortBy) {
			this.sortBy = sortBy;
		}

		public boolean isSortAscending() {
			return sortAscending;
		}

		public void setSortAscending(boolean sortAscending) {
			this.sortAscending = sortAscending;
		}
	}
	private String id;
	private SortV2Default defaultValue;
	private List<SortColumnV2> columns;

	public SortV2(){}


	@JsonProperty
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	@JsonProperty
	public List<SortColumnV2> getColumns() {
		return columns;
	}
	public void setColumns(List<SortColumnV2> columns) {
		this.columns = columns;
	}

	@JsonProperty("default")
	public SortV2Default getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(SortV2Default defaultValue) {
		this.defaultValue = defaultValue;
	}
}

