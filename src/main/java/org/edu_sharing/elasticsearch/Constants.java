package org.edu_sharing.elasticsearch;

import java.util.HashMap;
import java.util.Map;

public class Constants {


    public final static String NAMESPACE_CCM ="http://www.campuscontent.de/model/1.0";

    public final static String NAMESPACE_LOM = "http://www.campuscontent.de/model/lom/1.0";

    public final static String NAMESPACE_CM = "http://www.alfresco.org/model/content/1.0";

    public final static String NAMESPACE_SYS = "http://www.alfresco.org/model/system/1.0";

    public final static String NAMESPACE_VIRTUAL = "virtualproperty";

    public final static String NAMESPACE_EXIF = "http://www.alfresco.org/model/exif/1.0";

    public final static String NAMESPACE_SHORT_CCM = "ccm";

    public final static String NAMESPACE_SHORT_LOM = "cclom";

    public final static String NAMESPACE_SHORT_CM = "cm";

    public final static String NAMESPACE_SHORT_SYS = "sys";

    public final static String NAMESPACE_SHORT_VIRTUAL = "virtual";

    public final static String NAMESPACE_SHORT_EXIF = "exif";

    private static HashMap<String, String> nameSpaceMap = null;
    /**
     * @return <namespace,localnamespace>
     */
    public static HashMap<String, String> getNameSpaceMap() {
        if(nameSpaceMap == null){
            nameSpaceMap = new HashMap<String, String>();
            nameSpaceMap.put(NAMESPACE_CCM, NAMESPACE_SHORT_CCM);
            nameSpaceMap.put(NAMESPACE_CM, NAMESPACE_SHORT_CM);
            nameSpaceMap.put(NAMESPACE_LOM, NAMESPACE_SHORT_LOM);
            nameSpaceMap.put(NAMESPACE_SYS,  NAMESPACE_SHORT_SYS);
            nameSpaceMap.put(NAMESPACE_VIRTUAL, NAMESPACE_SHORT_VIRTUAL);
            nameSpaceMap.put(NAMESPACE_EXIF, NAMESPACE_SHORT_EXIF);
        }
        return nameSpaceMap;
    }

    /**
     * get locale name for a namespace value
     * @param value
     * @return
     */
    public static String getValidLocalName(String value){

        if(value == null) return null;

        for(Map.Entry<String,String> entry: getNameSpaceMap().entrySet()){
            if(value.contains(entry.getKey())){
                String valMinusNamespace =  value.replaceAll("\\{"+entry.getKey()+"\\}","");
                return entry.getValue()+":"+valMinusNamespace;
            }
        }
        return null;
    }

    /**
     * get globale name for a namespace value
     * @param value
     * @return
     */
    public static String getValidGlobalName(String value){

        for(Map.Entry<String,String> entry: getNameSpaceMap().entrySet()){
            if(value.startsWith(entry.getValue())){
                String valMinusNamespace =  value.replaceAll("^.+:", "");
                return "{" + entry.getKey() + "}" + valMinusNamespace;
            }
        }
        return null;
    }
}
