package com.iflytek.vie.app.provider.impl.tools.wordAssociation;

import com.iflytek.vie.app.api.tools.WordAssociationService;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.auth.AuthorizeInfo;
import com.iflytek.vie.app.pojo.dimension.WordAssociationResponse;
import com.iflytek.vie.jni.IFlyFOD;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordAssociationServiceImpl implements WordAssociationService {
   private final Logger logger = LoggerFactory.getLogger(WordAssociationServiceImpl.class);
   private String resultWord;
   private String fileName;

   public List<WordAssociationResponse> getWordAssiociationService(String kwd, int wordNumber, AuthorizeInfo authorizeInfo) throws ViePlatformServiceException {
      List<WordAssociationResponse> list = new ArrayList<>();

      try {
         if (wordNumber < 0 || wordNumber > 600) {
            this.logger.error("请输入0-600的正整数");
            throw new ViePlatformServiceException("词语联想输入参数有误（相关词个数),请输入0-600的正整数");
         }

         this.logger.info("开始获取相关联想词语");
         this.logger.info(this.fileName);
         IFlyFOD iFlyFOD = new IFlyFOD();
         iFlyFOD.jFODInit(this.fileName);
         this.resultWord = iFlyFOD.jFODSearch(kwd, wordNumber);
         if (null == this.resultWord || "".equals(this.resultWord)) {
            this.logger.info("没查询到相关联想词语");
            return list;
         }

         String[] word = this.resultWord.split(";");

         for (int i = 0; i < word.length; i++) {
            WordAssociationResponse wordAssociationResponse = new WordAssociationResponse();
            String[] str = word[i].split(" ");
            str[0] = this.deleteFuhao(str[0]);
            if (str.length == 2) {
               wordAssociationResponse.setWord(str[0]);
               wordAssociationResponse.setPoint(str[1]);
            }

            list.add(wordAssociationResponse);
         }
      } catch (Exception var10) {
         this.logger.error("获取相关联想词语失败");
         throw new ViePlatformServiceException("服务内部错误", var10);
      }

      this.logger.info("获取相关联想词语成功");
      return list;
   }

   public String deleteFuhao(String string) {
      return string.replaceAll("[\\pP‘’“”]", "");
   }

   public String getResultWord() {
      return this.resultWord;
   }

   public void setResultWord(String resultWord) {
      this.resultWord = resultWord;
   }

   public String getFileName() {
      return this.fileName;
   }

   public void setFileName(String fileName) {
      this.fileName = fileName;
   }
}
