package com.iflytek.vie.app.provider.ruleparse.parse;

import com.google.common.base.Strings;
import com.iflytek.vie.app.provider.ruleparse.model.operator.DoubleOperator;
import com.iflytek.vie.app.provider.ruleparse.model.operator.IntOperator;
import com.iflytek.vie.app.provider.ruleparse.model.operator.OperatorType;
import com.iflytek.vie.app.provider.ruleparse.model.operator.StringOperator;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagPropType;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagProperty;
import com.iflytek.vie.app.provider.ruleparse.model.tag.Tag;
import com.iflytek.vie.app.provider.ruleparse.model.tag.TagContainer;
import com.iflytek.vie.app.provider.ruleparse.model.tag.TagType;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TagRuleParse {
   public TagContainer tagContainer = new TagContainer();

   public TagRuleParse(String objectRule) {
      this.parseObjRule(objectRule);
   }

   public TagRuleParse(String textRule, String objectRule) {
      this(objectRule);
      this.tagContainer.textRule = textRule;
   }

   public void parseObjRule(String rule) {
      if (!Strings.isNullOrEmpty(rule)) {
         SAXParserFactory fact = SAXParserFactory.newInstance();
         fact.setNamespaceAware(false);
         fact.setValidating(false);

         try {
            SAXParser parser = fact.newSAXParser();
            TagRuleParse.ObjectRuleHandler handler = new TagRuleParse.ObjectRuleHandler();
            parser.parse(new InputSource(new StringReader(rule)), handler);
            if (this.tagContainer.relativeTagMap.size() > 0) {
               for (String tagId : this.tagContainer.relativeTagMap.values()) {
                  this.tagContainer.tagMap.get(tagId).isOpposite = true;
               }
            }
         } catch (Exception var7) {
            throw new RuntimeException(var7);
         }
      }
   }

   private class ObjectRuleHandler extends DefaultHandler {
      Tag tag = null;
      int illCount = 0;
      List<Integer> GTOrEqualList = new ArrayList<>();
      List<Integer> LEOrEqualList = new ArrayList<>();

      private ObjectRuleHandler() {
      }

      @Override
      public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
         if (qName.equals("filter")) {
            String id = attributes.getValue("id");
            String type = attributes.getValue("type");
            this.tag = new Tag();
            this.tag.id = id;
            this.tag.type = TagType.getType(type);
            TagRuleParse.this.tagContainer.tagMap.put(id, this.tag);
            TagRuleParse.this.tagContainer.tagTypeMap.put(this.tag.type, this.tag);
         }

         if (qName.equals("rule")) {
            String type = attributes.getValue("type");
            String operator = attributes.getValue("operator");
            String value = attributes.getValue("value");
            String relativeId = attributes.getValue("relativeobject");
            TagProperty property = new TagProperty();
            property.type = TagPropType.getType(type);
            this.tag.tagPropList.add(property);
            this.tag.tagProTypeMap.put(property.type, property);
            OperatorType oprType = OperatorType.getType(operator);
            switch (property.type) {
               case NumOfOccurrences:
                  property.operator = new IntOperator(oprType);
                  property.intValue = Integer.parseInt(value);
                  if ((oprType == OperatorType.Equal || oprType == OperatorType.LTOrEqual) && property.intValue == 0
                     || oprType == OperatorType.LessThan && property.intValue == 1) {
                     this.tag.isNot = true;
                  }
                  break;
               case Size:
                  property.operator = new IntOperator(oprType);
                  property.intValue = Integer.parseInt(value);
                  if (oprType == OperatorType.Equal && property.intValue == 0 && this.illCount == 0) {
                     this.tag.numIsZero = true;
                  }

                  if (oprType == OperatorType.GTOrEqual || oprType == OperatorType.GreaterThan) {
                     if (null != this.GTOrEqualList && this.GTOrEqualList.size() == 0) {
                        this.GTOrEqualList.add(new Integer(property.intValue));
                     } else {
                        int propertyVal = -1;
                        if (null != this.GTOrEqualList) {
                           propertyVal = this.GTOrEqualList.get(0);
                        }

                        if (propertyVal > -1 && property.intValue > propertyVal) {
                           this.GTOrEqualList.clear();
                           this.GTOrEqualList.add(new Integer(property.intValue));
                        }
                     }
                  }

                  if (oprType == OperatorType.LessThan || oprType == OperatorType.LTOrEqual) {
                     if (null != this.LEOrEqualList && this.LEOrEqualList.size() == 0) {
                        this.LEOrEqualList.add(new Integer(property.intValue));
                     } else {
                        int propertyValx = -1;
                        if (null != this.LEOrEqualList) {
                           propertyValx = this.LEOrEqualList.get(0);
                        }

                        if (propertyValx > -1 && property.intValue < propertyValx) {
                           this.LEOrEqualList.clear();
                           this.LEOrEqualList.add(new Integer(property.intValue));
                        }
                     }
                  }

                  if (oprType == OperatorType.GTOrEqual) {
                     if (null != this.LEOrEqualList && this.LEOrEqualList.size() > 0) {
                        int proVal = this.LEOrEqualList.get(0);
                        if (proVal < property.intValue) {
                           this.tag.numIsZero = false;
                           this.illCount++;
                        } else if (property.intValue == 0 && this.illCount == 0) {
                           this.tag.numIsZero = true;
                        } else {
                           this.tag.numIsZero = false;
                           this.illCount++;
                        }
                     } else if (property.intValue == 0 && this.illCount == 0) {
                        this.tag.numIsZero = true;
                     } else {
                        this.tag.numIsZero = false;
                        this.illCount++;
                     }
                  }

                  if (oprType == OperatorType.GreaterThan) {
                     this.tag.numIsZero = false;
                     this.illCount++;
                  }

                  if (oprType == OperatorType.LessThan || oprType == OperatorType.LTOrEqual) {
                     if (null != this.GTOrEqualList && this.GTOrEqualList.size() > 0) {
                        int proVal = this.GTOrEqualList.get(0);
                        if (proVal > property.intValue) {
                           this.tag.numIsZero = false;
                           this.illCount++;
                        } else if (proVal == 0 && this.illCount == 0) {
                           this.tag.numIsZero = true;
                        } else {
                           this.tag.numIsZero = false;
                           this.illCount++;
                        }
                     } else if (oprType == OperatorType.LessThan && property.intValue == 0) {
                        this.tag.numIsZero = false;
                        this.illCount++;
                     } else if (this.illCount == 0) {
                        this.tag.numIsZero = true;
                     } else {
                        this.tag.numIsZero = false;
                        this.illCount++;
                     }
                  }
                  break;
               case RelativePosition:
                  property.operator = new IntOperator(oprType);
                  property.intValue = Integer.parseInt(value);
                  TagRuleParse.this.tagContainer.relativeTagMap.put(this.tag.id, relativeId);
                  this.tag.isRelative = true;
                  break;
               case AbsolutePosition:
                  property.operator = new IntOperator(oprType);
                  property.intValue = Integer.parseInt(value);
                  this.tag.isAbsolue = true;
                  break;
               case List:
                  if (this.tag.type == TagType.Keyword) {
                     property.textRuleParse = new TextRuleParse(value);
                     property.strValue = value;
                  }
                  break;
               case BeforeChannel:
                  property.operator = new StringOperator(oprType);
                  property.strValue = value.equals("n0") ? "0" : "1";
                  break;
               case AfterChannel:
                  property.operator = new StringOperator(oprType);
                  property.strValue = value.equals("n0") ? "0" : "1";
                  break;
               case Channel:
                  property.operator = new StringOperator(oprType);
                  property.strValue = value.equals("n0") ? "0" : "1";
                  break;
               case SingleSpeed:
                  property.operator = new DoubleOperator(oprType);
                  property.doubleValue = Double.parseDouble(value);
                  break;
               case AvgSpeed:
                  property.operator = new DoubleOperator(oprType);
                  property.doubleValue = Double.parseDouble(value);
                  break;
               default:
                  property.operator = new StringOperator(oprType);
                  property.strValue = value;
            }
         }
      }

      @Override
      public void endElement(String uri, String localName, String qName) throws SAXException {
         if (qName.equals("filter")) {
            this.tag = null;
         }
      }
   }
}
