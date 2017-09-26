
package model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "dataSecuritykey"
})
public class DataSecurityKey {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("dataSecuritykey")
    private List<DataSecurityKeyDef> dataSecuritykey;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    
    @JsonProperty("dataSecuritykey")
    public List<DataSecurityKeyDef> getDataSecuritykey() {
		return dataSecuritykey;
	}
    @JsonProperty("dataSecuritykey")
	public void setDataSecuritykey(List<DataSecurityKeyDef> dataSecuritykey) {
		this.dataSecuritykey = dataSecuritykey;
	}

	@JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
