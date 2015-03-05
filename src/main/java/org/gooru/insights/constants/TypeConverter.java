package org.gooru.insights.constants;

import java.util.Date;
import java.util.Map;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

public class TypeConverter {

	public static <T> T stringToIntArray(String arrayAsString) {
		if (arrayAsString != null) {

			String[] items = arrayAsString.replaceAll("\\[", "").replaceAll("\\]", "").split(",");

			int[] results = new int[items.length];

			for (int i = 0; i < items.length; i++) {
				try {
					results[i] = Integer.parseInt(items[i]);
				} catch (NumberFormatException nfe) {
				}
				;
			}
			return (T) results;
		}
		return null;
	}

	public static <T> T stringToAny(String value, String type) {
		Object result = null;
		if (value != null && type != null) {
			if (type.equalsIgnoreCase("Long")) {
				try {
					result = Long.valueOf(value.trim());
				} catch (NumberFormatException nfel) {
					result = 0L;
					return (T) result;
				}
			} else if (type.equalsIgnoreCase("Double")) {
				try {
					result = Double.valueOf(value.trim());
				} catch (NumberFormatException nfel) {
					result = 0.0;
					return (T) result;
				}
			} else if (type.equalsIgnoreCase("Integer")) {
				try {
					result = Integer.valueOf(value.trim());
				} catch (NumberFormatException nfel) {
					result = 0;
					return (T) result;
				}
			} else if (type.equalsIgnoreCase("JSONObject")) {
				try {
					result = new JSONObject(value.trim());
				} catch (JSONException e) {
					System.out.print("Unable to convert to JSONObject");
					return (T) new JSONObject();
				}
			} else if (type.equalsIgnoreCase("Date")) {
				// accepting timestamp
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss+0000");
				SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
				SimpleDateFormat formatter3 = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
				SimpleDateFormat formatter4 = new SimpleDateFormat("yyyy/MM/dd kk:mm:ss.000");
				try {
					result = new Date(Long.valueOf(value));
				} catch (Exception e) {
					try {
						result = formatter.parse(value);
					} catch (Exception e1) {
						try {
							result = formatter2.parse(value);
						} catch (Exception e2) {
							try {
								result = formatter3.parse(value);
							} catch (Exception e3) {
								try {
									result = formatter4.parse(value);
								} catch (Exception e4) {
									System.out.print("Error while convert " + value + " to date");
								}
							}
						}
					}
				}

			} else if (type.equalsIgnoreCase("Boolean")) {
				result = Boolean.valueOf(value.trim());
			} else if (type.equalsIgnoreCase("String")) {
				result = value.trim();
			} else if (type.equals("IntegerArray")) {

				String[] items = value.trim().replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\"", "").split(",");

				int[] results = new int[items.length];

				for (int i = 0; i < items.length; i++) {
					try {
						if (!items[i].trim().isEmpty() && Integer.parseInt(items[i].trim()) != 0) {
							results[i] = Integer.parseInt(items[i].trim());
						}
					} catch (NumberFormatException nfe) {
						// nfe.printStackTrace();
					}
					;
				}
				result = results;
			} else if (type.equalsIgnoreCase("StringArray")) {

				String[] items = value.trim().replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\"", "").split(",");

				String[] results = new String[items.length];

				for (int i = 0; i < items.length; i++) {
					try {
						results[i] = items[i].trim();
					} catch (Exception nfe) {
					}
					;
				}
				result = results;
			} else if (type.equalsIgnoreCase("JSONArray")) {
				try {
					result = new JSONArray(value);
				} catch (JSONException e) {
					System.out.print("Unable to convert to JSONArray");
					return (T) new JSONArray();
				}
			} else if (type.equalsIgnoreCase("Timestamp")) {
				// accepting timestamp
				try {
					result = new Timestamp(Long.valueOf(value));
				} catch (Exception e) {
					System.out.print("Error while convert " + value + " to timestamp");
				}

			} else {
				throw new RuntimeException("Unsupported type " + type + ". Please Contact Admin!!");
			}

			return (T) result;
		}
		return null;
	}

	public static String convertMapToJsonString(Map<String, String> map) {
		Gson gson = new Gson();
		String json = gson.toJson(map);
		return json;
	}

}