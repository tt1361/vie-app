package com.iflytek.vie.app.provider.impl.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.api.model.ModelFragmentService;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.ServiceResponse;
import com.iflytek.vie.app.pojo.model.ModelFragment;
import com.iflytek.vie.app.pojo.model.ModelRequest;
import com.iflytek.vie.app.pojo.model.ModelTagDimension;
import com.iflytek.vie.app.provider.database.ModelFragmentDB;
import com.iflytek.vie.app.provider.ruleparse.ModelCheckException;
import com.iflytek.vie.app.provider.ruleparse.ModelParserPlatform;
import com.iflytek.vie.app.provider.ruleparse.ParserResultPlatform;
import com.iflytek.vie.app.provider.ruleparse.RuleCheckPlatform;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelFragmentServiceImpl implements ModelFragmentService {
   private ObjectMapper mapper = new ObjectMapper();
   private final Logger logger = LoggerFactory.getLogger(ModelFragmentServiceImpl.class);
   private ModelFragmentDB fragmentDB;
   private ServiceResponse serviceResponse = new ServiceResponse();
   private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

   public ServiceResponse addModelFragmentService(ModelRequest request) throws ViePlatformServiceException {
      try {
         this.serviceResponse = new ServiceResponse();
         Date date = new Date();
         this.logger.info("start add model fragment");
         ModelFragment fragment = request.getModelFragment();
         boolean isHaveDX = false;
         boolean isHaveLB = false;
         if (fragment.getRuleType() == 2) {
            fragment.setModelId(-1L);
            fragment.setCreateTime(this.sdf.format(date));
            fragment.setPreviewId(request.getPriviewId());
            fragment.setCreateTimestamp(date.getTime());
            this.fragmentDB.save(fragment);
            this.serviceResponse.setSuccessful(true);
            this.serviceResponse.setValue(fragment.getFragmentId());
            this.logger.info("success add model fragment");
            return this.serviceResponse;
         } else {
            if ("1".equals(fragment.getIsOrder())
               && fragment.getFragmentContent().contains("!")
               && (fragment.getFragmentContent().contains("|") || fragment.getFragmentContent().contains("&"))) {
               try {
                  RuleCheckPlatform.checkNotRule(fragment.getFragmentContent());
               } catch (ModelCheckException var14) {
                  this.logger.error("顺序模型片段取非规则错误，输入的规则为:" + fragment.getFragmentContent());
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage("顺序模型片段取非规则错误，输入的规则为:" + fragment.getFragmentContent());
                  return this.serviceResponse;
               }
            }

            if (fragment.getIsTag() != 1) {
               ModelParserPlatform modelParser = new ModelParserPlatform(null, fragment.getFragmentContent(), null);
               ParserResultPlatform parserResult = modelParser.parse();
               if (fragment.getIsTag() != 1 && parserResult.getRet() == 0) {
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage("该片段规则输入有误！片段序号[" + fragment.getFragmentNum() + "]");
                  return this.serviceResponse;
               }
            } else {
               List<HashMap<String, Object>> list = (List<HashMap<String, Object>>)this.mapper.readValue(fragment.getTagContent(), List.class);

               for (int i = 0; i < list.size(); i++) {
                  HashMap<String, Object> hm = list.get(i);
                  String dimensionCode = hm.get("dimensionCode").toString();
                  if (dimensionCode.equals(ModelTagDimension.DCODE1)) {
                     List<HashMap<String, Object>> list2 = (List<HashMap<String, Object>>)hm.get("value");

                     for (int j = 0; j < list2.size(); j++) {
                        HashMap<String, Object> hm2 = list2.get(j);
                        String propertyCode = hm2.get("propertyCode").toString();
                        if (propertyCode.equals(ModelTagDimension.PDAXIAOCODE)) {
                           isHaveDX = true;
                        }
                     }

                     if (!isHaveDX) {
                        this.logger.error("请先选择静音对象大小");
                        this.serviceResponse.setSuccessful(false);
                        this.serviceResponse.setMessage("请先选择静音对象大小");
                        return this.serviceResponse;
                     }
                  } else {
                     List<HashMap<String, Object>> list2 = (List<HashMap<String, Object>>)hm.get("value");

                     for (int jx = 0; jx < list2.size(); jx++) {
                        HashMap<String, Object> hm2 = list2.get(jx);
                        String propertyCode = hm2.get("propertyCode").toString();
                        if (propertyCode.equals(ModelTagDimension.PLIEBIAOCODE)) {
                           isHaveLB = true;
                        }
                     }

                     if (!isHaveLB) {
                        this.logger.error("请先选择关键词列表");
                        this.serviceResponse.setSuccessful(false);
                        this.serviceResponse.setMessage("请先选择关键词列表");
                        return this.serviceResponse;
                     }
                  }
               }
            }

            fragment.setModelId(-1L);
            fragment.setCreateTime(this.sdf.format(date));
            fragment.setPreviewId(request.getPriviewId());
            fragment.setCreateTimestamp(date.getTime());
            this.fragmentDB.save(fragment);
            this.serviceResponse.setSuccessful(true);
            this.serviceResponse.setValue(fragment.getFragmentId());
            this.logger.info("success add model fragment");
            return this.serviceResponse;
         }
      } catch (Exception var15) {
         this.logger.error("添加模型片段错误");
         throw new ViePlatformServiceException("接口内部错误", var15);
      }
   }

   public ModelFragment getCombineFragmentIdService(ModelRequest request) throws ViePlatformServiceException {
      return null;
   }

   public ModelFragmentDB getFragmentDB() {
      return this.fragmentDB;
   }

   public void setFragmentDB(ModelFragmentDB fragmentDB) {
      this.fragmentDB = fragmentDB;
   }
}
