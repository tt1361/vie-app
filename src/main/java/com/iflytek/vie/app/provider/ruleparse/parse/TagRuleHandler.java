package com.iflytek.vie.app.provider.ruleparse.parse;

import com.iflytek.vie.utils.StringUtils;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TagRuleHandler extends DefaultHandler {
   private String objectRule;
   public List<LinkedHashMap<String, Object>> filterMap = new ArrayList<>();
   public LinkedHashMap<String, String> ruleMap;
   private LinkedHashMap<String, Object> filterValue;
   private List<LinkedHashMap<String, String>> ruleList;
   private String id;

   public TagRuleHandler(String objectRule) {
      this.objectRule = objectRule;
   }

   public List<LinkedHashMap<String, Object>> getFilterMap() {
      SAXParserFactory saxFactory = SAXParserFactory.newInstance();
      saxFactory.setNamespaceAware(false);
      saxFactory.setValidating(false);

      try {
         SAXParser saxParser = saxFactory.newSAXParser();
         saxParser.parse(new InputSource(new StringReader(this.objectRule)), this);
      } catch (ParserConfigurationException var3) {
         var3.printStackTrace();
      } catch (SAXException var4) {
         var4.printStackTrace();
      } catch (IOException var5) {
         var5.printStackTrace();
      }

      return this.filterMap;
   }

   @Override
   public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      if ("filter".equals(qName)) {
         this.ruleList = new ArrayList<>();
         this.filterValue = new LinkedHashMap<>();
         this.id = attributes.getValue("id");
         String type = attributes.getValue("type");
         this.filterValue.put("id", this.id);
         this.filterValue.put("dimensionCode", type);
         this.filterValue.put("value", this.ruleList);
         this.filterMap.add(this.filterValue);
      }

      if ("rule".equals(qName)) {
         this.ruleMap = new LinkedHashMap<>();
         String ruleType = attributes.getValue("type");
         String ruleOperatro = attributes.getValue("operator");
         String strValue = attributes.getValue("value");
         this.ruleMap.put("propertyCode", ruleType);
         this.ruleMap.put("operationCode", ruleOperatro);
         this.ruleMap.put("value", strValue);
         if (!StringUtils.isNullOrEmpry(attributes.getValue("relativeobject"))) {
            this.ruleMap.put("relativeobject", attributes.getValue("relativeobject"));
         }
      }
   }

   @Override
   public void endElement(String uri, String localName, String qName) throws SAXException {
      if ("rule".equals(qName)) {
         this.ruleList.add(this.ruleMap);
      }
   }

   public String getObjectRule() {
      return this.objectRule;
   }

   public void setObjectRule(String objectRule) {
      this.objectRule = objectRule;
   }
}
