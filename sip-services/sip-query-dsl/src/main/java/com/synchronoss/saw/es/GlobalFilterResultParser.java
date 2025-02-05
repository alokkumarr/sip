package com.synchronoss.saw.es;


import com.fasterxml.jackson.databind.JsonNode;
import com.synchronoss.saw.model.globalfilter.Filter;
import com.synchronoss.saw.model.globalfilter.GlobalFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalFilterResultParser {

    private final static String KEY = "key";
    private final static String KEY_AS_STRING ="key_as_string";
    private final static String BUCKETS = "buckets";
    private final static String VALUE = "value";
    private final static String _MIN = "_min";
    private final static String _MAX = "_max";

    private GlobalFilter globalFilter;


    private static final Logger logger = LoggerFactory.getLogger(GlobalFilterResultParser.class);

    public GlobalFilterResultParser(GlobalFilter globalFilter)
    {
        this.globalFilter=globalFilter;
    }

    /**
     * JSON node parser based on the global filter fields
     * to convert .
     * @param jsonNode
     * @return
     */
    public Map jsonNodeParser(JsonNode jsonNode)
    {
        Map<String,Object> dataObj = new HashMap();

        for (Filter filter : globalFilter.getFilters())
            if(filter.getType()== Filter.Type.STRING) {
                JsonNode childNode = jsonNode.get(filter.getColumnName()).get(BUCKETS);
                Iterator<JsonNode> iterator = childNode.iterator();
                List<String> uniqueValues = new ArrayList<>();
                while (iterator.hasNext()) {
                    JsonNode jsonNode1 = iterator.next();
                    if (jsonNode1.get(KEY) != null) {
                        if (jsonNode1.get(KEY_AS_STRING) != null)
                            uniqueValues.add(jsonNode1.get(KEY_AS_STRING).textValue());
                        else if (jsonNode1.get(KEY).isNumber()) {
                            switch (jsonNode1.get(KEY).numberType()) {
                                case LONG:
                                    uniqueValues.add(String.valueOf(jsonNode1.get(KEY).longValue()));
                                    break;
                                case BIG_INTEGER:
                                    uniqueValues.add(String.valueOf(jsonNode1.get(KEY).bigIntegerValue()));
                                    break;
                                case FLOAT:
                                    uniqueValues.add(String.valueOf(jsonNode1.get(KEY).floatValue()));
                                    break;
                                case DOUBLE:
                                    uniqueValues.add(String.valueOf(jsonNode1.get(KEY).doubleValue()));
                                    break;
                                case BIG_DECIMAL:
                                    uniqueValues.add(String.valueOf(jsonNode1.get(KEY).doubleValue()));
                                    break;
                                case INT:
                                    uniqueValues.add(String.valueOf(jsonNode1.get(KEY).intValue()));
                                    break;
                                default:
                                    uniqueValues.add(jsonNode1.get(KEY).textValue());
                            }
                        }
                        else {
                            uniqueValues.add(jsonNode1.get(KEY).textValue());
                        }
                    }
                }
                dataObj.put(filter.getColumnName(),uniqueValues);
            }
            else {
            // This is used to parse range value
                Map<String,String> rangeValue = new HashMap<>();
                JsonNode fieldMinValue = jsonNode.get(filter.getColumnName()+_MIN);
                JsonNode fieldMaxValue = jsonNode.get(filter.getColumnName()+_MAX);
                rangeValue.put(_MIN,String.valueOf(fieldMinValue.get(VALUE)));
                rangeValue.put(_MAX,String.valueOf(fieldMaxValue.get(VALUE)));
                dataObj.put(filter.getColumnName(),rangeValue);
            }
                return dataObj;
            }

}
