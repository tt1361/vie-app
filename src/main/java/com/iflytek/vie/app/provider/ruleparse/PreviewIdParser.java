package com.iflytek.vie.app.provider.ruleparse;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.iflytek.vie.app.provider.common.SqlHelper;
import com.iflytek.vie.constants.IndexConstants;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreviewIdParser extends SqlParser implements Serializable {
   private List<String> ids;
   private String channelRule;
   private String textRule;

   public PreviewIdParser(String textRule, int channel, List<Filter> filters, String tableName, List<String> ids, String channelRule) {
      super(textRule, channel, filters, tableName);
      this.ids = ids;
      this.channelRule = channelRule;
      this.textRule = textRule;
   }

   @Override
   public Object parseSql(String dataSource) {
      int insightType = IndexConstants.getInsightType(dataSource);
      this.init();
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append("select ");
      sqlBuffer.append("datamining('rule-analyse','");
      String jsonStr = this.dataMiningParams();
      sqlBuffer.append(jsonStr.substring(1, jsonStr.length() - 1));
      sqlBuffer.append("',id");
      if (null == this.textRule || !this.textRule.contains("@")) {
         switch (this.getChannel()) {
            case 0:
               sqlBuffer.append(",contentN0");
               break;
            case 1:
               sqlBuffer.append(",contentN1");
               break;
            default:
               if (1 == insightType) {
                  sqlBuffer.append(",content,child_fields.childContentLength,child_fields.childVoiceId");
               } else {
                  sqlBuffer.append(",content");
               }
         }
      } else if (1 == insightType) {
         sqlBuffer.append(",content,contentN0,contentN1,child_fields.childContentLength,child_fields.childVoiceId");
      } else {
         sqlBuffer.append(",content,contentN0,contentN1");
      }

      sqlBuffer.append(") as result from ");
      sqlBuffer.append(this.getTableName());
      sqlBuffer.append(" where processed=0 and id " + SqlHelper.createIn(this.ids, true));
      return sqlBuffer.toString();
   }

   public void init() {
      StringBuffer columnBuffer = new StringBuffer();
      columnBuffer.append("id");
      if (!Strings.isNullOrEmpty(this.getTextRule())) {
         columnBuffer.append(",");
         switch (this.getChannel()) {
            case 0:
               columnBuffer.append("contentN0,");
               break;
            case 1:
               columnBuffer.append("contentN1,");
               break;
            default:
               columnBuffer.append("content,");
         }
      }
   }

   public String dataMiningParams() {
      Map<String, String> paramsMap = new HashMap<>();
      if (this.getTextRule() != null && !"null".equals(this.getTextRule()) && !"".equals(this.getTextRule())) {
         paramsMap.put("textRule", "(" + this.getTextRule() + ")");
      } else {
         paramsMap.put("textRule", "()");
      }

      if (null != this.channelRule && !"".equals(this.channelRule)) {
         paramsMap.put("channelRule", this.channelRule);
      }

      paramsMap.put("objectRule", "");
      return JSON.toJSONString(paramsMap);
   }
}
