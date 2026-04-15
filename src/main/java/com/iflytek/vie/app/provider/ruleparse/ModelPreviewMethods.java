package com.iflytek.vie.app.provider.ruleparse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.pojo.model.ModelFragment;
import com.iflytek.vie.app.pojo.model.ModelTagDimension;
import com.iflytek.vie.utils.ListUtil;
import com.iflytek.vie.utils.StringUtils;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class ModelPreviewMethods implements Serializable {
   private static final long serialVersionUID = -728086341685589929L;
   protected ObjectMapper mapper = new ObjectMapper();

   public String getPaseRule(boolean isFragment, List<ModelFragment> modelFragmentList, String fragmentContent, String inputRules, int channel, int searchType) {
      String sql = "";
      if (!isFragment && !StringUtils.isNullOrEmpry(inputRules)) {
         boolean isNotFlag = false;
         if (inputRules.contains("@")) {
            isNotFlag = true;
         }

         inputRules = inputRules.replace("@", "&");
         String input = dealInputRules(inputRules);
         List<String> rules = getList(input);
         input = input.replace("|", " or ").replace("&", " and ").replace("!", "");
         String[] inputArr = input.split("");
         String[] inputArr2 = deleteOne(inputArr, 0);
         String[] inputArr3 = this.deleteEmpyt(input.split("\\d"));
         String[] inputArr5 = this.deleteEmpyt(input.split("or|and|\\||\\(|\\)"));
         String[] inputArr4 = null;
         int len = inputArr3.length > inputArr5.length ? inputArr3.length : inputArr5.length;
         if (inputArr2[0].matches("\\d")) {
            List<String> list = new ArrayList<>();

            for (int t = 0; t < len; t++) {
               if (t < inputArr5.length) {
                  list.add(inputArr5[t]);
               }

               if (t < inputArr3.length) {
                  list.add(inputArr3[t]);
               }
            }

            inputArr4 = listToStringArr(list);
         } else {
            List<String> list = new ArrayList<>();

            for (int t = 0; t < len; t++) {
               if (t < inputArr3.length) {
                  list.add(inputArr3[t]);
               }

               if (t < inputArr5.length) {
                  list.add(inputArr5[t]);
               }
            }

            inputArr4 = listToStringArr(list);
         }

         for (int i = 0; i < rules.size(); i++) {
            String fragmentState = rules.get(i);
            String[] fragmentStateArr = fragmentState.split(";");
            String numStr = fragmentStateArr[0];
            int showNum = Integer.parseInt(fragmentStateArr[1]);
            int num = 0;
            int times = 0;

            for (int k = 0; k < inputArr4.length; k++) {
               if (numStr.equals(inputArr4[k].trim())) {
                  if (++times == 1) {
                     num = k;
                     break;
                  }
               }
            }

            try {
               HashMap<String, String> hm = this.getFramentContent(modelFragmentList, fragmentStateArr[0]);
               if ("1".equals(hm.get("type"))) {
                  inputArr4[num] = "(" + hm.get("text") + ")";
               } else if ("false".equals(fragmentStateArr[2])) {
                  if (searchType == 0) {
                     inputArr4[num] = "(fulltext('+(" + hm.get("content") + ":(!(" + hm.get("text") + ")))','content-word-query-parser')=true)";
                  } else {
                     inputArr4[num] = "(fulltext('+(" + hm.get("content") + ":(!(" + hm.get("text") + ")))','content-query-parser')=true)";
                  }
               } else if (searchType == 0) {
                  inputArr4[num] = "(fulltext('+(" + hm.get("content") + ":(" + hm.get("text") + "))','content-word-query-parser')=true)";
               } else {
                  inputArr4[num] = "(fulltext('+(" + hm.get("content") + ":(" + hm.get("text") + "))','content-query-parser')=true)";
               }
            } catch (Exception var25) {
               var25.printStackTrace();
            }
         }

         String text = "";

         for (int i = 0; i < inputArr4.length; i++) {
            if (!isNotFlag) {
               text = text + inputArr4[i];
            } else if (!inputArr4[i].contains("!")) {
               text = text + inputArr4[i];
            } else if (text.length() >= 3 && "and".equals(text.substring(text.length() - 3, text.length()))) {
               text = text.substring(0, text.length() - 3);
            }
         }

         if (text.length() >= 3 && "and".equals(text.substring(0, 3))) {
            text = text.substring(3, text.length());
         }

         sql = "".equals(text) ? "" : "(" + text + ")";
      } else {
         String channelCondition = "";
         switch (channel) {
            case 0:
               channelCondition = "contentN0";
               break;
            case 1:
               channelCondition = "contentN1";
               break;
            case 2:
               channelCondition = "content";
         }

         if (modelFragmentList.get(0).getIsTag() == 1) {
            try {
               sql = this.getParseTagContent(modelFragmentList.get(0));
            } catch (Exception var26) {
               var26.printStackTrace();
            }
         } else if (!StringUtils.isNullOrEmpry(fragmentContent)) {
            if (searchType == 0) {
               sql = "(fulltext('+(" + channelCondition + ":(" + fragmentContent + "))','content-word-query-parser')=true)";
            } else {
               sql = "(fulltext('+(" + channelCondition + ":(" + fragmentContent + "))','content-query-parser')=true)";
            }
         }
      }

      return sql;
   }

   public String getParseTagContent(ModelFragment mf) throws Exception {
      new ArrayList();
      List<HashMap<String, Object>> list = (List)this.mapper.readValue(mf.getTagContent(), List.class);
      String ruleText = "";

      for (int i = 0; i < list.size(); i++) {
         HashMap<String, Object> hm = (HashMap<String, Object>)list.get(i);
         List<HashMap<String, Object>> list2 = (List<HashMap<String, Object>>)hm.get("value");
         String dimensionCode = hm.get("dimensionCode").toString();
         if (dimensionCode.equals(ModelTagDimension.DCODE1)) {
            if (i == list.size() - 1) {
               ruleText = ruleText + dealJYRuel(list2, list);
            } else {
               ruleText = ruleText + dealJYRuel(list2, list) + " and ";
            }
         } else if (i == list.size() - 1) {
            ruleText = ruleText + dealGJCRuel(list2, list);
         } else {
            ruleText = ruleText + dealGJCRuel(list2, list) + " and ";
         }
      }

      return ruleText;
   }

   private static String dealInputRules(String input) {
      String[] a = input.split("");
      String[] arr = deleteOne(a, 0);
      List<Integer> list = new ArrayList<>();

      for (int i = 0; i < arr.length; i++) {
         if ("!".equals(arr[i])) {
            list.add(i);
         }
      }

      if (list != null && list.size() > 0) {
         for (int j = 0; j < list.size(); j++) {
            String ss = input.substring(list.get(j) + 1, input.length());
            String ss2 = input.substring(0, list.get(j));
            if (ss.startsWith("(")) {
               String str2 = getPP(ss);
               String[] str2s = str2.split("");
               String[] str22 = deleteOne(str2s, 0);
               String result = "";

               for (int m = 0; m < str22.length; m++) {
                  if ("|".equals(str22[m])) {
                     result = result + "&";
                  } else if ("&".equals(str22[m])) {
                     result = result + "|";
                  } else {
                     result = result + str22[m];
                  }
               }

               input = input.substring(0, list.get(j) + 1) + result + input.substring(list.get(j) + 1 + str2.length(), input.length());
            }
         }
      }

      return input;
   }

   public static String getPP(String str) {
      Stack<Character> stack = new Stack<>();
      int n = 0;

      for (int j = 0; j < str.length(); j++) {
         if ('(' == str.charAt(j)) {
            stack.add(str.charAt(j));
         }

         if (')' == str.charAt(j)) {
            stack.pop();
            if (stack.isEmpty()) {
               n = j;
               break;
            }
         }
      }

      return str.substring(0, n + 1);
   }

   private static List<String> getList(String input) {
      List<String> numList = new ArrayList<>();
      String[] a = input.split("");
      String[] arr = deleteOne(a, 0);
      List<String> strList = new ArrayList<>();
      String[] numArr = input.split("!|&|\\||\\(|\\)");
      List<Integer> list = new ArrayList<>();

      for (int t = 0; t < numArr.length; t++) {
         if (numArr[t].matches("[0-9]+")) {
            if (strList.contains(numArr[t])) {
               int nnn = getNumOfList(strList, numArr[t]);
               numList.add(numArr[t] + ";" + nnn + ";true");
            } else {
               numList.add(numArr[t] + ";1;true");
            }

            strList.add(numArr[t]);
         }
      }

      for (int i = 0; i < arr.length; i++) {
         if ("!".equals(arr[i])) {
            list.add(i);
         }
      }

      if (list != null && list.size() > 0) {
         for (int j = 0; j < list.size(); j++) {
            String ss = input.substring(list.get(j) + 1, input.length());
            String ss2 = input.substring(0, list.get(j));
            if (ss.startsWith("(")) {
               String str2 = getPP(ss);
               String[] str2A = str2.split("");
               String[] str2Arr = deleteOne(str2A, 0);
               List<String> str2List = new ArrayList<>();

               for (int k = 0; k < str2Arr.length; k++) {
                  if (str2Arr[k].matches("[0-9]+")) {
                     if (k + 1 < str2Arr.length) {
                        if (str2Arr[k + 1].matches("[0-9]+")) {
                           int km = getNumOfList(str2List, str2Arr[k] + str2Arr[k + 1]);
                           int kk = getNumOfTarget(ss2, str2Arr[k] + str2Arr[k + 1]);
                           int cxNum = kk + km;
                           numList = dealList(numList, str2Arr[k] + str2Arr[k + 1], cxNum);
                           str2List.add(str2Arr[k] + str2Arr[k + 1]);
                        } else {
                           int km = getNumOfList(str2List, str2Arr[k]);
                           int kk = getNumOfTarget(ss2, str2Arr[k]);
                           int cxNum = kk + km;
                           numList = dealList(numList, str2Arr[k], cxNum);
                           str2List.add(str2Arr[k]);
                        }
                     } else {
                        int km = getNumOfList(str2List, str2Arr[k]);
                        int kk = getNumOfTarget(ss2, str2Arr[k]);
                        int cxNum = kk + km;
                        numList = dealList(numList, str2Arr[k], cxNum);
                        str2List.add(str2Arr[k]);
                     }
                  }
               }
            } else if (ss.contains(")")) {
               String sm = ss.substring(0, ss.indexOf(")"));
               int kk = getNumOfTarget(ss2, sm);
               int cxNum = kk + 1;
               numList = dealList(numList, sm, cxNum);
            } else {
               if (list.get(j) == 0) {
                  ss2 = input.substring(0, list.get(j) + 1);
               }

               int kk = getNumOfTarget(ss2, ss);
               int cxNum = kk + 1;
               numList = dealList(numList, ss, cxNum);
            }
         }
      }

      return numList;
   }

   private String[] deleteEmpyt(String[] arr) {
      String[] reArr = null;
      List<String> list = new ArrayList<>();

      for (int i = 0; i < arr.length; i++) {
         if (!StringUtils.isNullOrEmpry(arr[i]) && !" ".equals(arr[i])) {
            list.add(arr[i].trim());
         }
      }

      reArr = new String[list.size()];

      for (int ix = 0; ix < list.size(); ix++) {
         reArr[ix] = list.get(ix);
      }

      return reArr;
   }

   private static String[] listToStringArr(List<String> list) {
      String[] arr = new String[list.size()];

      for (int i = 0; i < list.size(); i++) {
         arr[i] = list.get(i);
      }

      return arr;
   }

   private static String[] deleteOne(String[] strArr, int num) {
      String[] arr = new String[strArr.length - 1];
      return Arrays.copyOfRange(strArr, 1, strArr.length);
   }

   private static List<String> dealList(List<String> strList, String str, int num) {
      List<String> resultList = new ArrayList<>();

      for (String ss : strList) {
         String[] ssArr = ss.split(";");
         if (ssArr[0].equals(str) && Integer.parseInt(ssArr[1]) == num) {
            if ("true".equals(ssArr[2])) {
               ssArr[2] = "false";
            } else {
               ssArr[2] = "true";
            }
         }

         ss = ssArr[0] + ";" + ssArr[1] + ";" + ssArr[2];
         resultList.add(ss);
      }

      return resultList;
   }

   private static int getNumOfList(List<String> list, String str) {
      int num = 1;

      for (String ss : list) {
         if (ss.equals(str)) {
            num++;
         }
      }

      return num;
   }

   private static int getNumOfTarget(String text, String str) {
      int num = 0;
      String[] arr = text.split("");
      String[] arr2 = deleteOne(arr, 0);
      int a = 0;

      for (int i = 0; i < arr2.length; i++) {
         if (arr2[i].equals(str)) {
            if ((i - 1 < 0 || !arr2[i - 1].matches("[0-9]")) && (i + 1 >= arr2.length || !arr2[i + 1].matches("[0-9]"))) {
               num++;
            }
         } else {
            a = i + 1;
            if (a < arr2.length - 1 && (arr2[i] + arr2[i + 1]).equals(str)) {
               num++;
            }
         }
      }

      return num;
   }

   private HashMap<String, String> getFramentContent(List<ModelFragment> modelFragmentList, String num) throws Exception {
      HashMap<String, String> hm = new HashMap<>();

      for (ModelFragment mf : modelFragmentList) {
         if ((mf.getFragmentNum() + "").equals(num)) {
            if (mf.getIsTag() == 1) {
               String text = this.getParseTagContent(mf);
               hm.put("type", "1");
               hm.put("text", text);
            } else {
               hm.put("type", "0");
               hm.put("text", mf.getFragmentContent());
            }

            if (mf.getChannel() == 2) {
               hm.put("content", "content");
            } else if (mf.getChannel() == 0) {
               hm.put("content", "contentN0");
            } else {
               hm.put("content", "contentN1");
            }
         }
      }

      return hm;
   }

   private static String dealJYRuel(List<HashMap<String, Object>> list, List<HashMap<String, Object>> totalList) throws Exception {
      List<String> dxList = new ArrayList<>();
      List<String> csList = new ArrayList<>();
      List<HashMap<String, Object>> xdList = new ArrayList<>();
      List<String> jdList = new ArrayList<>();
      List<String> zqList = new ArrayList<>();
      List<String> zhList = new ArrayList<>();

      for (int i = 0; i < list.size(); i++) {
         HashMap<String, Object> hm2 = list.get(i);
         String propertyCode = hm2.get("propertyCode").toString();
         String operationCode = hm2.get("operationCode").toString();
         String value = hm2.get("value").toString();
         if (propertyCode.equals(ModelTagDimension.PDAXIAOCODE)) {
            dxList.add(operationCode + ":" + value);
         }

         if (propertyCode.equals(ModelTagDimension.PCISHUCODE)) {
            csList.add(operationCode + ":" + value);
         }

         if (propertyCode.equals(ModelTagDimension.PZHIQIANCODE)) {
            zqList.add(operationCode);
         }

         if (propertyCode.equals(ModelTagDimension.PZHIHOUCODE)) {
            zhList.add(operationCode);
         }

         if (propertyCode.equals(ModelTagDimension.PJUEDUICODE)) {
            jdList.add(operationCode + ":" + value);
         }

         if (propertyCode.equals(ModelTagDimension.PXIANGDUICODE)) {
            String relationId = hm2.get("relativeobject").toString();
            HashMap<String, Object> hm3 = new HashMap<>();
            hm3.put("operationCode", operationCode);
            hm3.put("value", value);

            for (int j = 0; j < totalList.size(); j++) {
               HashMap<String, Object> hm4 = new HashMap<>();
               String id = hm4.get("relativeobject").toString();
               if (relationId.equals(id)) {
                  hm3.put("relationObject", hm4);
               }
            }

            xdList.add(hm3);
         }
      }

      return dealJYText(dxList, csList, zqList, zhList, jdList, xdList);
   }

   private static String dealGJCRuel(List<HashMap<String, Object>> list, List<HashMap<String, Object>> totalList) {
      List<String> sdList = new ArrayList<>();
      List<String> jdList = new ArrayList<>();
      List<String> csList = new ArrayList<>();
      List<HashMap<String, Object>> xdList = new ArrayList<>();
      List<String> lbList = new ArrayList<>();

      for (int i = 0; i < list.size(); i++) {
         HashMap<String, Object> hm2 = list.get(i);
         String propertyCode = hm2.get("propertyCode").toString();
         String operationCode = hm2.get("operationCode").toString();
         String value = hm2.get("value").toString();
         if (propertyCode.equals(ModelTagDimension.PLIEBIAOCODE)) {
            lbList.add(value);
         } else if (propertyCode.equals(ModelTagDimension.PSHENGDAOCODE)) {
            if ("VT001".equals(operationCode)) {
               sdList.add("contentN1");
            } else if ("VT002".equals(operationCode)) {
               sdList.add("contentN0");
            }
         } else if (propertyCode.equals(ModelTagDimension.PCISHUCODE)) {
            csList.add(operationCode + ";" + value);
         } else if (!propertyCode.equals(ModelTagDimension.PXIANGDUICODE)) {
            if (propertyCode.equals(ModelTagDimension.PJUEDUICODE)) {
               jdList.add(operationCode + ";" + value);
            }
         } else {
            String relationId = hm2.get("relativeobject").toString();
            HashMap<String, Object> hm4 = new HashMap<>();
            hm4.put("operationCode", operationCode);
            hm4.put("value", value);

            for (int j = 0; j < totalList.size(); j++) {
               HashMap<String, Object> hm3 = totalList.get(i);
               String id = hm3.get("id").toString();
               if (relationId.equals(id)) {
                  hm4.put("relationObject", hm3);
               }
            }

            xdList.add(hm4);
         }
      }

      return getGJCText(lbList, sdList, csList, jdList, xdList);
   }

   private static String dealJYText(
      List<String> dxList, List<String> csList, List<String> zqList, List<String> zhList, List<String> jdList, List<HashMap<String, Object>> xdList
   ) {
      String litle = "";
      String max = "";
      String equ = "";
      String text = "";

      for (int i = 0; i < dxList.size(); i++) {
         String ss = dxList.get(i);
         String[] ssArr = ss.split(":");
         if (ssArr[0].equals(ModelTagDimension.ODAYUCODE)) {
            ssArr[0] = ">";
            litle = getDXValue(ss, ssArr[0] + ":" + ssArr[1], 1);
         }

         if (ssArr[0].equals(ModelTagDimension.ODENGYUCODE)) {
            ssArr[0] = "=";
            equ = ssArr[0] + ";" + ssArr[1];
         }

         if (ssArr[0].equals(ModelTagDimension.ODAYUDENGYUCODE)) {
            ssArr[0] = ">=";
            litle = getDXValue(ss, ssArr[0] + ":" + ssArr[1], 1);
         }

         if (ssArr[0].equals(ModelTagDimension.OXIAOYUCODE)) {
            ssArr[0] = "<";
            max = getDXValue(ss, ssArr[0] + ":" + ssArr[1], 3);
         }

         if (ssArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
            ssArr[0] = "<";
            max = getDXValue(ss, ssArr[0] + ":" + ssArr[1], 3);
         }
      }

      if (StringUtils.isNullOrEmpry(litle)) {
         litle = ModelTagDimension.ODAYUDENGYUCODE + ";*";
      }

      if (StringUtils.isNullOrEmpry(max)) {
         max = ModelTagDimension.OXIAOYUDENGYUCODE + ";*";
      }

      String[] litleArr = litle.split(";");
      String[] maxArr = max.split(";");
      String[] equArr = equ.split(";");
      String minValue = "";
      String maxValue = "";
      String equValue = "";
      if (equArr.length > 1) {
         equValue = equArr[1];
      }

      List<String> kuohao = new ArrayList<>();
      if (litleArr[1].equals("*")) {
         minValue = "0000000000";
         kuohao.add("[");
      } else {
         minValue = dealValue(litleArr[1]);
         if (ModelTagDimension.ODAYUCODE.equals(litleArr[0])) {
            kuohao.add("(");
         } else {
            kuohao.add("[");
         }
      }

      if (maxArr[1].equals("*")) {
         kuohao.add("]");
         maxValue = "9999999999";
      } else {
         maxValue = dealValue(maxArr[1]);
         if (ModelTagDimension.OXIAOYUCODE.equals(maxArr[0])) {
            kuohao.add(")");
         } else {
            kuohao.add("]");
         }
      }

      if (ListUtil.isNullOrEmpty(zqList)
         && ListUtil.isNullOrEmpty(zhList)
         && ListUtil.isNullOrEmpty(csList)
         && ListUtil.isNullOrEmpty(xdList)
         && ListUtil.isNullOrEmpty(jdList)) {
         if (!StringUtils.isNullOrEmpry(equValue)) {
            if ("0000000000".equals(minValue) && "9999999999".equals(maxValue)) {
               text = text + "(fulltext('content:s\\[n00" + dealValue(equValue) + ",n00" + dealValue(equValue) + "\\]#1')=true)";
            } else {
               text = text + "(fulltext('content:s\\[n00" + dealValue(equValue) + ",n00" + dealValue(equValue) + "\\]#1')=true) AND ";
               if ("0000000000".equals(minValue)) {
                  text = text
                     + "(fulltext('content:s\\"
                     + kuohao.get(0)
                     + "n00"
                     + minValue
                     + ",n00"
                     + maxValue
                     + "\\"
                     + kuohao.get(1)
                     + "#1')=true OR silenceLong=0 )";
               } else {
                  text = text + "(fulltext('content:s\\" + kuohao.get(0) + "n00" + minValue + ",n00" + maxValue + "\\" + kuohao.get(1) + "#1')=true)";
               }
            }
         } else if ("0000000000".equals(minValue)) {
            text = text
               + "(fulltext('content:s\\"
               + kuohao.get(0)
               + "n00"
               + minValue
               + ",n00"
               + maxValue
               + "\\"
               + kuohao.get(1)
               + "#1')=true OR silenceLong=0 )";
         } else {
            text = text + "(fulltext('content:s\\" + kuohao.get(0) + "n00" + minValue + ",n00" + maxValue + "\\" + kuohao.get(1) + "#1')=true)";
         }
      } else {
         if (!ListUtil.isNullOrEmpty(xdList)) {
            for (int i = 0; i < xdList.size(); i++) {
               HashMap<String, Object> hm = xdList.get(i);
               String operationCode = hm.get("operationCode").toString();
               String value = hm.get("value").toString();
               HashMap<String, Object> relationObject = (HashMap<String, Object>)hm.get("relationObject");
               String relationDimensionCode = relationObject.get("dimensionCode").toString();
               if (relationDimensionCode.equals(ModelTagDimension.DCODE1)) {
               }
            }
         }

         if (!ListUtil.isNullOrEmpty(jdList)) {
            text = text + getJYJDText(minValue, maxValue, kuohao, csList, zqList, zhList, jdList);
         } else if (!ListUtil.isNullOrEmpty(csList)) {
            text = text + getJYCSText(minValue, maxValue, kuohao, csList, zqList, zhList);
         } else if (!ListUtil.isNullOrEmpty(zqList) && !ListUtil.isNullOrEmpty(zhList)) {
            for (int j = 0; j < zqList.size(); j++) {
               for (int m = 0; m < zhList.size(); m++) {
                  String zqSD = zqList.get(j);
                  String zhSD = zhList.get(m);
                  boolean havaAnd = true;
                  if (j == zqList.size() - 1 && m == zhList.size() - 1) {
                     havaAnd = false;
                  }

                  if (zqSD.equals(ModelTagDimension.OZUOXICODE) && zhSD.equals(ModelTagDimension.OZUOXICODE)) {
                     text = text + "(fulltext('silences:" + kuohao.get(0) + "00" + minValue + " TO 00" + maxValue + kuohao.get(1) + "')=true)";
                  } else if (zqSD.equals(ModelTagDimension.OZUOXICODE) && zhSD.equals(ModelTagDimension.OKEHUCODE)) {
                     text = text + "(fulltext('silences:" + kuohao.get(0) + "01" + minValue + " TO 01" + maxValue + kuohao.get(1) + "')=true)";
                  } else if (zqSD.equals(ModelTagDimension.OKEHUCODE) && zhSD.equals(ModelTagDimension.OZUOXICODE)) {
                     text = text + "(fulltext('silences:" + kuohao.get(0) + "10" + minValue + " TO 10" + maxValue + kuohao.get(1) + "')=true)";
                  } else if (zqSD.equals(ModelTagDimension.OKEHUCODE) && zhSD.equals(ModelTagDimension.OZUOXICODE)) {
                     text = text + "(fulltext('silences:" + kuohao.get(0) + "11" + minValue + " TO 11" + maxValue + kuohao.get(1) + "')=true)";
                  }

                  if (havaAnd) {
                     text = text + " AND ";
                  }
               }
            }
         } else {
            if (!ListUtil.isNullOrEmpty(zqList)) {
               for (int j = 0; j < zqList.size(); j++) {
                  String sd = zqList.get(j);
                  if (sd.equals(ModelTagDimension.OZUOXICODE)) {
                     if (j == zqList.size() - 1) {
                        text = text
                           + "(fulltext('silences:"
                           + kuohao.get(0)
                           + "01"
                           + minValue
                           + " TO 01"
                           + maxValue
                           + kuohao.get(1)
                           + " OR "
                           + "silences:"
                           + kuohao.get(0)
                           + "00"
                           + minValue
                           + " TO 00"
                           + maxValue
                           + kuohao.get(1)
                           + "')=true) ";
                     } else {
                        text = text
                           + "(fulltext('silences:"
                           + kuohao.get(0)
                           + "01"
                           + minValue
                           + " TO 01"
                           + maxValue
                           + kuohao.get(1)
                           + " OR "
                           + "silences:"
                           + kuohao.get(0)
                           + "00"
                           + minValue
                           + " TO 00"
                           + maxValue
                           + kuohao.get(1)
                           + "')=true) AND ";
                     }
                  } else if (j == zqList.size() - 1) {
                     text = text
                        + "(fulltext('silences:"
                        + kuohao.get(0)
                        + "11"
                        + minValue
                        + " TO 11"
                        + maxValue
                        + kuohao.get(1)
                        + " OR "
                        + "silences:"
                        + kuohao.get(0)
                        + "10"
                        + minValue
                        + " TO 10"
                        + maxValue
                        + kuohao.get(1)
                        + "')=true) ";
                  } else {
                     text = text
                        + "(fulltext('silences:"
                        + kuohao.get(0)
                        + "11"
                        + minValue
                        + " TO 11"
                        + maxValue
                        + kuohao.get(1)
                        + " OR "
                        + "silences:"
                        + kuohao.get(0)
                        + "10"
                        + minValue
                        + " TO 10"
                        + maxValue
                        + kuohao.get(1)
                        + "')=true) AND ";
                  }
               }
            }

            if (!ListUtil.isNullOrEmpty(zhList)) {
               for (int jx = 0; jx < zhList.size(); jx++) {
                  String sd = zhList.get(jx);
                  if (sd.equals(ModelTagDimension.OZUOXICODE)) {
                     if (jx == zhList.size() - 1) {
                        text = text
                           + "(fulltext('silences:"
                           + kuohao.get(0)
                           + "00"
                           + minValue
                           + " TO 00"
                           + maxValue
                           + kuohao.get(1)
                           + " OR "
                           + "silences:"
                           + kuohao.get(0)
                           + "10"
                           + minValue
                           + " TO 10"
                           + maxValue
                           + kuohao.get(1)
                           + "')=true) ";
                     } else {
                        text = text
                           + "(fulltext('silences:"
                           + kuohao.get(0)
                           + "00"
                           + minValue
                           + " TO 00"
                           + maxValue
                           + kuohao.get(1)
                           + " OR "
                           + "silences:"
                           + kuohao.get(0)
                           + "10"
                           + minValue
                           + " TO 10"
                           + maxValue
                           + kuohao.get(1)
                           + "')=true) AND ";
                     }
                  } else if (jx == zhList.size() - 1) {
                     text = text
                        + "(fulltext('silences:"
                        + kuohao.get(0)
                        + "01"
                        + minValue
                        + " TO 01"
                        + maxValue
                        + kuohao.get(1)
                        + " OR "
                        + "silences:"
                        + kuohao.get(0)
                        + "11"
                        + minValue
                        + " TO 11"
                        + maxValue
                        + kuohao.get(1)
                        + "')=true) ";
                  } else {
                     text = text
                        + "(fulltext('silences:"
                        + kuohao.get(0)
                        + "01"
                        + minValue
                        + " TO 01"
                        + maxValue
                        + kuohao.get(1)
                        + " OR "
                        + "silences:"
                        + kuohao.get(0)
                        + "11"
                        + minValue
                        + " TO 11"
                        + maxValue
                        + kuohao.get(1)
                        + "')=true) AND ";
                  }
               }
            }
         }
      }

      return text;
   }

   private static String getGJCText(List<String> lbList, List<String> sdList, List<String> csList, List<String> jdList, List<HashMap<String, Object>> xdList) {
      String text = "";
      if (ListUtil.isNullOrEmpty(sdList) && ListUtil.isNullOrEmpty(csList) && ListUtil.isNullOrEmpty(jdList) && ListUtil.isNullOrEmpty(xdList)) {
         for (int i = 0; i < lbList.size(); i++) {
            if (i == lbList.size() - 1) {
               text = text + "(fulltext('(content:" + lbList.get(i) + ")','content-word-query-parser')=true)";
            } else {
               text = text + "(fulltext('(content:" + lbList.get(i) + ")','content-word-query-parser')=true)" + " and ";
            }
         }
      } else if (!ListUtil.isNullOrEmpty(jdList)) {
         text = text + getJDText(lbList, sdList, csList, jdList);
      } else if (!ListUtil.isNullOrEmpty(csList)) {
         text = text + getCSText(lbList, sdList, csList);
      } else if (!ListUtil.isNullOrEmpty(sdList)) {
         text = text + getSDText(lbList, sdList);
      } else if (!ListUtil.isNullOrEmpty(xdList)) {
      }

      return text;
   }

   private static String getDXValue(String abs, String now, int status) {
      if (StringUtils.isNullOrEmpry(now)) {
         return abs;
      } else {
         String[] absArr = abs.split(":");
         String[] nowArr = abs.split(":");
         if (status == 1 && Integer.parseInt(nowArr[1]) < Integer.parseInt(absArr[1])) {
            nowArr[0] = absArr[0];
            nowArr[1] = absArr[1];
         }

         if (status == 2) {
            nowArr[1] = nowArr[1] + "|" + absArr[1];
         }

         if (status == 3 && Integer.parseInt(nowArr[1]) > Integer.parseInt(absArr[1])) {
            nowArr[0] = absArr[0];
            nowArr[1] = absArr[1];
         }

         return nowArr[0] + ";" + nowArr[1];
      }
   }

   private static String dealValue(String value) {
      DecimalFormat df = new DecimalFormat("0000000000");
      return df.format(Integer.parseInt(value) * 1000);
   }

   private static String getJYJDText(
      String minValue, String maxValue, List<String> kh, List<String> csList, List<String> zqList, List<String> zhList, List<String> jdList
   ) {
      String text = "";
      List<String> khList = new ArrayList<>();

      for (int i = 0; i < kh.size(); i++) {
         String khs = kh.get(i);
         khs = khs.replace("{", "(").replace("}", ")");
         khList.add(khs);
      }

      for (int i = 0; i < jdList.size(); i++) {
         String[] jdArr = jdList.get(i).split(":");
         if (ListUtil.isNullOrEmpty(csList) && ListUtil.isNullOrEmpty(zqList) && ListUtil.isNullOrEmpty(zhList)) {
            String text0 = "s\\" + khList.get(0) + "s20" + minValue + ",s20" + maxValue + "\\" + khList.get(1);
            String text1 = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
            String text2 = "s\\" + khList.get(0) + "s00" + minValue + ",s00" + maxValue + "\\" + khList.get(1);
            String text3 = "s\\" + khList.get(0) + "s10" + minValue + ",s10" + maxValue + "\\" + khList.get(1);
            String text4 = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
            if (jdArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
               if (i == jdList.size() - 1) {
                  text = text
                     + "(fulltext('content:VS#"
                     + text0
                     + "#"
                     + jdArr[1]
                     + "')=true OR"
                     + " fulltext('content:VS#"
                     + text1
                     + "#"
                     + jdArr[1]
                     + "')=true OR "
                     + " fulltext('content:VS#"
                     + text2
                     + "#"
                     + jdArr[1]
                     + "')=true OR "
                     + " fulltext('content:VS#"
                     + text3
                     + "#"
                     + jdArr[1]
                     + "')=true OR "
                     + " fulltext('content:VS#"
                     + text4
                     + "#"
                     + jdArr[1]
                     + "')=true)";
               } else {
                  text = text
                     + "(fulltext('content:VS#"
                     + text0
                     + "#"
                     + jdArr[1]
                     + "')=true OR"
                     + " fulltext('content:VS#"
                     + text1
                     + "#"
                     + jdArr[1]
                     + "')=true OR "
                     + " fulltext('content:VS#"
                     + text2
                     + "#"
                     + jdArr[1]
                     + "')=true OR "
                     + " fulltext('content:VS#"
                     + text3
                     + "#"
                     + jdArr[1]
                     + "')=true OR "
                     + " fulltext('content:VS#"
                     + text4
                     + "#"
                     + jdArr[1]
                     + "')=true) AND ";
               }
            } else if (i == jdList.size() - 1) {
               text = text
                  + "(fulltext('content:"
                  + text0
                  + "#VE#"
                  + jdArr[1]
                  + "')=true OR "
                  + "fulltext('content:"
                  + text1
                  + "#VE#"
                  + jdArr[1]
                  + "')=true OR "
                  + "fulltext('content:"
                  + text2
                  + "#VE#"
                  + jdArr[1]
                  + "')=true OR "
                  + "fulltext('content:"
                  + text3
                  + "#VE#"
                  + jdArr[1]
                  + "')=true OR "
                  + "fulltext('content:"
                  + text4
                  + "#VE#"
                  + jdArr[1]
                  + " ')=true)";
            } else {
               text = text
                  + "(fulltext('content:"
                  + text0
                  + "#VE#"
                  + jdArr[1]
                  + "')=true OR "
                  + "fulltext('content:"
                  + text1
                  + "#VE#"
                  + jdArr[1]
                  + "')=true OR "
                  + "fulltext('content:"
                  + text2
                  + "#VE#"
                  + jdArr[1]
                  + "')=true OR "
                  + "fulltext('content:"
                  + text3
                  + "#VE#"
                  + jdArr[1]
                  + "')=true OR "
                  + "fulltext('content:"
                  + text4
                  + "#VE#"
                  + jdArr[1]
                  + " ')=true) AND ";
            }
         } else if (!ListUtil.isNullOrEmpty(csList)) {
            for (int j = 0; j < csList.size(); j++) {
               String csStr = csList.get(j);
               String[] csArr = csStr.split(":");
               if (ListUtil.isNullOrEmpty(zqList) && ListUtil.isNullOrEmpty(zhList)) {
                  if (i == jdList.size() - 1 && j == csList.size() - 1) {
                     text = text + getJYJDCSSD(minValue, maxValue, "", csArr, khList, jdArr, 1, 1);
                  } else {
                     text = text + getJYJDCSSD(minValue, maxValue, "", csArr, khList, jdArr, 1, 1) + " AND ";
                  }
               } else if (!ListUtil.isNullOrEmpty(zqList) && !ListUtil.isNullOrEmpty(zhList)) {
                  if (!ListUtil.isNullOrEmpty(zqList)) {
                     for (int m = 0; m < zqList.size(); m++) {
                        for (int n = 0; n < zhList.size(); n++) {
                           if (i == jdList.size() - 1 && j == csList.size() - 1 && m == zqList.size() - 1 && n == zhList.size() - 1) {
                              text = text + getJYJDCSSD(minValue, maxValue, "", csArr, khList, jdArr, 1, 1) + " AND ";
                           } else {
                              text = text + getJYJDCSSD(minValue, maxValue, "", csArr, khList, jdArr, 1, 1) + " AND ";
                           }
                        }
                     }
                  }

                  if (!ListUtil.isNullOrEmpty(zhList)) {
                     for (int m = 0; m < zhList.size(); m++) {
                        if (i == jdList.size() - 1 && j == csList.size() - 1 && m == zhList.size() - 1) {
                           text = text + getJYJDCSSD(minValue, maxValue, "", csArr, khList, jdArr, 0, 1);
                        } else {
                           text = text + getJYJDCSSD(minValue, maxValue, "", csArr, khList, jdArr, 0, 1) + " AND ";
                        }
                     }
                  }
               } else {
                  if (!ListUtil.isNullOrEmpty(zqList)) {
                     for (int mx = 0; mx < zqList.size(); mx++) {
                        if (i == jdList.size() - 1 && j == csList.size() - 1 && mx == zqList.size() - 1) {
                           text = text + getJYJDCSSD(minValue, maxValue, "", csArr, khList, jdArr, 1, 0);
                        } else {
                           text = text + getJYJDCSSD(minValue, maxValue, "", csArr, khList, jdArr, 1, 0) + " AND ";
                        }
                     }
                  }

                  if (!ListUtil.isNullOrEmpty(zhList)) {
                     for (int mxx = 0; mxx < zhList.size(); mxx++) {
                        if (i == jdList.size() - 1 && j == csList.size() - 1 && mxx == zhList.size() - 1) {
                           text = text + getJYJDCSSD(minValue, maxValue, "", csArr, khList, jdArr, 0, 1);
                        } else {
                           text = text + getJYJDCSSD(minValue, maxValue, "", csArr, khList, jdArr, 0, 1) + " AND ";
                        }
                     }
                  }
               }
            }
         } else if (!ListUtil.isNullOrEmpty(zqList) && !ListUtil.isNullOrEmpty(zhList)) {
            if (!ListUtil.isNullOrEmpty(zqList)) {
               String sd = zqList.get(i);
               String text1 = "";
               String text2 = "";
               if (sd.equals(ModelTagDimension.OZUOXICODE)) {
                  text1 = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
                  text2 = "s\\" + khList.get(0) + "s00" + minValue + ",s00" + maxValue + "\\" + khList.get(1);
               } else {
                  text1 = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
                  text2 = "s\\" + khList.get(0) + "s10" + minValue + ",s10" + maxValue + "\\" + khList.get(1);
               }

               if (jdArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                  text = text + "(fulltext('content:VS#" + text1 + " OR content:VS#" + text2 + "')=true) and ";
               } else {
                  text = text + "(fulltext('content:" + text1 + "#VE#" + jdArr[1] + " OR content:" + text2 + "#VE#" + jdArr[1] + "')=true) and ";
               }
            }

            if (!ListUtil.isNullOrEmpty(zhList)) {
               for (int jx = 0; jx < zhList.size(); jx++) {
                  String sdx = zhList.get(i);
                  String text1x = "";
                  String text2x = "";
                  if (sdx.equals(ModelTagDimension.OZUOXICODE)) {
                     text1x = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
                     text2x = "s\\" + khList.get(0) + "s00" + minValue + ",s00" + maxValue + "\\" + khList.get(1);
                  } else {
                     text1x = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
                     text2x = "s\\" + khList.get(0) + "s10" + minValue + ",s10" + maxValue + "\\" + khList.get(1);
                  }

                  if (jdArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                     if (i == jdList.size() - 1 && jx == zhList.size() - 1) {
                        text = text + "(fulltext('content:VS#" + text1x + "#" + jdArr[1] + " OR content:VS#" + text2x + "#" + jdArr[1] + "')=true)";
                     } else {
                        text = text + "(fulltext('content:VS#" + text1x + " OR content:VS#" + text2x + "')=true) and ";
                     }
                  } else if (i == jdList.size() - 1 && jx == zhList.size() - 1) {
                     text = text + "(fulltext('content:" + text1x + "#VE#" + jdArr[1] + " OR content:" + text2x + "#VE#" + jdArr[1] + "')=true)";
                  } else {
                     text = text + "(fulltext('content:" + text1x + "#VE#" + jdArr[1] + " OR content:" + text2x + "#VE#" + jdArr[1] + "')=true) and ";
                  }
               }
            }
         } else {
            if (!ListUtil.isNullOrEmpty(zqList)) {
               String sdxx = zqList.get(i);
               String text1xx = "";
               String text2xx = "";
               if (sdxx.equals(ModelTagDimension.OZUOXICODE)) {
                  text1xx = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
                  text2xx = "s\\" + khList.get(0) + "s00" + minValue + ",s00" + maxValue + "\\" + khList.get(1);
               } else {
                  text1xx = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
                  text2xx = "s\\" + khList.get(0) + "s10" + minValue + ",s10" + maxValue + "\\" + khList.get(1);
               }

               if (jdArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                  if (i == jdList.size() - 1) {
                     text = text + "(fulltext('content:VS#" + text1xx + "#" + jdArr[1] + " OR content:VS#" + text2xx + "#" + jdArr[1] + "')=true)";
                  } else {
                     text = text + "(fulltext('content:VS#" + text1xx + " OR content:VS#" + text2xx + "')=true) and ";
                  }
               } else if (i == jdList.size() - 1) {
                  text = text + "(fulltext('content:" + text1xx + "#VE#" + jdArr[1] + " OR content:" + text2xx + "#VE#" + jdArr[1] + " ')=true)";
               } else {
                  text = text + "(fulltext('content:" + text1xx + "#VE#" + jdArr[1] + " OR content:" + text2xx + "#VE#" + jdArr[1] + " ')true) and ";
               }
            }

            if (!ListUtil.isNullOrEmpty(zhList)) {
               for (int jx = 0; jx < zhList.size(); jx++) {
                  String sdxxx = zhList.get(i);
                  String text1xxx = "";
                  String text2xxx = "";
                  if (sdxxx.equals(ModelTagDimension.OZUOXICODE)) {
                     text1xxx = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
                     text2xxx = "s\\" + khList.get(0) + "s00" + minValue + ",s00" + maxValue + "\\" + khList.get(1);
                  } else {
                     text1xxx = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
                     text2xxx = "s\\" + khList.get(0) + "s10" + minValue + ",s10" + maxValue + "\\" + khList.get(1);
                  }

                  if (jdArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                     if (i == jdList.size() - 1) {
                        text = text + "(fulltext('content:VS#" + text1xxx + "#" + jdArr[1] + " OR content:VS#" + text2xxx + "#" + jdArr[1] + "')=true)";
                     } else {
                        text = text + "(fulltext('content:VS#" + text1xxx + " OR content:VS#" + text2xxx + "')=true) and ";
                     }
                  } else if (i == jdList.size() - 1) {
                     text = text + "(fulltext('content:" + text1xxx + "#VE#" + jdArr[1] + " OR content:" + text2xxx + "#VE#" + jdArr[1] + " ')=true)";
                  } else {
                     text = text + "(fulltext('content:" + text1xxx + "#VE#" + jdArr[1] + " OR content:" + text2xxx + "#VE#" + jdArr[1] + " ')=true) and ";
                  }
               }
            }
         }
      }

      return text;
   }

   private static String getJYJDCSSD(String minValue, String maxValue, String sd, String[] csArr, List<String> khList, String[] jdArr, int zq, int zh) {
      String repeatText = "";
      String repeatText2 = "";
      String repeatText3 = "";
      String repeatText4 = "";
      String totalText = "";
      String falseText = "";
      String resultText = "";
      if (StringUtils.isNullOrEmpry(sd)) {
         repeatText = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
         repeatText2 = "s\\" + khList.get(0) + "s00" + minValue + ",s00" + maxValue + "\\" + khList.get(1);
         repeatText3 = "s\\" + khList.get(0) + "s10" + minValue + ",s10" + maxValue + "\\" + khList.get(1);
         repeatText4 = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
      } else if (zq == 0) {
         if (sd.equals(ModelTagDimension.OZUOXICODE)) {
            repeatText = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
            repeatText2 = "s\\" + khList.get(0) + "s00" + minValue + ",s00" + maxValue + "\\" + khList.get(1);
            repeatText3 = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
            repeatText4 = "s\\" + khList.get(0) + "s00" + minValue + ",s00" + maxValue + "\\" + khList.get(1);
         } else {
            repeatText = "s\\" + khList.get(0) + "s10" + minValue + ",s10" + maxValue + "\\" + khList.get(1);
            repeatText2 = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
            repeatText3 = "s\\" + khList.get(0) + "s10" + minValue + ",s10" + maxValue + "\\" + khList.get(1);
            repeatText4 = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
         }
      } else if (sd.equals(ModelTagDimension.OZUOXICODE)) {
         repeatText = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
         repeatText2 = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
         repeatText3 = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
         repeatText4 = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
      } else {
         repeatText = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
         repeatText2 = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
         repeatText3 = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
         repeatText4 = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
      }

      if (jdArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
         if (csArr[0].equals(ModelTagDimension.ODAYUCODE)) {
            totalText = "content:(VS#"
               + getWordTextTimes(repeatText, Integer.parseInt(csArr[1]) + 1)
               + "#"
               + jdArr[1]
               + ") OR "
               + "content:(VS#"
               + getWordTextTimes(repeatText2, Integer.parseInt(csArr[1]) + 1)
               + "#"
               + jdArr[1]
               + ") OR "
               + "content:(VS#"
               + getWordTextTimes(repeatText3, Integer.parseInt(csArr[1]) + 1)
               + "#"
               + jdArr[1]
               + ") OR "
               + "content:(VS#"
               + getWordTextTimes(repeatText4, Integer.parseInt(csArr[1]) + 1)
               + "#"
               + jdArr[1]
               + ")";
         } else if (csArr[0].equals(ModelTagDimension.ODAYUDENGYUCODE)) {
            totalText = "content:(VS#"
               + getWordTextTimes(repeatText, Integer.parseInt(csArr[1]))
               + "#"
               + jdArr[1]
               + ") OR "
               + "content:(VS#"
               + getWordTextTimes(repeatText2, Integer.parseInt(csArr[1]))
               + "#"
               + jdArr[1]
               + ") OR "
               + "content:(VS#"
               + getWordTextTimes(repeatText3, Integer.parseInt(csArr[1]))
               + "#"
               + jdArr[1]
               + ") OR "
               + "content:(VS#"
               + getWordTextTimes(repeatText4, Integer.parseInt(csArr[1]))
               + "#"
               + jdArr[1]
               + ")";
         } else if (csArr[0].equals(ModelTagDimension.OXIAOYUCODE)) {
            totalText = "content:(VS#"
               + getWordTextTimes(repeatText, Integer.parseInt(csArr[1]))
               + "#"
               + jdArr[1]
               + ") AND "
               + "content:(VS#"
               + getWordTextTimes(repeatText2, Integer.parseInt(csArr[1]))
               + "#"
               + jdArr[1]
               + ") AND "
               + "content:(VS#"
               + getWordTextTimes(repeatText3, Integer.parseInt(csArr[1]))
               + "#"
               + jdArr[1]
               + ") AND "
               + "content:(VS#"
               + getWordTextTimes(repeatText4, Integer.parseInt(csArr[1]))
               + "#"
               + jdArr[1]
               + ")";
         } else if (csArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
            totalText = "content:(VS#"
               + getWordTextTimes(repeatText, Integer.parseInt(csArr[1]) + 1)
               + "#"
               + jdArr[1]
               + ") OR "
               + "content:(VS#"
               + getWordTextTimes(repeatText2, Integer.parseInt(csArr[1]) + 1)
               + "#"
               + jdArr[1]
               + ") OR "
               + "content:(VS#"
               + getWordTextTimes(repeatText3, Integer.parseInt(csArr[1]) + 1)
               + "#"
               + jdArr[1]
               + ") OR "
               + "content:(VS#"
               + getWordTextTimes(repeatText4, Integer.parseInt(csArr[1]) + 1)
               + "#"
               + jdArr[1]
               + ")";
         } else if (csArr[0].equals(ModelTagDimension.ODENGYUCODE)) {
            falseText = "content:(VS#"
               + getWordTextTimes(repeatText, Integer.parseInt(csArr[1]) + 1)
               + "#"
               + jdArr[1]
               + ") OR "
               + "content:(VS#"
               + getWordTextTimes(repeatText2, Integer.parseInt(csArr[1]) + 1)
               + "#"
               + jdArr[1]
               + ") OR "
               + "content:(VS#"
               + getWordTextTimes(repeatText3, Integer.parseInt(csArr[1]) + 1)
               + "#"
               + jdArr[1]
               + ") OR "
               + "content:(VS#"
               + getWordTextTimes(repeatText4, Integer.parseInt(csArr[1]) + 1)
               + "#"
               + jdArr[1]
               + ")";
            totalText = "content:(VS#"
               + getWordTextTimes(repeatText, Integer.parseInt(csArr[1]) + 1)
               + "#"
               + jdArr[1]
               + ") OR "
               + "content:(VS#"
               + getWordTextTimes(repeatText2, Integer.parseInt(csArr[1]) + 1)
               + "#"
               + jdArr[1]
               + ") OR "
               + "content:(VS#"
               + getWordTextTimes(repeatText3, Integer.parseInt(csArr[1]) + 1)
               + "#"
               + jdArr[1]
               + ") OR "
               + "content:(VS#"
               + getWordTextTimes(repeatText4, Integer.parseInt(csArr[1]) + 1)
               + "#"
               + jdArr[1]
               + ")";
         }
      } else if (csArr[0].equals(ModelTagDimension.ODAYUCODE)) {
         totalText = "content:("
            + getWordTextTimes(repeatText, Integer.parseInt(csArr[1]) + 1)
            + "#VE#"
            + jdArr[1]
            + ") OR "
            + "content:("
            + getWordTextTimes(repeatText2, Integer.parseInt(csArr[1]) + 1)
            + "#VE#"
            + jdArr[1]
            + ") OR "
            + "content:("
            + getWordTextTimes(repeatText3, Integer.parseInt(csArr[1]) + 1)
            + "#VE#"
            + jdArr[1]
            + ") OR "
            + "content:("
            + getWordTextTimes(repeatText4, Integer.parseInt(csArr[1]) + 1)
            + "#VE#"
            + jdArr[1]
            + ")";
      } else if (csArr[0].equals(ModelTagDimension.ODAYUDENGYUCODE)) {
         totalText = "content:("
            + getWordTextTimes(repeatText, Integer.parseInt(csArr[1]))
            + "#VE#"
            + jdArr[1]
            + ") OR "
            + "content:("
            + getWordTextTimes(repeatText2, Integer.parseInt(csArr[1]))
            + "#VE#"
            + jdArr[1]
            + ") OR "
            + "content:("
            + getWordTextTimes(repeatText3, Integer.parseInt(csArr[1]))
            + "#VE#"
            + jdArr[1]
            + ") OR "
            + "content:("
            + getWordTextTimes(repeatText4, Integer.parseInt(csArr[1]))
            + "#VE#"
            + jdArr[1]
            + ")";
      } else if (csArr[0].equals(ModelTagDimension.OXIAOYUCODE)) {
         totalText = "content:("
            + getWordTextTimes(repeatText, Integer.parseInt(csArr[1]))
            + "#VE#"
            + jdArr[1]
            + ") AND "
            + "content:("
            + getWordTextTimes(repeatText2, Integer.parseInt(csArr[1]))
            + "#VE#"
            + jdArr[1]
            + ") AND "
            + "content:("
            + getWordTextTimes(repeatText3, Integer.parseInt(csArr[1]))
            + "#VE#"
            + jdArr[1]
            + ") AND "
            + "content:("
            + getWordTextTimes(repeatText4, Integer.parseInt(csArr[1]))
            + "#VE#"
            + jdArr[1]
            + ")";
      } else if (csArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
         totalText = "content:("
            + getWordTextTimes(repeatText, Integer.parseInt(csArr[1]) + 1)
            + "#VE#"
            + jdArr[1]
            + ") OR "
            + "content:("
            + getWordTextTimes(repeatText2, Integer.parseInt(csArr[1]) + 1)
            + "#VE#"
            + jdArr[1]
            + ") OR "
            + "content:("
            + getWordTextTimes(repeatText3, Integer.parseInt(csArr[1]) + 1)
            + "#VE#"
            + jdArr[1]
            + ") OR "
            + "content:("
            + getWordTextTimes(repeatText4, Integer.parseInt(csArr[1]) + 1)
            + "#VE#"
            + jdArr[1]
            + ")";
      } else if (csArr[0].equals(ModelTagDimension.ODENGYUCODE)) {
         falseText = "content:("
            + getWordTextTimes(repeatText, Integer.parseInt(csArr[1]) + 1)
            + "#VE#"
            + jdArr[1]
            + ") OR "
            + "content:("
            + getWordTextTimes(repeatText2, Integer.parseInt(csArr[1]) + 1)
            + "#VE#"
            + jdArr[1]
            + ") OR "
            + "content:("
            + getWordTextTimes(repeatText3, Integer.parseInt(csArr[1]) + 1)
            + "#VE#"
            + jdArr[1]
            + ") OR "
            + "content:("
            + getWordTextTimes(repeatText4, Integer.parseInt(csArr[1]) + 1)
            + "#VE#"
            + jdArr[1]
            + ")";
         totalText = "content:("
            + getWordTextTimes(repeatText, Integer.parseInt(csArr[1]) + 1)
            + "#VE#"
            + jdArr[1]
            + ") OR "
            + "content:("
            + getWordTextTimes(repeatText2, Integer.parseInt(csArr[1]) + 1)
            + "#VE#"
            + jdArr[1]
            + ") OR "
            + "content:("
            + getWordTextTimes(repeatText3, Integer.parseInt(csArr[1]) + 1)
            + "#VE#"
            + jdArr[1]
            + ") OR "
            + "content:("
            + getWordTextTimes(repeatText4, Integer.parseInt(csArr[1]) + 1)
            + "#VE#"
            + jdArr[1]
            + ")";
      }

      if (StringUtils.isNullOrEmpry(falseText)) {
         resultText = "(fulltext('" + totalText + "')=true) ";
      } else {
         resultText = "((fulltext('" + falseText + "')=false) AND (fulltext('" + totalText + "')=true)) ";
      }

      return resultText;
   }

   private static String getJYCSText(String minValue, String maxValue, List<String> kh, List<String> csList, List<String> zqList, List<String> zhList) {
      String text = "";
      List<String> khList = new ArrayList<>();

      for (int i = 0; i < kh.size(); i++) {
         String khs = kh.get(i);
         khs = khs.replace("{", "(").replace("}", ")");
         khList.add(khs);
      }

      for (int i = 0; i < csList.size(); i++) {
         String csStr = csList.get(i);
         String[] csArr = csStr.split(":");
         if (ListUtil.isNullOrEmpty(zqList) && ListUtil.isNullOrEmpty(zhList)) {
            String repeatText0 = "s\\" + khList.get(0) + "n00" + minValue + ",n00" + maxValue + "\\" + khList.get(1);
            String repeatTotal = "s\\[s\\*\\,s\\*\\]";
            if (csArr[0].equals(ModelTagDimension.ODAYUCODE)) {
               if (i == csList.size() - 1) {
                  text = text + "(fulltext('content:(" + getWordsText(repeatText0, Integer.parseInt(csArr[1]) + 1) + ")')=true)";
               } else {
                  text = text + "(fulltext('content:(" + getWordsText(repeatText0, Integer.parseInt(csArr[1]) + 1) + ")')=true) and";
               }
            } else if (csArr[0].equals(ModelTagDimension.ODAYUDENGYUCODE)) {
               if (i == csList.size() - 1) {
                  text = text + "(fulltext('content:(" + getWordsText(repeatText0, Integer.parseInt(csArr[1])) + ")')=true )";
               } else {
                  text = text + "(fulltext('content:(" + getWordsText(repeatText0, Integer.parseInt(csArr[1])) + ")')=true ) and";
               }
            } else if (csArr[0].equals(ModelTagDimension.OXIAOYUCODE)) {
               if (i == csList.size() - 1) {
                  text = text + "(fulltext('content:(" + getWordsText(repeatText0, Integer.parseInt(csArr[1])) + ")')=false) ";
               } else {
                  text = text + "(fulltext('content:(" + getWordsText(repeatText0, Integer.parseInt(csArr[1])) + ")')=false) AND ";
               }
            } else if (csArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
               if (i == csList.size() - 1) {
                  text = text + "(fulltext('content:(" + getWordsText(repeatText0, Integer.parseInt(csArr[1]) + 1) + ")')=false)";
               } else {
                  text = text + "(fulltext('content:(" + getWordsText(repeatText0, Integer.parseInt(csArr[1]) + 1) + ")')=false) and ";
               }
            } else if (csArr[0].equals(ModelTagDimension.ODENGYUCODE)) {
               if (i == csList.size() - 1) {
                  text = text
                     + "((fulltext('content:("
                     + getWordsText(repeatText0, Integer.parseInt(csArr[1]) + 1)
                     + ")')=false ) AND "
                     + "(fulltext('content:("
                     + getWordsText(repeatText0, Integer.parseInt(csArr[1]))
                     + ")')=true ))";
               } else {
                  text = text
                     + "((fulltext('content:("
                     + getWordsText(repeatText0, Integer.parseInt(csArr[1]) + 1)
                     + ")')=false ) AND "
                     + "(fulltext('content:("
                     + getWordsText(repeatText0, Integer.parseInt(csArr[1]))
                     + ")')=true )) AND ";
               }
            }
         } else if (!ListUtil.isNullOrEmpty(zqList) && !ListUtil.isNullOrEmpty(zhList)) {
            for (int j = 0; j < zqList.size(); j++) {
               for (int m = 0; m < zhList.size(); m++) {
                  String zqSD = zqList.get(j);
                  String zhSD = zhList.get(m);
                  String repeatStr = "";
                  boolean haveAnd = true;
                  if (i == csList.size() - 1 && j == zqList.size() - 1 && m == zhList.size() - 1) {
                     haveAnd = false;
                  }

                  String repeatText0 = "s\\" + khList.get(0) + "s20" + minValue + ",s20" + maxValue + "\\" + khList.get(1);
                  String repeatText = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
                  String repeatText2 = "s\\" + khList.get(0) + "s00" + minValue + ",s00" + maxValue + "\\" + khList.get(1);
                  String repeatText3 = "s\\" + khList.get(0) + "s10" + minValue + ",s10" + maxValue + "\\" + khList.get(1);
                  String repeatText4 = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
                  if (zqSD.equals(ModelTagDimension.OZUOXICODE) && zhSD.equals(ModelTagDimension.OZUOXICODE)) {
                     repeatStr = "s\\" + khList.get(0) + "s00" + minValue + ",s00" + maxValue + "\\" + khList.get(1);
                  } else if (zqSD.equals(ModelTagDimension.OZUOXICODE) && zhSD.equals(ModelTagDimension.OKEHUCODE)) {
                     repeatStr = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
                  } else if (zqSD.equals(ModelTagDimension.OKEHUCODE) && zhSD.equals(ModelTagDimension.OZUOXICODE)) {
                     repeatStr = "s\\" + khList.get(0) + "s10" + minValue + ",s10" + maxValue + "\\" + khList.get(1);
                  } else if (zqSD.equals(ModelTagDimension.OKEHUCODE) && zhSD.equals(ModelTagDimension.OKEHUCODE)) {
                     repeatStr = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
                  }

                  if (csArr[0].equals(ModelTagDimension.ODAYUCODE)) {
                     text = text + "(fulltext('content:" + getWordsText(repeatStr, Integer.parseInt(csArr[1]) + 1) + "')=true)";
                  } else if (csArr[0].equals(ModelTagDimension.ODAYUDENGYUCODE)) {
                     text = text + "(fulltext('content:" + getWordsText(repeatStr, Integer.parseInt(csArr[1])) + "')=true)";
                  } else if (csArr[0].equals(ModelTagDimension.OXIAOYUCODE)) {
                     text = text
                        + "((fulltext('content:("
                        + getWordsText(repeatText0, Integer.parseInt(csArr[1]))
                        + ")')=false AND "
                        + "fulltext('content:("
                        + getWordsText(repeatText, Integer.parseInt(csArr[1]))
                        + ")')=false AND "
                        + "fulltext('content:("
                        + getWordsText(repeatText2, Integer.parseInt(csArr[1]))
                        + ")')=false AND "
                        + "fulltext('content:("
                        + getWordsText(repeatText3, Integer.parseInt(csArr[1]))
                        + ")')=false AND "
                        + "fulltext('content:("
                        + getWordsText(repeatText4, Integer.parseInt(csArr[1]))
                        + ")')=false ) AND "
                        + "(fulltext('content:"
                        + repeatStr
                        + "#')=true))";
                  } else if (csArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                     text = text + "(fulltext('content:" + getWordsText(repeatStr, Integer.parseInt(csArr[1]) + 1) + "')=false)";
                  } else if (csArr[0].equals(ModelTagDimension.ODENGYUCODE)) {
                     text = text
                        + "(fulltext('content:"
                        + getWordsText(repeatStr, Integer.parseInt(csArr[1]) + 1)
                        + "')=false AND "
                        + "fulltext('content:"
                        + getWordsText(repeatStr, Integer.parseInt(csArr[1]))
                        + "')=true)";
                  }

                  if (haveAnd) {
                     text = text + " AND ";
                  }
               }
            }
         } else {
            String repeatText0x = "s\\" + khList.get(0) + "s20" + minValue + ",s20" + maxValue + "\\" + khList.get(1);
            String repeatText1 = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
            String repeatText22 = "s\\" + khList.get(0) + "s00" + minValue + ",s00" + maxValue + "\\" + khList.get(1);
            String repeatText3x = "s\\" + khList.get(0) + "s10" + minValue + ",s10" + maxValue + "\\" + khList.get(1);
            String repeatText4x = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
            if (!ListUtil.isNullOrEmpty(zqList)) {
               for (int j = 0; j < zqList.size(); j++) {
                  String sd = zqList.get(j);
                  String repeatTextx = "";
                  String repeatText2x = "";
                  if (sd.equals(ModelTagDimension.OZUOXICODE)) {
                     repeatTextx = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
                     repeatText2x = "s\\" + khList.get(0) + "s00" + minValue + ",s00" + maxValue + "\\" + khList.get(1);
                  } else {
                     repeatTextx = "s\\" + khList.get(0) + "s10" + minValue + ",s10" + maxValue + "\\" + khList.get(1);
                     repeatText2x = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
                  }

                  if (csArr[0].equals(ModelTagDimension.ODAYUCODE)) {
                     if (i == csList.size() - 1 && j == zqList.size() - 1) {
                        text = text
                           + "(fulltext('content:("
                           + getWordsText(repeatTextx, Integer.parseInt(csArr[1]) + 1)
                           + ")')=true OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2x, Integer.parseInt(csArr[1]) + 1)
                           + "')=true)";
                     } else {
                        text = text
                           + "(fulltext('content:"
                           + getWordsText(repeatTextx, Integer.parseInt(csArr[1]) + 1)
                           + "')=true OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2x, Integer.parseInt(csArr[1]) + 1)
                           + "')=true) AND ";
                     }
                  } else if (csArr[0].equals(ModelTagDimension.ODAYUDENGYUCODE)) {
                     if (i == csList.size() - 1 && j == zqList.size() - 1) {
                        text = text
                           + "(fulltext('content:("
                           + getWordsText(repeatTextx, Integer.parseInt(csArr[1]))
                           + ")')=true OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2x, Integer.parseInt(csArr[1]))
                           + "')=true)";
                     } else {
                        text = text
                           + "(fulltext('content:"
                           + getWordsText(repeatTextx, Integer.parseInt(csArr[1]))
                           + "')=true OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2x, Integer.parseInt(csArr[1]))
                           + "')=true) AND ";
                     }
                  } else if (csArr[0].equals(ModelTagDimension.OXIAOYUCODE)) {
                     if (i == csList.size() - 1 && j == zqList.size() - 1) {
                        text = text
                           + "((fulltext('content:("
                           + getWordsText(repeatText0x, Integer.parseInt(csArr[1]))
                           + ")')=false AND "
                           + "fulltext('content:("
                           + getWordsText(repeatText1, Integer.parseInt(csArr[1]))
                           + ")')=false AND "
                           + "fulltext('content:("
                           + getWordsText(repeatText22, Integer.parseInt(csArr[1]))
                           + ")')=false AND "
                           + "fulltext('content:("
                           + getWordsText(repeatText3x, Integer.parseInt(csArr[1]))
                           + ")')=false AND "
                           + "fulltext('content:("
                           + getWordsText(repeatText4x, Integer.parseInt(csArr[1]))
                           + ")')=false ) AND "
                           + "(fulltext('content:"
                           + repeatTextx
                           + "#')=true OR fulltext('content:"
                           + repeatText2x
                           + "')=true))";
                     } else {
                        text = text
                           + "((fulltext('content:("
                           + getWordsText(repeatText0x, Integer.parseInt(csArr[1]))
                           + ")')=false AND "
                           + "fulltext('content:("
                           + getWordsText(repeatTextx, Integer.parseInt(csArr[1]))
                           + ")')=false AND "
                           + "fulltext('content:("
                           + getWordsText(repeatText2x, Integer.parseInt(csArr[1]))
                           + ")')=false AND "
                           + "fulltext('content:("
                           + getWordsText(repeatText3x, Integer.parseInt(csArr[1]))
                           + ")')=false AND "
                           + "fulltext('content:("
                           + getWordsText(repeatText4x, Integer.parseInt(csArr[1]))
                           + ")')=false ) AND "
                           + "(fulltext('content:"
                           + repeatTextx
                           + "#')=true OR fulltext('content:"
                           + repeatText2x
                           + "')=true))";
                     }
                  } else if (csArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                     if (i == csList.size() - 1 && j == zqList.size() - 1) {
                        text = text
                           + "(fulltext('content:("
                           + getWordsText(repeatTextx, Integer.parseInt(csArr[1]) + 1)
                           + ")')=false OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2x, Integer.parseInt(csArr[1]) + 1)
                           + "')=false)";
                     } else {
                        text = text
                           + "(fulltext('content:"
                           + getWordsText(repeatTextx, Integer.parseInt(csArr[1]) + 1)
                           + "')=false OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2x, Integer.parseInt(csArr[1]) + 1)
                           + "')=false) AND ";
                     }
                  } else if (csArr[0].equals(ModelTagDimension.ODENGYUCODE)) {
                     if (i == csList.size() - 1 && j == zqList.size() - 1) {
                        text = text
                           + "((fulltext('content:("
                           + getWordsText(repeatTextx, Integer.parseInt(csArr[1]) + 1)
                           + ")')=false OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2x, Integer.parseInt(csArr[1]) + 1)
                           + "')=false) AND "
                           + "(fulltext('content:("
                           + getWordsText(repeatTextx, Integer.parseInt(csArr[1]))
                           + ")')=true OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2x, Integer.parseInt(csArr[1]))
                           + "')=true))";
                     } else {
                        text = text
                           + "((fulltext('content:("
                           + getWordsText(repeatTextx, Integer.parseInt(csArr[1]) + 1)
                           + ")')=false OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2x, Integer.parseInt(csArr[1]) + 1)
                           + "')=false) AND "
                           + "(fulltext('content:("
                           + getWordsText(repeatTextx, Integer.parseInt(csArr[1]))
                           + ")')=false OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2x, Integer.parseInt(csArr[1]))
                           + "')=true)) AND ";
                     }
                  }
               }
            }

            if (!ListUtil.isNullOrEmpty(zhList)) {
               for (int j = 0; j < zhList.size(); j++) {
                  String sdx = zhList.get(j);
                  String repeatTextxx = "";
                  String repeatText2xx = "";
                  if (sdx.equals(ModelTagDimension.OZUOXICODE)) {
                     repeatTextxx = "s\\" + khList.get(0) + "s10" + minValue + ",s10" + maxValue + "\\" + khList.get(1);
                     repeatText2xx = "s\\" + khList.get(0) + "s00" + minValue + ",s00" + maxValue + "\\" + khList.get(1);
                  } else {
                     repeatTextxx = "s\\" + khList.get(0) + "s11" + minValue + ",s11" + maxValue + "\\" + khList.get(1);
                     repeatText2xx = "s\\" + khList.get(0) + "s01" + minValue + ",s01" + maxValue + "\\" + khList.get(1);
                  }

                  if (csArr[0].equals(ModelTagDimension.ODAYUCODE)) {
                     if (i == csList.size() - 1 && j == zhList.size() - 1) {
                        text = text
                           + "(fulltext('content:("
                           + getWordsText(repeatTextxx, Integer.parseInt(csArr[1]) + 1)
                           + ")')=true OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2xx, Integer.parseInt(csArr[1]) + 1)
                           + "')=true)";
                     } else {
                        text = text
                           + "(fulltext('content:"
                           + getWordsText(repeatTextxx, Integer.parseInt(csArr[1]) + 1)
                           + "')=true OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2xx, Integer.parseInt(csArr[1]) + 1)
                           + "')=true) AND ";
                     }
                  } else if (csArr[0].equals(ModelTagDimension.ODAYUDENGYUCODE)) {
                     if (i == csList.size() - 1 && j == zhList.size() - 1) {
                        text = text
                           + "(fulltext('content:("
                           + getWordsText(repeatTextxx, Integer.parseInt(csArr[1]))
                           + ")')=true OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2xx, Integer.parseInt(csArr[1]))
                           + "')=true)";
                     } else {
                        text = text
                           + "(fulltext('content:"
                           + getWordsText(repeatTextxx, Integer.parseInt(csArr[1]))
                           + "')=true OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2xx, Integer.parseInt(csArr[1]))
                           + "')=true) AND ";
                     }
                  } else if (csArr[0].equals(ModelTagDimension.OXIAOYUCODE)) {
                     if (i == csList.size() - 1 && j == zhList.size() - 1) {
                        text = text
                           + "((fulltext('content:("
                           + getWordsText(repeatText0x, Integer.parseInt(csArr[1]))
                           + ")')=false AND "
                           + "fulltext('content:("
                           + getWordsText(repeatText1, Integer.parseInt(csArr[1]))
                           + ")')=false AND "
                           + "fulltext('content:("
                           + getWordsText(repeatText22, Integer.parseInt(csArr[1]))
                           + ")')=false AND "
                           + "fulltext('content:("
                           + getWordsText(repeatText3x, Integer.parseInt(csArr[1]))
                           + ")')=false AND "
                           + "fulltext('content:("
                           + getWordsText(repeatText4x, Integer.parseInt(csArr[1]))
                           + ")')=false ) AND "
                           + "(fulltext('content:"
                           + repeatTextxx
                           + "#')=true OR fulltext('content:"
                           + repeatText2xx
                           + "')=true))";
                     } else {
                        text = text
                           + "((fulltext('content:("
                           + getWordsText(repeatText0x, Integer.parseInt(csArr[1]))
                           + ")')=false AND "
                           + "fulltext('content:("
                           + getWordsText(repeatText1, Integer.parseInt(csArr[1]))
                           + ")')=false AND "
                           + "fulltext('content:("
                           + getWordsText(repeatText22, Integer.parseInt(csArr[1]))
                           + ")')=false AND "
                           + "fulltext('content:("
                           + getWordsText(repeatText3x, Integer.parseInt(csArr[1]))
                           + ")')=false AND "
                           + "fulltext('content:("
                           + getWordsText(repeatText4x, Integer.parseInt(csArr[1]))
                           + ")')=false ) AND "
                           + "(fulltext('content:"
                           + repeatTextxx
                           + "#')=true OR fulltext('content:"
                           + repeatText2xx
                           + "')=true))";
                     }
                  } else if (csArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                     if (i == csList.size() - 1 && j == zhList.size() - 1) {
                        text = text
                           + "(fulltext('content:("
                           + getWordsText(repeatTextxx, Integer.parseInt(csArr[1]) + 1)
                           + ")')=false OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2xx, Integer.parseInt(csArr[1]) + 1)
                           + "')=false)";
                     } else {
                        text = text
                           + "(fulltext('content:"
                           + getWordsText(repeatTextxx, Integer.parseInt(csArr[1]) + 1)
                           + "')=false OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2xx, Integer.parseInt(csArr[1]) + 1)
                           + "')=false) AND ";
                     }
                  } else if (csArr[0].equals(ModelTagDimension.ODENGYUCODE)) {
                     if (i == csList.size() - 1 && j == zhList.size() - 1) {
                        text = text
                           + "((fulltext('content:("
                           + getWordsText(repeatTextxx, Integer.parseInt(csArr[1]) + 1)
                           + ")')=false OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2xx, Integer.parseInt(csArr[1]) + 1)
                           + "')=false) AND "
                           + "(fulltext('content:("
                           + getWordsText(repeatTextxx, Integer.parseInt(csArr[1]))
                           + ")')=true OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2xx, Integer.parseInt(csArr[1]))
                           + "')=true))";
                     } else {
                        text = text
                           + "((fulltext('content:("
                           + getWordsText(repeatTextxx, Integer.parseInt(csArr[1]) + 1)
                           + ")')=false OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2xx, Integer.parseInt(csArr[1]) + 1)
                           + "')=false) AND "
                           + "(fulltext('content:("
                           + getWordsText(repeatTextxx, Integer.parseInt(csArr[1]))
                           + ")')=true OR "
                           + "fulltext('content:"
                           + getWordsText(repeatText2xx, Integer.parseInt(csArr[1]))
                           + "')=true)) AND ";
                     }
                  }
               }
            }
         }
      }

      return text;
   }

   private static String getSDText(List<String> lbList, List<String> sdList) {
      String text = "";

      for (int i = 0; i < sdList.size(); i++) {
         for (int j = 0; j < lbList.size(); j++) {
            if (i == sdList.size() - 1 && j == lbList.size() - 1) {
               text = text + "(fulltext('(" + sdList.get(i) + ":" + lbList.get(j) + ")')=true) ";
            } else {
               text = text + "(fulltext('(" + sdList.get(i) + ":" + lbList.get(j) + ")')=true) and ";
            }
         }
      }

      return text;
   }

   private static String getCSText(List<String> lbList, List<String> sdList, List<String> csList) {
      String text = "";

      for (int i = 0; i < csList.size(); i++) {
         String csStr = csList.get(i);
         String[] csArr = csStr.split(";");
         if (!ListUtil.isNullOrEmpty(sdList)) {
            for (int j = 0; j < sdList.size(); j++) {
               for (int m = 0; m < lbList.size(); m++) {
                  if (csArr[0].equals(ModelTagDimension.ODAYUCODE)) {
                     if (i == csList.size() - 1 && j == sdList.size() - 1 && m == lbList.size() - 1) {
                        text = text + "(fulltext('(" + sdList.get(j) + ":" + getWordsText(lbList.get(m), Integer.parseInt(csArr[1]) + 1) + ")')=true) ";
                     } else {
                        text = text + "(fulltext('(" + sdList.get(j) + ":" + getWordsText(lbList.get(m), Integer.parseInt(csArr[1]) + 1) + ")')=true) and ";
                     }
                  } else if (csArr[0].equals(ModelTagDimension.ODAYUDENGYUCODE)) {
                     if (i == csList.size() - 1 && j == sdList.size() - 1 && m == lbList.size() - 1) {
                        text = text + "(fulltext('(" + sdList.get(j) + ":" + getWordsText(lbList.get(m), Integer.parseInt(csArr[1])) + ")')=true) ";
                     } else {
                        text = text + "(fulltext('(" + sdList.get(j) + ":" + getWordsText(lbList.get(m), Integer.parseInt(csArr[1]) + 1) + ")')=true) and ";
                     }
                  } else if (csArr[0].equals(ModelTagDimension.OXIAOYUCODE)) {
                     if (i == csList.size() - 1 && j == sdList.size() - 1 && m == lbList.size() - 1) {
                        text = text + "(fulltext('(" + sdList.get(j) + ":" + getWordsText(lbList.get(m), Integer.parseInt(csArr[1])) + ")')=false) ";
                     } else {
                        text = text + "(fulltext('(" + sdList.get(j) + ":" + getWordsText(lbList.get(m), Integer.parseInt(csArr[1])) + ")')=false) and ";
                     }
                  } else if (csArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                     if (i == csList.size() - 1 && j == sdList.size() - 1 && m == lbList.size() - 1) {
                        text = text + "(fulltext('(" + sdList.get(j) + ":" + getWordsText(lbList.get(m), Integer.parseInt(csArr[1]) + 1) + ")')=false) ";
                     } else {
                        text = text + "(fulltext('(" + sdList.get(j) + ":" + getWordsText(lbList.get(m), Integer.parseInt(csArr[1]) + 1) + ")')=false) and ";
                     }
                  } else if (csArr[0].equals(ModelTagDimension.ODENGYUCODE)) {
                     if (i == csList.size() - 1 && j == sdList.size() - 1 && m == lbList.size() - 1) {
                        text = text
                           + "(fulltext('("
                           + sdList.get(j)
                           + ":"
                           + getWordsText(lbList.get(m), Integer.parseInt(csArr[1]) + 1)
                           + ")')=false and "
                           + " fulltext('("
                           + sdList.get(j)
                           + ":"
                           + getWordsText(lbList.get(m), Integer.parseInt(csArr[1]))
                           + ")')=true) ";
                     } else {
                        text = text
                           + "(fulltext('("
                           + sdList.get(j)
                           + ":"
                           + getWordsText(lbList.get(m), Integer.parseInt(csArr[1]) + 1)
                           + ")')=false and "
                           + " fulltext('("
                           + sdList.get(j)
                           + ":"
                           + getWordsText(lbList.get(m), Integer.parseInt(csArr[1]))
                           + ")')=true) and ";
                     }
                  }
               }
            }
         } else {
            for (int j = 0; j < lbList.size(); j++) {
               if (csArr[0].equals(ModelTagDimension.ODAYUCODE)) {
                  if (i == csList.size() - 1 && j == lbList.size() - 1) {
                     text = text + "(fulltext('(content:" + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1) + ")')=true) ";
                  } else {
                     text = text + "(fulltext('(content:" + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1) + ")')=true) and ";
                  }
               } else if (csArr[0].equals(ModelTagDimension.ODAYUDENGYUCODE)) {
                  if (i == csList.size() - 1 && j == lbList.size() - 1) {
                     text = text + "(fulltext('(content:" + getWordsText(lbList.get(j), Integer.parseInt(csArr[1])) + ")')=true) ";
                  } else {
                     text = text + "(fulltext('(content:" + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1) + ")')=true) and ";
                  }
               } else if (csArr[0].equals(ModelTagDimension.OXIAOYUCODE)) {
                  if (i == csList.size() - 1 && j == lbList.size() - 1) {
                     text = text + "(fulltext('(content:" + getWordsText(lbList.get(j), Integer.parseInt(csArr[1])) + ")')=false) ";
                  } else {
                     text = text + "(fulltext('(content:" + getWordsText(lbList.get(j), Integer.parseInt(csArr[1])) + ")')=false) and ";
                  }
               } else if (csArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                  if (i == csList.size() - 1 && j == lbList.size() - 1) {
                     text = text + "(fulltext('(content:" + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1) + ")')=false) ";
                  } else {
                     text = text + "(fulltext('(content:" + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1) + ")')=false) and ";
                  }
               } else if (csArr[0].equals(ModelTagDimension.ODENGYUCODE)) {
                  if (i == csList.size() - 1 && j == lbList.size() - 1) {
                     text = text
                        + "(fulltext('(content:"
                        + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1)
                        + ")')=false and "
                        + " fulltext('(content:"
                        + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]))
                        + ")')=true) ";
                  } else {
                     text = text
                        + "(fulltext('(content:"
                        + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1)
                        + ")')=false and "
                        + " fulltext('(content:"
                        + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]))
                        + ")')=true) and ";
                  }
               }
            }
         }
      }

      return text;
   }

   private static String getJDText(List<String> lbList, List<String> sdList, List<String> csList, List<String> jdList) {
      String text = "";

      for (int i = 0; i < jdList.size(); i++) {
         String jdStr = jdList.get(i);
         String[] jdArr = jdStr.split(";");
         if (!ListUtil.isNullOrEmpty(csList)) {
            for (int k = 0; k < csList.size(); k++) {
               String csStr = csList.get(k);
               String[] csArr = csStr.split(";");
               if (!ListUtil.isNullOrEmpty(sdList)) {
                  for (int j = 0; j < sdList.size(); j++) {
                     for (int m = 0; m < lbList.size(); m++) {
                        if (jdArr.equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                           if (csArr[0].equals(ModelTagDimension.ODAYUCODE)) {
                              if (i == jdList.size() - 1 && k == csList.size() - 1 && j == sdList.size() - 1 && m == lbList.size() - 1) {
                                 text = text
                                    + "(fulltext('("
                                    + sdList.get(j)
                                    + ":VS#"
                                    + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]) + 1)
                                    + "#"
                                    + jdArr[1]
                                    + ")')=true) ";
                              } else {
                                 text = text
                                    + "(fulltext('("
                                    + sdList.get(j)
                                    + ":VS#"
                                    + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]) + 1)
                                    + "#"
                                    + jdArr[1]
                                    + ")')=true) and ";
                              }
                           } else if (csArr[0].equals(ModelTagDimension.ODAYUDENGYUCODE)) {
                              if (i == jdList.size() - 1 && k == csList.size() - 1 && j == sdList.size() - 1 && m == lbList.size() - 1) {
                                 text = text
                                    + "(fulltext('("
                                    + sdList.get(j)
                                    + ":VS#"
                                    + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]))
                                    + "#"
                                    + jdArr[1]
                                    + "#"
                                    + jdArr[1]
                                    + ")')=true) ";
                              } else {
                                 text = text
                                    + "(fulltext('("
                                    + sdList.get(j)
                                    + ":VS#"
                                    + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]) + 1)
                                    + "#"
                                    + jdArr[1]
                                    + "#"
                                    + jdArr[1]
                                    + ")')=true) and ";
                              }
                           } else if (csArr[0].equals(ModelTagDimension.OXIAOYUCODE)) {
                              if (i == jdList.size() - 1 && k == csList.size() - 1 && j == sdList.size() - 1 && m == lbList.size() - 1) {
                                 text = text
                                    + "(fulltext('("
                                    + sdList.get(j)
                                    + ":VS#"
                                    + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]))
                                    + "#"
                                    + jdArr[1]
                                    + ")')=false) ";
                              } else {
                                 text = text
                                    + "(fulltext('("
                                    + sdList.get(j)
                                    + ":VS#"
                                    + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]))
                                    + "#"
                                    + jdArr[1]
                                    + ")')=false) and ";
                              }
                           } else if (csArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                              if (i == jdList.size() - 1 && k == csList.size() - 1 && j == sdList.size() - 1 && m == lbList.size() - 1) {
                                 text = text
                                    + "(fulltext('("
                                    + sdList.get(j)
                                    + ":VS#"
                                    + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]) + 1)
                                    + "#"
                                    + jdArr[1]
                                    + ")')=false) ";
                              } else {
                                 text = text
                                    + "(fulltext('("
                                    + sdList.get(j)
                                    + ":VS#"
                                    + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]) + 1)
                                    + "#"
                                    + jdArr[1]
                                    + ")')=false) and ";
                              }
                           } else if (csArr[0].equals(ModelTagDimension.ODENGYUCODE)) {
                              if (i == jdList.size() - 1 && k == csList.size() - 1 && j == sdList.size() - 1 && m == lbList.size() - 1) {
                                 text = text
                                    + "(fulltext('("
                                    + sdList.get(j)
                                    + ":VS#"
                                    + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]) + 1)
                                    + "#"
                                    + jdArr[1]
                                    + ")')=false and "
                                    + " fulltext('("
                                    + sdList.get(j)
                                    + ":VS#"
                                    + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]))
                                    + "#"
                                    + jdArr[1]
                                    + ")')=true) ";
                              } else {
                                 text = text
                                    + "(fulltext('("
                                    + sdList.get(j)
                                    + ":VS#"
                                    + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]) + 1)
                                    + "#"
                                    + jdArr[1]
                                    + ")')=false and "
                                    + " fulltext('("
                                    + sdList.get(j)
                                    + ":VS#"
                                    + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]))
                                    + "#"
                                    + jdArr[1]
                                    + ")')=true) and ";
                              }
                           }
                        } else if (csArr[0].equals(ModelTagDimension.ODAYUCODE)) {
                           if (i == jdList.size() - 1 && k == csList.size() - 1 && j == sdList.size() - 1 && m == lbList.size() - 1) {
                              text = text
                                 + "(fulltext('("
                                 + sdList.get(j)
                                 + ":"
                                 + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]) + 1)
                                 + "#VE#"
                                 + jdArr[1]
                                 + ")')=true) ";
                           } else {
                              text = text
                                 + "(fulltext('("
                                 + sdList.get(j)
                                 + ":"
                                 + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]) + 1)
                                 + "#VE#"
                                 + jdArr[1]
                                 + ")')=true) and ";
                           }
                        } else if (csArr[0].equals(ModelTagDimension.ODAYUDENGYUCODE)) {
                           if (i == jdList.size() - 1 && k == csList.size() - 1 && j == sdList.size() - 1 && m == lbList.size() - 1) {
                              text = text
                                 + "(fulltext('("
                                 + sdList.get(j)
                                 + ":"
                                 + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]))
                                 + "#VE#"
                                 + jdArr[1]
                                 + ")')=true) ";
                           } else {
                              text = text
                                 + "(fulltext('("
                                 + sdList.get(j)
                                 + ":"
                                 + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]) + 1)
                                 + "#VE#"
                                 + jdArr[1]
                                 + ")')=true) and ";
                           }
                        } else if (csArr[0].equals(ModelTagDimension.OXIAOYUCODE)) {
                           if (i == jdList.size() - 1 && k == csList.size() - 1 && j == sdList.size() - 1 && m == lbList.size() - 1) {
                              text = text
                                 + "(fulltext('("
                                 + sdList.get(j)
                                 + ":"
                                 + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]))
                                 + "#VE#"
                                 + jdArr[1]
                                 + ")')=false) ";
                           } else {
                              text = text
                                 + "(fulltext('("
                                 + sdList.get(j)
                                 + ":"
                                 + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]))
                                 + "#VE#"
                                 + jdArr[1]
                                 + ")')=false) and ";
                           }
                        } else if (csArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                           if (i == jdList.size() - 1 && k == csList.size() - 1 && j == sdList.size() - 1 && m == lbList.size() - 1) {
                              text = text
                                 + "(fulltext('("
                                 + sdList.get(j)
                                 + ":"
                                 + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]) + 1)
                                 + "#VE#"
                                 + jdArr[1]
                                 + ")')=false) ";
                           } else {
                              text = text
                                 + "(fulltext('("
                                 + sdList.get(j)
                                 + ":"
                                 + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]) + 1)
                                 + "#VE#"
                                 + jdArr[1]
                                 + ")')=false) and ";
                           }
                        } else if (csArr[0].equals(ModelTagDimension.ODENGYUCODE)) {
                           if (i == jdList.size() - 1 && k == csList.size() - 1 && j == sdList.size() - 1 && m == lbList.size() - 1) {
                              text = text
                                 + "(fulltext('("
                                 + sdList.get(j)
                                 + ":"
                                 + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]) + 1)
                                 + "#VE#"
                                 + jdArr[1]
                                 + ")')=false and "
                                 + " fulltext('("
                                 + sdList.get(j)
                                 + ":"
                                 + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]))
                                 + "#VE#"
                                 + jdArr[1]
                                 + ")')=true) ";
                           } else {
                              text = text
                                 + "(fulltext('("
                                 + sdList.get(j)
                                 + ":"
                                 + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]) + 1)
                                 + "#VE#"
                                 + jdArr[1]
                                 + ")')=false and "
                                 + " fulltext('("
                                 + sdList.get(j)
                                 + ":"
                                 + getWordTextTimes(lbList.get(m), Integer.parseInt(csArr[1]))
                                 + "#VE#"
                                 + jdArr[1]
                                 + ")')=true) and ";
                           }
                        }
                     }
                  }
               } else {
                  for (int j = 0; j < lbList.size(); j++) {
                     if (jdArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                        if (csArr[0].equals(ModelTagDimension.ODAYUCODE)) {
                           if (i == csList.size() - 1 && j == lbList.size() - 1) {
                              text = text
                                 + "(fulltext('(content:VS#"
                                 + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1)
                                 + "#"
                                 + jdArr[1]
                                 + ")')=true) ";
                           } else {
                              text = text
                                 + "(fulltext('(content:VS#"
                                 + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1)
                                 + "#"
                                 + jdArr[1]
                                 + ")')=true) and ";
                           }
                        } else if (csArr[0].equals(ModelTagDimension.ODAYUDENGYUCODE)) {
                           if (i == csList.size() - 1 && j == lbList.size() - 1) {
                              text = text + "(fulltext('(content:VS#" + getWordsText(lbList.get(j), Integer.parseInt(csArr[1])) + "#" + jdArr[1] + ")')=true) ";
                           } else {
                              text = text
                                 + "(fulltext('(content:VS#"
                                 + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1)
                                 + "#"
                                 + jdArr[1]
                                 + ")')=true) and ";
                           }
                        } else if (csArr[0].equals(ModelTagDimension.OXIAOYUCODE)) {
                           if (i == csList.size() - 1 && j == lbList.size() - 1) {
                              text = text
                                 + "(fulltext('(content:VS#"
                                 + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]))
                                 + "#"
                                 + jdArr[1]
                                 + ")')=false) ";
                           } else {
                              text = text
                                 + "(fulltext('(content:VS"
                                 + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]))
                                 + "#"
                                 + jdArr[1]
                                 + ")')=false) and ";
                           }
                        } else if (csArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                           if (i == csList.size() - 1 && j == lbList.size() - 1) {
                              text = text
                                 + "(fulltext('(content:VS#"
                                 + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1)
                                 + "#"
                                 + jdArr[1]
                                 + ")')=false) ";
                           } else {
                              text = text
                                 + "(fulltext('(content:VS#"
                                 + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1)
                                 + "#"
                                 + jdArr[1]
                                 + ")')=false) and ";
                           }
                        } else if (csArr[0].equals(ModelTagDimension.ODENGYUCODE)) {
                           if (i == csList.size() - 1 && j == lbList.size() - 1) {
                              text = text
                                 + "(fulltext('(content:VS#"
                                 + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1)
                                 + "#"
                                 + jdArr[1]
                                 + ")')=false and "
                                 + " fulltext('(content:VS#"
                                 + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]))
                                 + "#"
                                 + jdArr[1]
                                 + ")')=true) ";
                           } else {
                              text = text
                                 + "(fulltext('(content:VS#"
                                 + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1)
                                 + "#"
                                 + jdArr[1]
                                 + ")')=false and "
                                 + " fulltext('(content:VS#"
                                 + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]))
                                 + "#"
                                 + jdArr[1]
                                 + ")')=true) and ";
                           }
                        }
                     } else if (csArr[0].equals(ModelTagDimension.ODAYUCODE)) {
                        if (i == csList.size() - 1 && j == lbList.size() - 1) {
                           text = text
                              + "(fulltext('(content:"
                              + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1)
                              + "#VE#"
                              + jdArr[1]
                              + ")')=true) ";
                        } else {
                           text = text
                              + "(fulltext('(content:"
                              + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1)
                              + "#VE#"
                              + jdArr[1]
                              + ")')=true) and ";
                        }
                     } else if (csArr[0].equals(ModelTagDimension.ODAYUDENGYUCODE)) {
                        if (i == csList.size() - 1 && j == lbList.size() - 1) {
                           text = text + "(fulltext('(content:" + getWordsText(lbList.get(j), Integer.parseInt(csArr[1])) + "#VE#" + jdArr[1] + ")')=true) ";
                        } else {
                           text = text
                              + "(fulltext('(content:"
                              + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1)
                              + "#VE#"
                              + jdArr[1]
                              + ")')=true) and ";
                        }
                     } else if (csArr[0].equals(ModelTagDimension.OXIAOYUCODE)) {
                        if (i == csList.size() - 1 && j == lbList.size() - 1) {
                           text = text + "(fulltext('(content:" + getWordsText(lbList.get(j), Integer.parseInt(csArr[1])) + "#VE#" + jdArr[1] + ")')=false) ";
                        } else {
                           text = text
                              + "(fulltext('(content:"
                              + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]))
                              + "#VE#"
                              + jdArr[1]
                              + ")')=false) and ";
                        }
                     } else if (csArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                        if (i == csList.size() - 1 && j == lbList.size() - 1) {
                           text = text
                              + "(fulltext('(content:"
                              + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1)
                              + "#VE#"
                              + jdArr[1]
                              + ")')=false) ";
                        } else {
                           text = text
                              + "(fulltext('(content:"
                              + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1)
                              + "#VE#"
                              + jdArr[1]
                              + ")')=false) and ";
                        }
                     } else if (csArr[0].equals(ModelTagDimension.ODENGYUCODE)) {
                        if (i == csList.size() - 1 && j == lbList.size() - 1) {
                           text = text
                              + "(fulltext('(content:"
                              + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1)
                              + "#VE#"
                              + jdArr[1]
                              + ")')=false and "
                              + " fulltext('(content:"
                              + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]))
                              + "#VE#"
                              + jdArr[1]
                              + ")')=true) ";
                        } else {
                           text = text
                              + "(fulltext('(content:"
                              + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]) + 1)
                              + "#VE#"
                              + jdArr[1]
                              + ")')=false and "
                              + " fulltext('(content:"
                              + getWordsText(lbList.get(j), Integer.parseInt(csArr[1]))
                              + "#VE#"
                              + jdArr[1]
                              + ")')=true) and ";
                        }
                     }
                  }
               }
            }
         } else if (!ListUtil.isNullOrEmpty(sdList)) {
            for (int n = 0; n < sdList.size(); n++) {
               for (int jx = 0; jx < lbList.size(); jx++) {
                  if (jdArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                     if (i == jdList.size() - 1 && n == sdList.size() - 1 && jx == lbList.size() - 1) {
                        text = text + "(fulltext('(" + sdList.get(i) + ":VS#" + lbList.get(jx) + "#" + jdArr[1] + ")')=true) ";
                     } else {
                        text = text + "(fulltext('(" + sdList.get(i) + ":VS#" + lbList.get(jx) + "#" + jdArr[1] + ")')=true) and ";
                     }
                  } else if (i == jdList.size() - 1 && n == sdList.size() - 1 && jx == lbList.size() - 1) {
                     text = text + "(fulltext('(" + sdList.get(i) + ":" + lbList.get(jx) + "#VE#" + jdArr[1] + ")')=true) ";
                  } else {
                     text = text + "(fulltext('(" + sdList.get(i) + ":" + lbList.get(jx) + "#VE#" + jdArr[1] + ")')=true) and ";
                  }
               }
            }
         } else {
            for (int jxx = 0; jxx < lbList.size(); jxx++) {
               if (jdArr[0].equals(ModelTagDimension.OXIAOYUDENGYUCODE)) {
                  if (i == jdList.size() - 1 && jxx == lbList.size() - 1) {
                     text = text + "(fulltext('(content:VS#" + lbList.get(jxx) + "#" + jdArr[1] + ")')=true) ";
                  } else {
                     text = text + "(fulltext('(content:VS#" + lbList.get(jxx) + "#" + jdArr[1] + ")')=true) and ";
                  }
               } else if (i == jdList.size() - 1 && jxx == lbList.size() - 1) {
                  text = text + "(fulltext('(content:" + lbList.get(jxx) + "#VE#" + jdArr[1] + ")')=true) ";
               } else {
                  text = text + "(fulltext('(content:" + lbList.get(jxx) + "#VE#" + jdArr[1] + ")')=true) and ";
               }
            }
         }
      }

      return text;
   }

   private static String getWordTextTimes(String word, int times) {
      String text = "";

      for (int i = 0; i < times; i++) {
         if (i == times - 1) {
            text = text + word;
         } else {
            text = text + word + "#";
         }
      }

      return text;
   }

   private static String getWordsText(String word, int times) {
      String text = "";

      for (int i = 0; i < times; i++) {
         text = text + word + "#";
      }

      if (times >= 1) {
         text = text + "999999";
      }

      return text;
   }
}
