package com.synchronoss.saw.storage.proxy.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONFlattener {
    /**
     * The JSONObject type
     */
    private static final Class<?> JSON_OBJECT = JSONObject.class;
    /**
     * The JSONArray type
     */
    private static final Class<?> JSON_ARRAY = JSONArray.class;
    /**
     * The class Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JSONFlattener.class);

    /**
     * Parse the JSON content at the given URI using the default
     * character encoding UTF-8
     *
     * @param uri
     * @return
     */
    public static List<Map<String, String>> parseJson(URI uri) {
        return parseJson(uri, "UTF-8");
    }

    /**
     * Parse the JSON content at the given URI using the specified
     * character encoding
     *
     * @param uri
     * @return
     */
    public static List<Map<String, String>> parseJson(URI uri, String encoding) {
        List<Map<String, String>> flatJson = null;
        String json = "";

        try {
            json = IOUtils.toString(uri, encoding);
            flatJson = parseJson(json);
        } catch (IOException e) {
            LOGGER.error("JsonFlattener#ParseJson(uri, encoding) IOException: ", e);
        } catch (Exception ex) {
            LOGGER.error("JsonFlattener#ParseJson(uri, encoding) Exception: ", ex);
        }

        return flatJson;
    }

    /**
     * Parse the JSON file using the default character encoding UTF-8
     *
     * @param file
     * @return
     */
    public static List<Map<String, String>> parseJson(File file) {
        return parseJson(file, "UTF-8");
    }

    /**
     * Parse the JSON file using the specified character encoding
     *
     * @param file
     * @return
     */
    public static List<Map<String, String>> parseJson(File file, String encoding) {
        List<Map<String, String>> flatJson = null;
        String json = "";

        try {
            json = FileUtils.readFileToString(file, encoding);
            flatJson = parseJson(json);
        } catch (IOException e) {
            LOGGER.error("JsonFlattener#ParseJson(file, encoding) IOException: ", e);
        } catch (Exception ex) {
            LOGGER.error("JsonFlattener#ParseJson(file, encoding) Exception: ", ex);
        }

        return flatJson;
    }

    /**
     * Parse the JSON String
     *
     * @param json
     * @return
     * @throws Exception
     */
    public static List<Map<String, String>> parseJson(String json) {
        List<Map<String, String>> flatJson = null;

        try {
            JSONObject jsonObject = new JSONObject(json);
            flatJson = new ArrayList<Map<String, String>>();
            flatJson.add(parse(jsonObject));
        } catch (JSONException je) {
            LOGGER.info("Handle the JSON String as JSON Array");
            flatJson = handleAsArray(json);
        }

        return flatJson;
    }

    /**
     * Parse a JSON Object
     *
     * @param jsonObject
     * @return
     * @throws JSONException 
     */
    public static Map<String, String> parse(JSONObject jsonObject) throws JSONException {
        Map<String, String> flatJson = new LinkedHashMap<String, String>();
        flatten(jsonObject, flatJson, "");

        return flatJson;
    }

    /**
     * Parse a JSON Array
     *
     * @param jsonArray
     * @return
     */
    public static List<Map<String, String>> parse(JSONArray jsonArray) throws JSONException {
        JSONObject jsonObject = null;
        List<Map<String, String>> flatJson = new ArrayList<Map<String, String>>();
        int length = jsonArray.length();

        for (int i = 0; i < length; i++) {
            jsonObject = jsonArray.getJSONObject(i);
            Map<String, String> stringMap = parse(jsonObject);
            flatJson.add(stringMap);
        }

        return flatJson;
    }

    /**
     * Handle the JSON String as Array
     *
     * @param json
     * @return
     * @throws Exception
     */
    private static List<Map<String, String>> handleAsArray(String json) {
        List<Map<String, String>> flatJson = null;

        try {
            JSONArray jsonArray = new JSONArray(json);
            flatJson = parse(jsonArray);
        } catch (Exception e) {
            // throw new Exception("Json might be malformed");
            LOGGER.error("JSON might be malformed, Please verify that your JSON is valid");
        }

        return flatJson;
    }

    /**
     * Flatten the given JSON Object
     *
     * This method will convert the JSON object to a Map of
     * String keys and values
     *
     * @param obj
     * @param flatJson
     * @param prefix
     * @throws JSONException 
     */
    private static void flatten(JSONObject obj, Map<String, String> flatJson, String prefix) throws JSONException {
        Iterator<?> iterator = obj.keys();
        String _prefix = prefix != "" ? prefix + "." : "";

        while (iterator.hasNext()) {
            String key = iterator.next().toString();

            if (obj.get(key).getClass() == JSON_OBJECT) {
                JSONObject jsonObject = (JSONObject) obj.get(key);
                flatten(jsonObject, flatJson, _prefix + key);
            } else if (obj.get(key).getClass() == JSON_ARRAY) {
                JSONArray jsonArray = (JSONArray) obj.get(key);

                if (jsonArray.length() < 1) {
                    continue;
                }

                flatten(jsonArray, flatJson, _prefix + key);
            } else {
                String value = obj.get(key).toString();

                if (value != null && !value.equals("null")) {
                    flatJson.put(_prefix + key, value);
                }
            }
        }

    }

    /**
     * Flatten the given JSON Array
     *
     * @param obj
     * @param flatJson
     * @param prefix
     * @throws JSONException 
     */
    private static void flatten(JSONArray obj, Map<String, String> flatJson, String prefix) throws JSONException {
        int length = obj.length();

        for (int i = 0; i < length; i++) {
            if (obj.get(i).getClass() == JSON_ARRAY) {
                JSONArray jsonArray = (JSONArray) obj.get(i);

                // jsonArray is empty
                if (jsonArray.length() < 1) {
                    continue;
                }

                flatten(jsonArray, flatJson, prefix + "[" + i + "]");
            } else if (obj.get(i).getClass() == JSON_OBJECT) {
                JSONObject jsonObject = (JSONObject) obj.get(i);
                flatten(jsonObject, flatJson, prefix + "[" + (i + 1) + "]");
            } else {
                String value = obj.get(i).toString();

                if (value != null) {
                    flatJson.put(prefix + "[" + (i + 1) + "]", value);
                }
            }
        }
    }

}
