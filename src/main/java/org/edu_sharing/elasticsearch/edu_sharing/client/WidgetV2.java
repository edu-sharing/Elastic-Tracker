package org.edu_sharing.elasticsearch.edu_sharing.client;

import java.util.ArrayList;
import java.util.List;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WidgetV2 {

	public static class Condition{
		private String value,type;
		private boolean negate;
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public boolean isNegate() {
			return negate;
		}
		public void setNegate(boolean negate) {
			this.negate = negate;
		}

	}
	public static class Subwidget{
    			private String id;

    			public String getId() {
    				return id;
    			}

    			public void setId(String id) {
    				this.id = id;
    			}

    }
		private String id,caption,bottomCaption,icon,type,template;
		private boolean hasValues;
		private List<ValueV2> values;
		private List<Subwidget> subwidgets;
		private String placeholder;
		private String unit;
		private Integer min;
		private Integer max;
		private Integer defaultMin;
		private Integer defaultMax;
		private Integer step;
		private boolean isExtended;
		private boolean isRequired;
		private boolean allowempty;
		private String defaultvalue;
		private boolean isSearchable;
		private Condition condition;

	public WidgetV2(){}

		public Condition getCondition() {
			return condition;
		}
		public void setCondition(Condition condition) {
			this.condition = condition;
		}
		@JsonProperty("isSearchable")
		public boolean isSearchable() {
			return isSearchable;
		}
		public void setSearchable(boolean searchable) {
		this.isSearchable = searchable;
	}
		public String getBottomCaption() {
			return bottomCaption;
		}
		public void setBottomCaption(String bottomCaption) {
			this.bottomCaption = bottomCaption;
		}
		public String getDefaultvalue() {
			return defaultvalue;
		}
		public void setDefaultvalue(String defaultvalue) {
			this.defaultvalue = defaultvalue;
		}
		public String getTemplate() {
			return template;
		}
		public void setTemplate(String template) {
			this.template = template;
		}
		public String getIcon() {
			return icon;
		}
		public void setIcon(String icon) {
			this.icon = icon;
		}
		public String getPlaceholder() {
			return placeholder;
		}
		public void setPlaceholder(String placeholder) {
			this.placeholder = placeholder;
		}
		public Integer getMin() {
			return min;
		}
		public void setMin(Integer min) {
			this.min = min;
		}
		public Integer getMax() {
			return max;
		}
		public void setMax(Integer max) {
			this.max = max;
		}
		public Integer getDefaultMin() {
			return defaultMin;
		}
		public void setDefaultMin(Integer defaultMin) {
			this.defaultMin = defaultMin;
		}
		public Integer getDefaultMax() {
			return defaultMax;
		}
		public void setDefaultMax(Integer defaultMax) {
			this.defaultMax = defaultMax;
		}
		public Integer getStep() {
			return step;
		}
		public void setStep(Integer step) {
			this.step = step;
		}

		@JsonProperty("isExtended")
	public boolean isExtended() {
		return isExtended;
	}


	public void setExtended(boolean extended) {
		isExtended = extended;
	}

	public boolean isAllowempty() {
			return allowempty;
		}
		public void setAllowempty(boolean allowempty) {
			this.allowempty = allowempty;
		}
		@JsonProperty("isRequired")
		public boolean isRequired() {
			return isRequired;
		}
		public void setRequired(boolean isRequired) {
			this.isRequired = isRequired;
		}
		public List<ValueV2> getValues() {
			return values;
		}
		public void setValues(List<ValueV2> values) {
			this.values = values;
		}
		public boolean isHasValues() {
			return hasValues;
		}

		public void setHasValues(boolean hasValues) {
			this.hasValues = hasValues;
		}

		public String getCaption() {
			return caption;
		}
		public void setCaption(String caption) {
			this.caption = caption;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getUnit() {
			return unit;
		}
		public void setUnit(String unit) {
			this.unit = unit;
		}
		@JsonProperty
		public List<Subwidget> getSubwidgets() {
			return subwidgets;
		}
		public void setSubwidgets(List<Subwidget> subwidgets) {
			this.subwidgets = subwidgets;
		}
	}

